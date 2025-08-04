package mil.army.usace.hec.vortex.math;

import mil.army.usace.hec.vortex.MessageStore;
import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.VortexProperty;
import mil.army.usace.hec.vortex.io.DataReader;
import mil.army.usace.hec.vortex.io.DataWriter;
import mil.army.usace.hec.vortex.io.TemporalDataReader;
import mil.army.usace.hec.vortex.util.Stopwatch;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import static mil.army.usace.hec.vortex.util.FilenameUtil.isSameFile;

/**
 * Fills gaps in time series by generating missing time steps.
 * <p>
 * This class identifies missing time steps in a temporal dataset and generates
 * the corresponding grids through interpolation or other methods.
 * </p>
 */
public class TimeStepFiller extends BatchGapFiller {
    private static final Logger LOGGER = Logger.getLogger(TimeStepFiller.class.getName());

    // Constants for configuration
    private static final int MAX_TIME_STEPS_WARNING = 1000;

    /**
     * Creates a new TimeStepFiller instance.
     *
     * @param builder The builder containing configuration
     */
    TimeStepFiller(Builder builder) {
        super(builder);
    }

    @Override
    public void run() {
        Stopwatch stopwatch = new Stopwatch();
        stopwatch.start();

        notifyStart();

        try {
            boolean isSourceEqualToDestination = isSameFile(source, destination);
            Set<String> condensed = condenseVariables(Set.copyOf(variables));

            AtomicInteger processed = new AtomicInteger();

            for (String variable : condensed) {
                processVariable(variable, isSourceEqualToDestination);
            }

            notifyCompletion(processed.get(), stopwatch);

        } catch (Exception e) {
            String errorMessage = "Error during time step filling";
            LOGGER.log(Level.SEVERE, errorMessage, e);
            support.firePropertyChange(VortexProperty.ERROR.toString(), null,
                    errorMessage + ": " + e.getMessage());
        }
    }

    /**
     * Start message
     *
     * @return The start processing message
     */
    protected String notifyStartMessage() {
        return MessageStore.getInstance().getMessage("time_step_filler_begin");
    }

    @Override
    protected String notifyCompleteMessage(int processed) {
        String templateEnd = MessageStore.getInstance().getMessage("time_step_filler_end");
        return String.format(templateEnd, processed, destination);
    }

    /**
     * Process a single variable to fill in missing time steps.
     *
     * @param variable                   The variable to process
     * @param isSourceEqualToDestination Whether source and destination are the same
     */
    private void processVariable(String variable, boolean isSourceEqualToDestination) {
        try {
            // Step 1: Analyze the time series to identify available times and intervals
            TimeSeriesInfo timeSeriesInfo = analyzeTimeSeries(variable);
            if (!validateTimeSeriesInfo(timeSeriesInfo)) {
                return;
            }

            // Step 2: Copy existing grids to destination if needed
            if (!isSourceEqualToDestination) {
                copyExistingGrids(variable);
            }

            // Step 3: Identify missing time steps
            List<ZonedDateTime> missingTimes = findMissingTimeSteps(timeSeriesInfo);
            if (missingTimes.isEmpty()) {
                String message = "No missing time steps found for variable: " + variable;
                LOGGER.info(() -> message);
                support.firePropertyChange(VortexProperty.STATUS.toString(), null, message);
                return;
            }

            // Warn if there are too many missing time steps
            if (missingTimes.size() > MAX_TIME_STEPS_WARNING) {
                String message = String.format("Large number of missing time steps (%d) for variable %s",
                        missingTimes.size(), variable);
                LOGGER.warning(() -> message);
                support.firePropertyChange(VortexProperty.WARNING.toString(), null, message);
            }

            // Step 4: Generate missing grids
            generateMissingGrids(variable, timeSeriesInfo, missingTimes);
        } catch (Exception e) {
            String errorMessage = "Error processing variable: " + variable;
            LOGGER.log(Level.SEVERE, e, e::getMessage);
            support.firePropertyChange(VortexProperty.WARNING.toString(), null,
                    errorMessage + ": " + e.getMessage());
        }
    }

    /**
     * Analyzes a time series to extract timing information.
     *
     * @param variable The variable to analyze
     * @return TimeSeriesInfo containing the analysis results
     * @throws Exception If reading fails
     */
    private TimeSeriesInfo analyzeTimeSeries(String variable) throws Exception {
        TimeSeriesInfo info = new TimeSeriesInfo();

        try (DataReader reader = DataReader.builder()
                .path(source)
                .variable(variable)
                .build()) {

            int dtoCount = reader.getDtoCount();
            if (dtoCount == 0) {
                throw new IllegalStateException("No data found for variable: " + variable);
            }

            // Extract time information from each grid
            for (int i = 0; i < dtoCount; i++) {
                VortexGrid grid = (VortexGrid) reader.getDto(i);
                info.addTimePoint(grid.startTime(), grid.interval());
            }

            // Store the reader for later use
            info.sourceReader = reader;
        }

        return info;
    }

    /**
     * Validates the time series information to ensure it's suitable for processing.
     *
     * @param info The time series information
     * @return true if valid, false otherwise
     */
    private boolean validateTimeSeriesInfo(TimeSeriesInfo info) {
        // Check that we have at least 2 time points
        if (info.startTimes.size() < 2) {
            LOGGER.severe("Insufficient time points for time step analysis (minimum 2 required)");
            return false;
        }

        // Check for consistent intervals
        if (info.intervals.size() != 1) {
            LOGGER.severe("Data interval must be consistent. Found " + info.intervals.size() + " different intervals.");
            return false;
        }

        // Determine time step for processing
        info.timeStep = info.intervals.iterator().next();
        if (info.timeStep.isZero()) {
            info.timeStep = getMostCommonInterval(info.startTimes);
            if (info.timeStep == null) {
                LOGGER.severe("Could not determine time step - inconsistent intervals detected");
                return false;
            }
        }

        return true;
    }

