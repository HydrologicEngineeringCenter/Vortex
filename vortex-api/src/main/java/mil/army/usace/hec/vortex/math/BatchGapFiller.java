package mil.army.usace.hec.vortex.math;

import mil.army.usace.hec.vortex.Message;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.VortexProperty;
import mil.army.usace.hec.vortex.io.DataReader;
import mil.army.usace.hec.vortex.io.DataWriter;
import mil.army.usace.hec.vortex.util.DssUtil;
import mil.army.usace.hec.vortex.util.Stopwatch;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static mil.army.usace.hec.vortex.util.FilenameUtil.isSameFile;

/**
 * Performs batch gap filling operations on gridded data.
 * Supports different gap filling methods and can process multiple variables.
 */
public class BatchGapFiller implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(BatchGapFiller.class.getName());
    protected final String source;
    protected final List<String> variables;
    protected final String destination;
    protected final boolean isSourceEqualToDestination;
    protected final Map<String, String> writeOptions;
    protected final GapFillMethod method;
    protected final PropertyChangeSupport support;

    protected BatchGapFiller(Builder builder) {
        source = Objects.requireNonNull(builder.source, "Source path cannot be null");
        destination = Objects.requireNonNull(builder.destination, "Destination path cannot be null");
        isSourceEqualToDestination = isSameFile(source, destination);
        variables = List.copyOf(condenseVariables(builder.variables));
        writeOptions = Map.copyOf(builder.writeOptions);
        method = builder.method;
        support = new PropertyChangeSupport(this);
    }

    public static class Builder {
        private String source;
        private final Set<String> variables = new HashSet<>();
        private boolean isSelectAll;
        private String destination;
        private final Map<String, String> writeOptions = new HashMap<>();
        private GapFillMethod method = GapFillMethod.LINEAR_INTERPOLATION;

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public Builder variables(List<String> variables) {
            this.variables.clear();
            if (variables != null) {
                this.variables.addAll(variables);
            }
            return this;
        }

        public Builder selectAllVariables() {
            this.isSelectAll = true;
            return this;
        }

        public Builder destination(String destination) {
            this.destination = destination;
            return this;
        }

        public Builder writeOptions(Map<String, String> writeOptions) {
            this.writeOptions.clear();
            if (writeOptions != null) {
                this.writeOptions.putAll(writeOptions);
            }
            return this;
        }

        public Builder method(GapFillMethod method) {
            this.method = method;
            return this;
        }

        public BatchGapFiller build() {
            validate();

            if (isSelectAll) {
                variables.clear();
                try {
                    variables.addAll(DataReader.getVariables(source));
                } catch (Exception e) {
                    throw new IllegalArgumentException("Failed to read variables from source: " + e.getMessage(), e);
                }
            }

            return switch (method) {
                case LINEAR_INTERPOLATION -> new LinearInterpGapFiller(this);
                case TIME_STEP -> new TimeStepFiller(this);
                default -> new BatchGapFiller(this);
            };
        }

        private void validate() {
            if (source == null || source.trim().isEmpty())
                throw new IllegalArgumentException("Source path is required");
            if (destination == null || destination.trim().isEmpty())
                throw new IllegalArgumentException("Destination path is required");
            if (!isSelectAll && variables.isEmpty())
                throw new IllegalArgumentException("No variables selected for processing");
            if (method == null)
                throw new IllegalArgumentException("Gap filling method cannot be null");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void run() {
        Stopwatch stopwatch = new Stopwatch();
        stopwatch.start();

        try {
            notifyStart();
            ensureDestinationDirectory();

            List<String> processableVars = getProcessableVariables();

            if (processableVars.isEmpty()) {
                String message = Message.format("gap_filler_error_no_matching_vars");
                LOGGER.warning(message);
                support.firePropertyChange(VortexProperty.STATUS.toString(), null, message);
                notifyCompletion(0, stopwatch);
                return;
            }

            int processedCount = processVariables(processableVars);
            notifyCompletion(processedCount, stopwatch);

        } catch (Exception e) {
            String message = Message.format("gap_filler_error_generic");
            LOGGER.log(Level.SEVERE, message, e);
            support.firePropertyChange(VortexProperty.ERROR.toString(), null, message + ": " + e.getMessage());
        }
    }

    private void ensureDestinationDirectory() {
        Path parent = Path.of(destination).getParent();
        if (parent != null && !Files.exists(parent)) {
            try {
                Files.createDirectories(parent);
            } catch (Exception e) {
                throw new IllegalStateException("Could not create destination directory: " + parent, e);
            }
        }
    }

    private List<String> getProcessableVariables() {
        Set<String> available;
        try {
            available = condenseVariables(DataReader.getVariables(source));
        } catch (Exception e) {
            String message = Message.format("gap_filler_error_getting_vars");
            LOGGER.log(Level.WARNING, message, e);
            return List.of();
        }

        List<String> processable = new ArrayList<>();
        for (String variable : variables) {
            if (available.contains(variable)) {
                processable.add(variable);
            } else {
                String message = Message.format("gap_filler_error_var_not_found");
                LOGGER.warning(() -> message + ": " + variable);
            }
        }
        return processable;
    }

    protected int processVariables(List<String> processableVars) {
        int processed = 0;

        for (String variable : processableVars) {
            try {
                processed += processVariable(variable);
            } catch (Exception e) {
                String message = Message.format("gap_filler_error_var", variable);
                LOGGER.log(Level.SEVERE, e, () -> message);
                support.firePropertyChange(VortexProperty.WARNING.toString(), null, message);
            }
        }

        return processed;
    }

    protected int processVariable(String variable) throws Exception {
        GapFiller gapFiller = GapFiller.of(method);
        int gapFilled = 0;

        try (DataReader reader = DataReader.builder()
                .path(source)
                .variable(variable)
                .build()) {

            int count = reader.getDtoCount();
            int variableIndex = variables.indexOf(variable);
            float variableProgress = (float) variableIndex / variables.size();

            for (int i = 0; i < count; i++) {
                VortexGrid grid = (VortexGrid) reader.getDto(i);
                VortexGrid filledGrid = gapFiller.fill(grid);

                if (filledGrid != grid) {
                    writeGrid(filledGrid);
                    gapFilled++;
                } else if (!isSourceEqualToDestination) {
                    writeGrid(grid);
                }

                float gridProgress = (float) (i + 1) / count / variables.size();
                int progressPercent = (int) ((variableProgress + gridProgress) * 100);
                support.firePropertyChange(VortexProperty.PROGRESS.toString(), null, progressPercent);
            }

            String message = Message.format("gap_filler_status_processed_var", count, variable);
            LOGGER.fine(() -> message);
        }
        return gapFilled;
    }

    protected void writeGrid(VortexGrid grid) {
        DataWriter.builder()
                .data(List.of(grid))
                .destination(destination)
                .options(writeOptions)
                .build()
                .write();
    }

    protected void notifyStart() {
        String message = notifyStartMessage();
        support.firePropertyChange(VortexProperty.STATUS.toString(), null, message);
    }

    protected String notifyStartMessage() {
        return Message.format("gap_filler_status_begin");
    }

    protected void notifyCompletion(int processed, Stopwatch stopwatch) {
        stopwatch.end();
        support.firePropertyChange(VortexProperty.COMPLETE.toString(), null, notifyCompleteMessage(processed));

        String messageTime = Message.format("gap_filler_status_time", stopwatch);
        LOGGER.info(messageTime);
        support.firePropertyChange(VortexProperty.STATUS.toString(), null, messageTime);
    }

    protected String notifyCompleteMessage(int processed) {
        return Message.format("gap_filler_status_end", processed, destination);
    }

    protected Set<String> condenseVariables(Set<String> variables) {
        return DssUtil.condenseVariables(source, variables);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        support.addPropertyChangeListener(propertyName, listener);
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        support.removePropertyChangeListener(propertyName, listener);
    }
}
