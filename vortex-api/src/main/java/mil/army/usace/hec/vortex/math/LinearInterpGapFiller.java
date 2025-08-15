package mil.army.usace.hec.vortex.math;

import mil.army.usace.hec.vortex.MessageStore;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.VortexProperty;
import mil.army.usace.hec.vortex.io.DataReader;
import mil.army.usace.hec.vortex.util.Stopwatch;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implements a linear interpolation algorithm to fill gaps (missing data) in time series grids.
 * This class processes grid data and performs temporal interpolation between available data points
 * to estimate values for missing cells.
 */
class LinearInterpGapFiller extends BatchGapFiller {

    private static final Logger LOGGER = Logger.getLogger(LinearInterpGapFiller.class.getName());
    private static final int BATCH_SIZE = 10; // Process grids in batches for better memory efficiency

    /**
     * Constructs a new LinearInterpGapFiller with the specified builder.
     *
     * @param builder The builder containing configuration for this gap filler
     */
    LinearInterpGapFiller(Builder builder) {
        super(builder);
    }

    @Override
    public void run() {
        Stopwatch stopwatch = new Stopwatch();
        stopwatch.start();

        try {
            notifyStart();

            Set<String> datasetVars = getAvailableVariables();
            Set<String> processableVars = getProcessableVariables(datasetVars);

            if (processableVars.isEmpty()) {
                String message = MessageStore.INSTANCE.getMessage("gap_filler_error_no_matching_vars");
                LOGGER.warning(() -> message);
                support.firePropertyChange(VortexProperty.ERROR.toString(), null, message);
                notifyCompletion(0, stopwatch);
                return;
            }

            int processedCount = processVariables(processableVars);
            notifyCompletion(processedCount, stopwatch);

        } catch (Exception e) {
            String message = MessageStore.INSTANCE.getMessage("linear_interp_filler_error_generic");
            LOGGER.log(Level.SEVERE, message, e);
            support.firePropertyChange(VortexProperty.ERROR.toString(), null, message);
        }
    }

    /**
     * Start message
     *
     * @return The start processing message
     */
    @Override
    protected String notifyStartMessage() {
        return MessageStore.getInstance().getMessage("linear_interp_filler_begin");
    }

    @Override
    protected String notifyCompleteMessage(int processed) {
        String templateEnd = MessageStore.getInstance().getMessage("linear_interp_filler_end");
        return String.format(templateEnd, processed, destination);
    }

    /**
     * Process a single variable and fill gaps in its data.
     *
     * @param variable The variable name to process
     * @return Number of grids processed for this variable
     * @throws Exception If an error occurs during processing
     */
    @Override
    protected int processVariable(String variable) throws Exception {
        AtomicInteger processedCount = new AtomicInteger(0);

        try (DataReader reader = DataReader.builder()
                .path(source)
                .variable(variable)
                .build()) {

            int dtoCount = reader.getDtoCount();
            if (dtoCount == 0) {
                LOGGER.info("No data found for variable: " + variable);
                return 0;
            }

            // First pass: analyze grid data and build metadata
            LOGGER.fine(() -> "Analyzing " + dtoCount + " grids for variable: " + variable);
            GridMetadata metadata = analyzeGridData(reader, dtoCount);

            // Second pass: process grids in batches for better memory efficiency
            LOGGER.fine(() -> "Processing " + metadata.hasNoData.size() + " grids with gaps for variable: " + variable);
            processGridsInBatches(reader, metadata, processedCount, variable);
        }

        return processedCount.get();
    }

    /**
     * Processes grids in batches to improve memory efficiency.
     *
     * @param reader         The data reader
     * @param metadata       Metadata about the grid series
     * @param processedCount Counter for processed grids
     * @param variable       Current variable name (for logging)
     * @throws Exception If an error occurs during processing
     */
    private void processGridsInBatches(DataReader reader, GridMetadata metadata,
                                       AtomicInteger processedCount, String variable) throws Exception {
        int dtoCount = reader.getDtoCount();

        // Process in batches of BATCH_SIZE
        for (int batchStart = 0; batchStart < dtoCount; batchStart += BATCH_SIZE) {
            int batchEnd = Math.min(batchStart + BATCH_SIZE, dtoCount);

            // Load batch of grids
            Map<Integer, VortexGrid> gridBatch = new HashMap<>();
            for (int i = batchStart; i < batchEnd; i++) {
                gridBatch.put(i, (VortexGrid) reader.getDto(i));
            }

            // Process each grid in the batch
            for (int i = batchStart; i < batchEnd; i++) {
                VortexGrid grid = gridBatch.get(i);

                // Only process grids that need gap filling
                if (metadata.hasNoData.contains(i)) {
                    VortexGrid filledGrid = fillGridGaps(grid, i, gridBatch, metadata, reader);
                    writeGrid(filledGrid);
                } else {
                    writeGrid(grid);
                }

                processedCount.incrementAndGet();
            }

            // Clear batch to free memory
            gridBatch.clear();
        }
    }

