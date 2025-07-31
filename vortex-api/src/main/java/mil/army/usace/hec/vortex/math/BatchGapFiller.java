package mil.army.usace.hec.vortex.math;

import mil.army.usace.hec.vortex.MessageStore;
import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.VortexProperty;
import mil.army.usace.hec.vortex.io.DataReader;
import mil.army.usace.hec.vortex.io.DataWriter;
import mil.army.usace.hec.vortex.util.Stopwatch;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Performs batch gap filling operations on gridded data.
 * Supports different gap filling methods and can process multiple variables.
 */
public class BatchGapFiller implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(BatchGapFiller.class.getName());
    private static final PathMatcher DSS_MATCHER = FileSystems.getDefault().getPathMatcher("glob:**/*.{dss,DSS}");

    final String source;
    final Set<String> variables;
    final String destination;
    final Map<String, String> writeOptions;
    private final GapFillMethod method;
    final PropertyChangeSupport support;

    /**
     * Creates a new BatchGapFiller with the specified configuration.
     *
     * @param builder The builder containing configuration parameters
     */
    protected BatchGapFiller(Builder builder) {
        this.source = Objects.requireNonNull(builder.source, "Source path cannot be null");
        this.destination = Objects.requireNonNull(builder.destination, "Destination path cannot be null");
        this.variables = new HashSet<>(builder.variables);
        this.writeOptions = new HashMap<>(builder.writeOptions);
        this.method = builder.method;
        this.support = new PropertyChangeSupport(this);
    }

    /**
     * Builder for creating BatchGapFiller instances with a fluent API.
     */
    public static class Builder {
        private String source;
        private final Set<String> variables = new HashSet<>();
        private boolean isSelectAll;
        private String destination;
        private final Map<String, String> writeOptions = new HashMap<>();
        private GapFillMethod method = GapFillMethod.LINEAR_INTERPOLATION; // Default method

        /**
         * Sets the source path for input data.
         *
         * @param source Path to the source data
         * @return This builder instance
         */
        public Builder source(String source) {
            this.source = source;
            return this;
        }

        /**
         * Sets the variables to process.
         *
         * @param variables List of variable names
         * @return This builder instance
         */
        public Builder variables(List<String> variables) {
            this.variables.clear();
            if (variables != null) {
                this.variables.addAll(variables);
            }
            return this;
        }

        /**
         * Configures the builder to process all available variables.
         *
         * @return This builder instance
         */
        public Builder selectAllVariables() {
            this.isSelectAll = true;
            return this;
        }

        /**
         * Sets the destination path for output data.
         *
         * @param destination Path to write output data
         * @return This builder instance
         */
        public Builder destination(String destination) {
            this.destination = destination;
            return this;
        }

        /**
         * Sets write options for the output.
         *
         * @param writeOptions Map of write options
         * @return This builder instance
         */
        public Builder writeOptions(Map<String, String> writeOptions) {
            this.writeOptions.clear();
            if (writeOptions != null) {
                this.writeOptions.putAll(writeOptions);
            }
            return this;
        }

        /**
         * Sets the gap filling method to use.
         *
         * @param method Gap filling method
         * @return This builder instance
         */
        public Builder method(GapFillMethod method) {
            this.method = method;
            return this;
        }

        /**
         * Builds and returns a BatchGapFiller instance with the configured parameters.
         *
         * @return A new BatchGapFiller instance
         * @throws IllegalArgumentException If required parameters are missing or invalid
         */
        public BatchGapFiller build() {
            validateBuildParameters();

            if (isSelectAll) {
                variables.clear();
                variables.addAll(DataReader.getVariables(source));
            }

            return createGapFiller();
        }

        /**
         * Validates builder parameters before building.
         *
         * @throws IllegalArgumentException If required parameters are missing or invalid
         */
        private void validateBuildParameters() {
            if (source == null || source.trim().isEmpty()) {
                throw new IllegalArgumentException("Source path is required");
            }

            if (destination == null || destination.trim().isEmpty()) {
                throw new IllegalArgumentException("Destination path is required");
            }

            if (!isSelectAll && variables.isEmpty()) {
                throw new IllegalArgumentException("No variables selected for processing");
            }

            if (method == null) {
                throw new IllegalArgumentException("Gap filling method cannot be null");
            }
        }

        /**
         * Creates the appropriate gap filler implementation based on the selected method.
         *
         * @return A BatchGapFiller instance of the appropriate type
         */
        private BatchGapFiller createGapFiller() {
            return switch (method) {
                case LINEAR_INTERPOLATION -> new LinearInterpGapFiller(this);
                case TIME_STEP -> new TimeStepFiller(this);
                default -> new BatchGapFiller(this);
            };
        }
    }

    /**
     * Creates a new builder for constructing BatchGapFiller instances.
     *
     * @return A new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void run() {
        Stopwatch stopwatch = new Stopwatch();
        stopwatch.start();

        try {
            notifyStart();
            validateSourceAndDestination();

            Set<String> datasetVars = getAvailableVariables();
            Set<String> processableVars = getProcessableVariables(datasetVars);

            if (processableVars.isEmpty()) {
                LOGGER.warning("No matching variables found for processing");
                notifyCompletion(0, stopwatch);
                return;
            }

            int processedCount = processVariables(processableVars);
            notifyCompletion(processedCount, stopwatch);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during batch gap filling", e);
            support.firePropertyChange(VortexProperty.ERROR.toString(), null,
                    "Error during batch gap filling: " + e.getMessage());
        }
    }

    /**
     * Validates source and destination paths before processing.
     *
     * @throws IllegalStateException If paths are invalid
     */
    private void validateSourceAndDestination() {
        Path sourcePath = Path.of(source);
        if (!Files.exists(sourcePath)) {
            throw new IllegalStateException("Source path does not exist: " + source);
        }

        // Ensure destination directory exists or can be created
        Path destinationPath = Path.of(destination);
        if (!Files.exists(destinationPath.getParent())) {
            try {
                Files.createDirectories(destinationPath.getParent());
            } catch (Exception e) {
                throw new IllegalStateException("Could not create destination directory: " +
                        destinationPath.getParent(), e);
            }
        }
    }

    /**
     * Retrieves available variables from the source.
     *
     * @return Set of available variable names
     */
    private Set<String> getAvailableVariables() {
        try {
            return DataReader.getVariables(source);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error getting variables from source", e);
            return Collections.emptySet();
        }
    }

    /**
     * Determines which variables can be processed based on availability.
     *
     * @param datasetVars Variables available in the dataset
     * @return Set of variables that can be processed
     */
    private Set<String> getProcessableVariables(Set<String> datasetVars) {
        Set<String> processableVars = new HashSet<>();
        for (String variable : variables) {
            if (datasetVars.contains(variable)) {
                processableVars.add(variable);
            } else {
                LOGGER.warning(() -> "Variable not found in dataset: " + variable);
            }
        }
        return processableVars;
    }

    /**
     * Processes the specified variables to fill gaps.
     *
     * @param processableVars Variables to process
     * @return Number of grids processed
     */
    private int processVariables(Set<String> processableVars) {
        AtomicInteger processed = new AtomicInteger(0);
        int totalVariables = processableVars.size();
        int currentVariable = 0;

        for (String variable : processableVars) {
            currentVariable++;
            try {
                processed.addAndGet(processVariable(variable));

                // Update progress after each variable
                int progressPercent = (currentVariable * 100) / totalVariables;
                support.firePropertyChange("progress", null, progressPercent);

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, e, () -> "Error processing variable: " + variable);
            }
        }
        return processed.get();
    }

    /**
     * Processes a single variable to fill gaps.
     *
     * @param variable The variable to process
     * @return Number of grids processed for this variable
     */
    private int processVariable(String variable) throws Exception {
        int processedCount = 0;
        GapFiller gapFiller = GapFiller.of(method);

        try (DataReader reader = DataReader.builder()
                .path(source)
                .variable(variable)
                .build()) {

            int count = reader.getDtoCount();
            LOGGER.fine(() -> "Processing " + count + " grids for variable: " + variable);

            for (int i = 0; i < count; i++) {
                VortexGrid grid = (VortexGrid) reader.getDto(i);
                VortexGrid filledGrid = gapFiller.fill(grid);

                writeGrid(filledGrid);
                processedCount++;

                // Update status periodically
                if (processedCount % 10 == 0) {
                    support.firePropertyChange(VortexProperty.STATUS.toString(), null,
                            "Processed " + processedCount + " grids for " + variable);
                }
            }
        }
        return processedCount;
    }

    /**
     * Writes a grid to the destination.
     *
     * @param grid The grid to write
     * @throws Exception If an error occurs during writing
     */
    private void writeGrid(VortexGrid grid) throws Exception {
        List<VortexData> filled = List.of(grid);

        DataWriter writer = DataWriter.builder()
                .data(filled)
                .destination(destination)
                .options(writeOptions)
                .build();

        writer.write();
    }

    /**
     * Notifies listeners that gap filling has started.
     */
    private void notifyStart() {
        String templateBegin = MessageStore.getInstance().getMessage("gap_filler_begin");
        String messageBegin = String.format(templateBegin);
        support.firePropertyChange(VortexProperty.STATUS.toString(), null, messageBegin);
    }

    /**
     * Notifies listeners that gap filling is complete.
     *
     * @param processed Number of grids processed
     * @param stopwatch Stopwatch measuring execution time
     */
    private void notifyCompletion(int processed, Stopwatch stopwatch) {
        stopwatch.end();

        // Log completion time
        String timeMessage = "Batch gap-filler time: " + stopwatch;
        LOGGER.info(timeMessage);

        // Notify completion
        String templateEnd = MessageStore.getInstance().getMessage("gap_filler_end");
        String messageEnd = String.format(templateEnd, processed, destination);
        support.firePropertyChange(VortexProperty.COMPLETE.toString(), null, messageEnd);

        // Notify about execution time
        String templateTime = MessageStore.getInstance().getMessage("gap_filler_time");
        String messageTime = String.format(templateTime, stopwatch);
        support.firePropertyChange(VortexProperty.STATUS.toString(), null, messageTime);

        // Ensure progress shows 100%
        support.firePropertyChange("progress", null, 100);
    }

    /**
     * Condenses DSS variables by removing time parts from DSS pathnames.
     * This is useful for processing multiple time steps of the same variable.
     */
    protected void condenseVariables() {
        if (DSS_MATCHER.matches(Path.of(source))) {
            Set<String> condensed = new HashSet<>();

            for (String dssPathname : variables) {
                String pathnameSansTime = removeTime(dssPathname);
                condensed.add(pathnameSansTime);
            }

            variables.clear();
            variables.addAll(condensed);
        }
    }

    /**
     * Removes the time part from a DSS pathname.
     *
     * @param dssPathname The DSS pathname to modify
     * @return The pathname with time part replaced by "*"
     */
    private static String removeTime(String dssPathname) {
        if (dssPathname == null) {
            return null;
        }

        dssPathname = dssPathname.trim();

        // Check if pathname starts and ends with '/'
        if (!dssPathname.startsWith("/") || !dssPathname.endsWith("/")) {
            return dssPathname;
        }

        // Remove the leading and trailing '/'
        String trimmedPathname = dssPathname.substring(1, dssPathname.length() - 1);

        // Split the pathname into parts using '/' as delimiter, include empty strings
        // Also trim the parts
        String[] parts = Arrays.stream(trimmedPathname.split("/", -1))
                .map(String::trim)
                .toArray(String[]::new);

        // There should be exactly 6 parts for a valid DSS pathname
        if (parts.length != 6) {
            return dssPathname;
        }

        // Replace time parts with wildcards
        return "/" + String.join("/", parts[0], parts[1], parts[2], "*", "*", parts[5]) + "/";
    }

    /**
     * Adds a PropertyChangeListener to receive events.
     *
     * @param listener The PropertyChangeListener to add
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }

    /**
     * Removes a PropertyChangeListener.
     *
     * @param listener The PropertyChangeListener to remove
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener(listener);
    }

    /**
     * Removes a PropertyChangeListener for a specific property.
     *
     * @param propertyName The name of the property
     * @param listener     The PropertyChangeListener to remove
     */
    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        support.removePropertyChangeListener(propertyName, listener);
    }
}