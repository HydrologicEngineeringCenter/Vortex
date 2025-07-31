package mil.army.usace.hec.vortex.math;

import mil.army.usace.hec.vortex.MessageStore;
import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.VortexProperty;
import mil.army.usace.hec.vortex.io.DataReader;
import mil.army.usace.hec.vortex.io.DataWriter;
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
public class LinearInterpGapFiller extends BatchGapFiller {

    private static final Logger LOGGER = Logger.getLogger(LinearInterpGapFiller.class.getName());

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

        notifyStart();
        condenseVariables();

        AtomicInteger processed = new AtomicInteger();

        for (String variable : variables) {
            try {
                processVariable(variable, processed);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error processing variable " + variable, e);
            }
        }

        stopwatch.end();
        logAndNotifyCompletion(stopwatch, processed.get());
    }

    /**
     * Process a single variable and fill gaps in its data.
     *
     * @param variable  The variable name to process
     * @param processed Counter for tracking processed grids
     * @throws Exception If an error occurs during processing
     */
    private void processVariable(String variable, AtomicInteger processed) throws Exception {
        try (DataReader reader = DataReader.builder()
                .path(source)
                .variable(variable)
                .build()) {

            int dtoCount = reader.getDtoCount();
            if (dtoCount == 0) {
                LOGGER.info("No data found for variable: " + variable);
                return;
            }

            GridMetadata metadata = analyzeGridData(reader, dtoCount);

            for (int i = 0; i < dtoCount; i++) {
                VortexGrid vortexGrid = (VortexGrid) reader.getDto(i);

                // Only process grids that contain missing data
                if (metadata.hasNoData.contains(i)) {
                    VortexGrid filledGrid = fillGridGaps(vortexGrid, i, reader, metadata);
                    writeGrid(filledGrid);
                } else {
                    writeGrid(vortexGrid);
                }

                processed.incrementAndGet();
            }
        }
    }

    /**
     * Analyzes all grids for a variable to collect metadata needed for interpolation.
     *
     * @param reader   The data reader
     * @param dtoCount The number of grids to process
     * @return Metadata about the grid series
     * @throws Exception If data is inconsistent or cannot be read
     */
    private GridMetadata analyzeGridData(DataReader reader, int dtoCount) throws Exception {
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
                }
            }

            if (gridHasNoData) {
                metadata.hasNoData.add(i);
            }

            metadata.times[i] = vortexGrid.startTime();

            // Ensure all grids have the same interval
            metadata.intervals.add(vortexGrid.interval());
            if (metadata.intervals.size() != 1) {
                throw new IllegalStateException(
                        "Inconsistent data intervals detected. All grids must have the same interval.");
            }
        }

        return metadata;
    }

    /**
     * Fills gaps in a single grid using linear interpolation from surrounding temporal data.
     *
     * @param vortexGrid The grid to fill
     * @param gridIndex  The index of the current grid
     * @param reader     The data reader (to access surrounding grids)
     * @param metadata   Metadata about the grid series
     * @return A new grid with gaps filled where possible
     * @throws Exception If an error occurs during processing
     */
    private VortexGrid fillGridGaps(VortexGrid vortexGrid, int gridIndex,
                                    DataReader reader, GridMetadata metadata) throws Exception {
        float[] data = vortexGrid.data().clone();  // Clone to avoid modifying original
        byte[] isNoDataI = metadata.isNoData[gridIndex];
        int cellCount = 0;

        for (int cellIndex = 0; cellIndex < isNoDataI.length; cellIndex++) {
            if (isNoDataI[cellIndex] == 1) {
                // Find previous valid data point
                int prevIndex = findPreviousValidIndex(gridIndex, cellIndex, metadata);
                if (prevIndex < 0) continue;  // No valid previous data

                // Find next valid data point
                int nextIndex = findNextValidIndex(gridIndex, cellIndex, metadata);
                if (nextIndex < 0) continue;  // No valid next data

                // Perform interpolation
                data[cellIndex] = interpolateCell(
                        metadata.times[gridIndex],
                        metadata.times[prevIndex],
                        ((VortexGrid) reader.getDto(prevIndex)).data()[cellIndex],
                        metadata.times[nextIndex],
                        ((VortexGrid) reader.getDto(nextIndex)).data()[cellIndex]
                );

                cellCount++;
            }
        }

        LOGGER.fine(String.format("Filled %d cells in grid at time %s",
                cellCount, vortexGrid.startTime()));

        return VortexGrid.toBuilder(vortexGrid)
                .data(data)
                .build();
    }

    /**
     * Finds the index of the previous grid with valid data for a specific cell.
     *
     * @param currentIndex The current grid index
     * @param cellIndex    The cell index to check
     * @param metadata     The metadata containing no-data flags
     * @return The index of the previous valid grid, or -1 if none found
     */
    private int findPreviousValidIndex(int currentIndex, int cellIndex, GridMetadata metadata) {
        for (int i = currentIndex - 1; i >= 0; i--) {
            if (metadata.isNoData[i][cellIndex] == 0) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Finds the index of the next grid with valid data for a specific cell.
     *
     * @param currentIndex The current grid index
     * @param cellIndex    The cell index to check
     * @param metadata     The metadata containing no-data flags
     * @return The index of the next valid grid, or -1 if none found
     */
    private int findNextValidIndex(int currentIndex, int cellIndex, GridMetadata metadata) {
        for (int i = currentIndex + 1; i < metadata.isNoData.length; i++) {
            if (metadata.isNoData[i][cellIndex] == 0) {
                return i;
            }
        }
        return -1;
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
        float x = currentTime.toEpochSecond();
        float x1 = prevTime.toEpochSecond();
        float x2 = nextTime.toEpochSecond();

        return interpolate(x, x1, prevValue, x2, nextValue);
    }

    /**
     * Writes a grid to the output destination.
     *
     * @param grid The grid to write
     * @throws Exception If an error occurs during writing
     */
    private void writeGrid(VortexGrid grid) throws Exception {
        List<VortexData> grids = Collections.singletonList(grid);

        DataWriter writer = DataWriter.builder()
                .data(grids)
                .destination(destination)
                .options(writeOptions)
                .build();

        writer.write();
    }

    /**
     * Notifies listeners that the gap filling process has started.
     */
    private void notifyStart() {
        String template = MessageStore.getInstance().getMessage("gap_filler_begin");
        String message = String.format(template);
        support.firePropertyChange(VortexProperty.STATUS.toString(), null, message);
    }

    /**
     * Logs completion information and notifies listeners.
     *
     * @param stopwatch The stopwatch tracking execution time
     * @param processed The number of grids processed
     */
    private void logAndNotifyCompletion(Stopwatch stopwatch, int processed) {
        String timeMessage = "Batch gap-filler time: " + stopwatch;
        LOGGER.info(timeMessage);

        String templateEnd = MessageStore.getInstance().getMessage("gap_filler_end");
        String messageEnd = String.format(templateEnd, processed, destination);
        support.firePropertyChange(VortexProperty.COMPLETE.toString(), null, messageEnd);

        String templateTime = MessageStore.getInstance().getMessage("gap_filler_time");
        String messageTime = String.format(templateTime, stopwatch);
        support.firePropertyChange(VortexProperty.STATUS.toString(), null, messageTime);
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
    private static float interpolate(float x, float x1, float y1, float x2, float y2) {
        if (x1 == x2) {
            throw new IllegalArgumentException("x1 and x2 cannot be the same value (would cause division by zero)");
        }
        return y1 + ((x - x1) * (y2 - y1)) / (x2 - x1);
    }

    /**
     * Inner class to hold metadata about a series of grids.
     */
    private static class GridMetadata {
        final ZonedDateTime[] times;
        final byte[][] isNoData;
        final Set<Integer> hasNoData;
        final Set<Duration> intervals;

        GridMetadata(int size) {
            times = new ZonedDateTime[size];
            isNoData = new byte[size][];
            hasNoData = new LinkedHashSet<>();
            intervals = new HashSet<>();
        }
    }
}