    /**
     * Analyzes all grids for a variable to collect metadata needed for interpolation.
     *
     * @param reader   The data reader
     * @param dtoCount The number of grids to process
     * @return Metadata about the grid series
     */
    private GridMetadata analyzeGridData(DataReader reader, int dtoCount) {
        GridMetadata metadata = new GridMetadata(dtoCount);

        for (int i = 0; i < dtoCount; i++) {
            VortexGrid vortexGrid = (VortexGrid) reader.getDto(i);
            float[] data = vortexGrid.data();
            double noDataValue = vortexGrid.noDataValue();

            // Track which cells have missing data
            metadata.isNoData[i] = new byte[data.length];
            boolean gridHasNoData = false;

            for (int j = 0; j < data.length; j++) {
                if (Double.compare(noDataValue, data[j]) == 0) {
                    metadata.isNoData[i][j] = 1;
                    gridHasNoData = true;

                    // Track cells that need interpolation across the time series
                    metadata.cellsNeedingInterpolation.add(j);
                }
            }

            if (gridHasNoData) {
                metadata.hasNoData.add(i);
            }

            metadata.times[i] = vortexGrid.startTime();
            metadata.noDataValues[i] = noDataValue;

            // Ensure all grids have the same interval
            metadata.intervals.add(vortexGrid.interval());
            if (metadata.intervals.size() > 1) {
                String message = MessageStore.INSTANCE.getMessage("linear_interp_filler_error_intervals");
                support.firePropertyChange(VortexProperty.ERROR.toString(), null, message);
                throw new IllegalStateException(message);
            }
        }

        return metadata;
    }

    /**
     * Fills gaps in a single grid using linear interpolation from surrounding temporal data.
     *
     * @param vortexGrid The grid to fill
     * @param gridIndex  The index of the current grid
     * @param gridBatch  A batch of grids that includes the current one (for local access)
     * @param metadata   Metadata about the grid series
     * @param reader     The data reader (to access grids outside the current batch)
     * @return A new grid with gaps filled where possible
     * @throws Exception If an error occurs during processing
     */
    private VortexGrid fillGridGaps(VortexGrid vortexGrid, int gridIndex,
                                    Map<Integer, VortexGrid> gridBatch,
                                    GridMetadata metadata, DataReader reader) throws Exception {
        float[] data = vortexGrid.data().clone();  // Clone to avoid modifying original
        byte[] isNoDataI = metadata.isNoData[gridIndex];
        int cellCount = 0;

        // Process only cells that need interpolation (performance optimization)
        for (int cellIndex : metadata.cellsNeedingInterpolation) {
            if (isNoDataI[cellIndex] == 1) {
                // Find previous valid data point
                TimeValuePair previous = findPreviousValidValue(gridIndex, cellIndex, metadata, gridBatch, reader);
                if (previous == null) continue;  // No valid previous data

                // Find next valid data point
                TimeValuePair next = findNextValidValue(gridIndex, cellIndex, metadata, gridBatch, reader);
                if (next == null) continue;  // No valid next data

                // Perform interpolation
                data[cellIndex] = interpolateCell(
                        metadata.times[gridIndex],
                        previous.time,
                        previous.value,
                        next.time,
                        next.value
                );

                cellCount++;
            }
        }

        if (cellCount > 0) {
            LOGGER.fine(String.format("Filled %d cells in grid at time %s",
                    cellCount, vortexGrid.startTime()));
        }

        return VortexGrid.toBuilder(vortexGrid)
                .data(data)
                .build();
    }