    /**
     * Copies existing grids to the destination if source and destination are different.
     *
     * @param variable The variable being processed
     * @throws Exception If writing fails
     */
    private void copyExistingGrids(String variable) throws Exception {
        AtomicInteger processed = new AtomicInteger();

        try (DataReader reader = DataReader.builder()
                .path(source)
                .variable(variable)
                .build()) {

            int dtoCount = reader.getDtoCount();
            for (int i = 0; i < dtoCount; i++) {
                VortexGrid grid = (VortexGrid) reader.getDto(i);

                DataWriter writer = DataWriter.builder()
                        .destination(destination)
                        .data(List.of(grid))
                        .options(writeOptions)
                        .build();

                writer.write();

                processed.incrementAndGet();

                int variableIndex = variables.indexOf(variable);
                float variableProgress = (float) variableIndex / variables.size();
                float gridProgress = (float) processed.get() / dtoCount / variables.size();
                int progressPercent = (int) ((variableProgress + gridProgress) * 100);
                support.firePropertyChange(VortexProperty.PROGRESS.toString(), null, progressPercent);
            }
        }
    }

    /**
     * Finds missing time steps in the time series.
     *
     * @param info The time series information
     * @return List of missing time steps
     */
    private List<ZonedDateTime> findMissingTimeSteps(TimeSeriesInfo info) {
        List<ZonedDateTime> missingTimes = new ArrayList<>();
        Set<ZonedDateTime> existingTimes = new HashSet<>(info.startTimes);

        // Sort start times to ensure correct iteration
        Collections.sort(info.startTimes);

        // First and last time in the series
        ZonedDateTime firstTime = info.startTimes.get(0);
        ZonedDateTime lastTime = info.startTimes.get(info.startTimes.size() - 1);

        // Find missing time steps
        ZonedDateTime currentTime = firstTime;
        while (currentTime.isBefore(lastTime)) {
            if (!existingTimes.contains(currentTime)) {
                missingTimes.add(currentTime);
            }
            currentTime = currentTime.plus(info.timeStep);
        }

        return missingTimes;
    }

    /**
     * Generates grids for missing time steps.
     *
     * @param variable     The variable being processed
     * @param info         Time series information
     * @param missingTimes List of missing time points
     * @throws Exception If grid generation fails
     */
    private void generateMissingGrids(String variable, TimeSeriesInfo info,
                                      List<ZonedDateTime> missingTimes) throws Exception {
        AtomicInteger processed = new AtomicInteger();

        // Create a temporal reader from the source reader
        DataReader readerCopy = DataReader.copy(info.sourceReader);
        Duration interval = info.intervals.iterator().next();

        try (TemporalDataReader temporalReader = TemporalDataReader.create(readerCopy)) {
            int totalMissing = missingTimes.size();

            for (ZonedDateTime missingTime : missingTimes) {
                ZonedDateTime endTime = missingTime.plus(interval);

                // Try to generate the grid for this time step
                temporalReader.read(missingTime, endTime).ifPresent(grid -> {
                    try {
                        writeGrid(grid);
                        processed.getAndIncrement();
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, e, e::getMessage);
                    }
                });

                // Update progress for missing time steps
                int variableIndex = variables.indexOf(variable);
                float variableProgress = (float) variableIndex / variables.size();
                float gridProgress = (float) processed.get() / totalMissing / variables.size();
                int progressPercent = (int) ((variableProgress + gridProgress) * 100);
                support.firePropertyChange("progress", null, progressPercent);
            }
        }
    }

    /**
     * Writes a grid to the destination.
     *
     * @param grid The grid to write
     * @throws Exception If writing fails
     */
    @Override
    protected void writeGrid(VortexGrid grid) throws Exception {
        List<VortexData> grids = List.of(grid);

        DataWriter writer = DataWriter.builder()
                .data(grids)
                .destination(destination)
                .options(writeOptions)
                .build();

        writer.write();
    }

    /**
     * Calculates the most common time interval between consecutive time points.
     *
     * @param times The list of time points
     * @return The most common interval, or null if no consistent interval is found
     */
    private static Duration getMostCommonInterval(List<ZonedDateTime> times) {
        if (times.size() < 2) {
            return null;
        }

        // Sort times to ensure correct calculation
        List<ZonedDateTime> sortedTimes = new ArrayList<>(times);
        Collections.sort(sortedTimes);

        // Count occurrences of each interval
        Map<Duration, Integer> durationCounts = new HashMap<>();
        for (int i = 1; i < sortedTimes.size(); i++) {
            Duration interval = Duration.between(sortedTimes.get(i - 1), sortedTimes.get(i));
            durationCounts.merge(interval, 1, Integer::sum);
        }

        // Find the most common interval
        return durationCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * Inner class to hold time series information.
     */
    private static class TimeSeriesInfo {
        final List<ZonedDateTime> startTimes = new ArrayList<>();
        final Set<Duration> intervals = new HashSet<>();
        Duration timeStep;
        DataReader sourceReader;

        /**
         * Adds a time point to the collection.
         *
         * @param startTime The start time
         * @param interval  The interval
         */
        void addTimePoint(ZonedDateTime startTime, Duration interval) {
            startTimes.add(startTime);
            intervals.add(interval);
        }
    }
}