    /**
     * Finds the previous valid value for a specific cell.
     *
     * @param currentIndex The current grid index
     * @param cellIndex    The cell index to check
     * @param metadata     The metadata containing no-data flags
     * @param gridBatch    Currently loaded grids for quick access
     * @param reader       The data reader for loading additional grids if needed
     * @return A TimeValuePair containing the previous valid value and its time, or null if none found
     * @throws Exception If an error occurs during grid access
     */
    private TimeValuePair findPreviousValidValue(int currentIndex, int cellIndex,
                                                 GridMetadata metadata,
                                                 Map<Integer, VortexGrid> gridBatch,
                                                 DataReader reader) throws Exception {
        for (int i = currentIndex - 1; i >= 0; i--) {
            if (metadata.isNoData[i][cellIndex] == 0) {
                float value;

                // Try to get value from batch first (faster)
                if (gridBatch.containsKey(i)) {
                    value = gridBatch.get(i).data()[cellIndex];
                } else {
                    // Need to load grid from reader
                    VortexGrid grid = (VortexGrid) reader.getDto(i);
                    value = grid.data()[cellIndex];
                }

                return new TimeValuePair(metadata.times[i], value);
            }
        }
        return null;
    }

    /**
     * Finds the next valid value for a specific cell.
     *
     * @param currentIndex The current grid index
     * @param cellIndex    The cell index to check
     * @param metadata     The metadata containing no-data flags
     * @param gridBatch    Currently loaded grids for quick access
     * @param reader       The data reader for loading additional grids if needed
     * @return A TimeValuePair containing the next valid value and its time, or null if none found
     */
    private TimeValuePair findNextValidValue(int currentIndex, int cellIndex,
                                             GridMetadata metadata,
                                             Map<Integer, VortexGrid> gridBatch,
                                             DataReader reader) {
        for (int i = currentIndex + 1; i < metadata.isNoData.length; i++) {
            if (metadata.isNoData[i][cellIndex] == 0) {
                float value;

                // Try to get value from batch first (faster)
                if (gridBatch.containsKey(i)) {
                    value = gridBatch.get(i).data()[cellIndex];
                } else {
                    // Need to load grid from reader
                    VortexGrid grid = (VortexGrid) reader.getDto(i);
                    value = grid.data()[cellIndex];
                }

                return new TimeValuePair(metadata.times[i], value);
            }
        }
        return null;
    }

    /**
     * Interpolates a value based on time and surrounding data points.
     *
     * @param currentTime The time to interpolate at
     * @param prevTime    The time of the previous valid data point
     * @param prevValue   The value of the previous valid data point
     * @param nextTime    The time of the next valid data point
     * @param nextValue   The value of the next valid data point
     * @return The interpolated value
     */
    private float interpolateCell(ZonedDateTime currentTime, ZonedDateTime prevTime,
                                  float prevValue, ZonedDateTime nextTime, float nextValue) {
        long x = currentTime.toEpochSecond();
        long x1 = prevTime.toEpochSecond();
        long x2 = nextTime.toEpochSecond();

        return interpolate(x, x1, prevValue, x2, nextValue);
    }

    /**
     * Performs linear interpolation between two points.
     *
     * @param x  The x-coordinate to interpolate at
     * @param x1 The x-coordinate of the first point
     * @param y1 The y-coordinate of the first point
     * @param x2 The x-coordinate of the second point
     * @param y2 The y-coordinate of the second point
     * @return The interpolated value at x
     * @throws IllegalArgumentException If x1 equals x2 (division by zero)
     */
    private static float interpolate(long x, long x1, float y1, long x2, float y2) {
        if (x1 == x2) {
            throw new IllegalArgumentException("x1 and x2 cannot be the same value (would cause division by zero)");
        }
        return y1 + ((x - x1) * (y2 - y1)) / (x2 - x1);
    }

    /**
     * Class to hold a time and value pair for interpolation.
     */
    private record TimeValuePair(ZonedDateTime time, float value) {
    }

    /**
     * Inner class to hold metadata about a series of grids.
     */
    private static class GridMetadata {
        final ZonedDateTime[] times;
        final byte[][] isNoData;
        final double[] noDataValues;
        final Set<Integer> hasNoData;
        final Set<Integer> cellsNeedingInterpolation;
        final Set<Duration> intervals;

        GridMetadata(int size) {
            times = new ZonedDateTime[size];
            isNoData = new byte[size][];
            noDataValues = new double[size];
            hasNoData = new LinkedHashSet<>();
            cellsNeedingInterpolation = new HashSet<>();
            intervals = new HashSet<>();
        }
    }
}