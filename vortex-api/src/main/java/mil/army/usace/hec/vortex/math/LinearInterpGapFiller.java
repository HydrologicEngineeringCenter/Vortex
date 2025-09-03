package mil.army.usace.hec.vortex.math;

import mil.army.usace.hec.vortex.MessageStore;
import mil.army.usace.hec.vortex.VortexDataType;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.VortexProperty;
import mil.army.usace.hec.vortex.io.DataReader;
import mil.army.usace.hec.vortex.io.DataWriter;

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

    private IndexedGrid previousCache;
    private IndexedGrid nextCache;

    /**
     * Constructs a new LinearInterpGapFiller with the specified builder.
     *
     * @param builder The builder containing configuration for this gap filler
     */
    LinearInterpGapFiller(Builder builder) {
        super(builder);
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
        if (processed <= 0)
            return null;

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
                String template = MessageStore.INSTANCE.getMessage("linear_interp_filler_error_no_data");
                String message = String.format(template, variable);
                LOGGER.info(() -> message);
                support.firePropertyChange(VortexProperty.STATUS.toString(), null, message);
                return 0;
            }

            // First pass: analyze grid data and build metadata
            String template1 = MessageStore.INSTANCE.getMessage("linear_interp_filler_info_analyzing");
            String message1 = String.format(template1, dtoCount, variable);
            LOGGER.fine(() -> message1);
            GridMetadata metadata = analyzeGridData(reader);

            if (metadata.hasNoData.isEmpty()) {
                String template2 = MessageStore.INSTANCE.getMessage("linear_interp_filler_info_none");
                String message2 = String.format(template2, variable);
                LOGGER.fine(() -> message2);
                support.firePropertyChange(VortexProperty.STATUS.toString(), null, message2);

                // Copy existing grids to destination if needed
                if (!isSourceEqualToDestination) {
                    try {
                        copyExistingGrids(variable);
                    } catch (Exception e) {
                        String template = MessageStore.INSTANCE.getMessage("time_step_filler_error_copy");
                        String message = String.format(template, source, destination);
                        LOGGER.log(Level.SEVERE, message, e);
                        support.firePropertyChange(VortexProperty.ERROR.toString(), null, message);
                    }
                }
            } else {
                // Second pass: process grids
                String template2 = MessageStore.INSTANCE.getMessage("linear_interp_filler_info_processing");
                String message2 = String.format(template2, metadata.hasNoData.size(), variable);
                LOGGER.fine(() -> message2);
                processGrids(reader, metadata, processedCount);
            }
        }

        return processedCount.get();
    }

    /**
     * Processes grids in batches to improve memory efficiency.
     *
     * @param reader         The data reader
     * @param metadata       Metadata about the grid series
     * @param processedCount Counter for processed grids
     */
    private void processGrids(DataReader reader, GridMetadata metadata,
                                AtomicInteger processedCount) {

        String variable = reader.getVariableName();

        int progress = 0;
        int size = metadata.indices.size();

        // Process each grid in the batch
        for (int i = 0; i < size; i++) {
            int index = metadata.indices.get(i);

            ZonedDateTime startTime = metadata.startTimes.get(i);

            VortexGrid grid = index >= 0 ? (VortexGrid) reader.getDto(i) : createEmptyGrid(metadata, startTime);

            // Only process grids that need gap filling
            if (metadata.hasNoData.contains(startTime)) {
                VortexGrid filledGrid = fillGridGaps(grid, metadata, reader);
                writeGrid(filledGrid);
                processedCount.incrementAndGet();
            } else if (!isSourceEqualToDestination) {
                writeGrid(grid);
            }

            int variableIndex = variables.indexOf(variable);
            float variableProgress = (float) variableIndex / variables.size();

            // Update progress for missing time steps
            float gridProgress = (float) progress++ / size / variables.size();
            int progressPercent = (int) ((variableProgress + gridProgress) * 100);
            support.firePropertyChange(VortexProperty.PROGRESS.toString(), null, progressPercent);
        }
    }

    private static VortexGrid createEmptyGrid(GridMetadata metadata, ZonedDateTime startTime) {
        double noDataValue = metadata.getNoDataValue();
        float[] data = new float[metadata.getSize()];
        Arrays.fill(data, (float) noDataValue);

        ZonedDateTime endTime = startTime.plus(metadata.getInterval());

        return VortexGrid.builder()
                .startTime(startTime)
                .endTime(endTime)
                .data(data)
                .noDataValue(noDataValue)
                .wkt(metadata.getWkt())
                .originX(metadata.getOriginX())
                .originY(metadata.getOriginY())
                .nx(metadata.getNx())
                .ny(metadata.getNy())
                .dx(metadata.getDx())
                .dy(metadata.getDy())
                .units(metadata.getUnits())
                .shortName(metadata.getShortName())
                .interval(metadata.getInterval())
                .dataType(metadata.getVortexDataType())
                .build();
    }

    /**
     * Copies existing grids to the destination if source and destination are different.
     *
     * @param variable The variable being processed
     * @throws Exception If writing fails
     */
    private void copyExistingGrids(String variable) throws Exception {
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
            }
        }
    }

    /**
     * Analyzes all grids for a variable to collect metadata needed for interpolation.
     *
     * @param reader The data reader
     * @return Metadata about the grid series
     */
    protected GridMetadata analyzeGridData(DataReader reader) {
        int dtoCount = reader.getDtoCount();
        GridMetadata metadata = new GridMetadata();

        for (int i = 0; i < dtoCount; i++) {
            VortexGrid vortexGrid = (VortexGrid) reader.getDto(i);

            metadata.indices.add(i);
            metadata.startTimes.add(vortexGrid.startTime());

            float[] data = vortexGrid.data();
            metadata.sizes.add(data.length);

            double noDataValue = vortexGrid.noDataValue();
            metadata.noDataValues.add(noDataValue);

            metadata.originX.add(vortexGrid.originX());
            metadata.originY.add(vortexGrid.originY());
            metadata.dx.add(vortexGrid.dx());
            metadata.dy.add(vortexGrid.dy());
            metadata.nx.add(vortexGrid.nx());
            metadata.ny.add(vortexGrid.ny());
            metadata.wkt.add(vortexGrid.wkt());
            metadata.units.add(vortexGrid.units());
            metadata.shortName.add(vortexGrid.shortName());
            metadata.dataType.add(vortexGrid.dataType());

            // Track which cells have missing data
            metadata.isNoData.add(i, new byte[data.length]);

            for (int j = 0; j < data.length; j++) {
                if (Double.compare(noDataValue, data[j]) == 0) {
                    metadata.isNoData.get(i)[j] = 1;
                    metadata.hasNoData.add(vortexGrid.startTime());
                    // Track cells that need interpolation across the time series
                    metadata.cellsNeedingInterpolation.add(j);
                }
            }
        }

        return metadata;
    }

    /**
     * Fills gaps in a single grid using linear interpolation from surrounding temporal data.
     *
     * @param vortexGrid The grid to fill
     * @param metadata   Metadata about the grid series
     * @param reader     The data reader (to access grids outside the current batch)
     * @return A new grid with gaps filled where possible
     */
    private VortexGrid fillGridGaps(VortexGrid vortexGrid, GridMetadata metadata, DataReader reader) {
        ZonedDateTime startTime = vortexGrid.startTime();
        int gridIndex = metadata.startTimes.indexOf(startTime);

        float[] data = vortexGrid.data().clone();  // Clone to avoid modifying original
        byte[] isNoDataI = metadata.isNoData.get(gridIndex);
        int cellCount = 0;

        // Process only cells that need interpolation (performance optimization)
        for (int cellIndex : metadata.cellsNeedingInterpolation) {
            if (isNoDataI[cellIndex] == 1) {
                // Find previous valid data point
                TimeValuePair previous = findPreviousValidValue(gridIndex, cellIndex, metadata, reader);
                if (previous == null) continue;  // No valid previous data

                // Find next valid data point
                TimeValuePair next = findNextValidValue(gridIndex, cellIndex, metadata, reader);
                if (next == null) continue;  // No valid next data

                // Perform interpolation
                data[cellIndex] = interpolateCell(
                        startTime,
                        previous.time,
                        previous.value,
                        next.time,
                        next.value
                );

                cellCount++;
            }
        }

        if (cellCount > 0) {
            String template = MessageStore.INSTANCE.getMessage("linear_interp_filler_info_filled");
            String message = String.format(template, cellCount, vortexGrid.startTime());
            LOGGER.fine(() -> message);
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
     * @param reader       The data reader for loading additional grids if needed
     * @return A TimeValuePair containing the previous valid value and its time, or null if none found
     */
    private TimeValuePair findPreviousValidValue(int currentIndex, int cellIndex,
                                                 GridMetadata metadata,
                                                 DataReader reader) {
        for (int i = currentIndex - 1; i >= 0; i--) {
            if (metadata.isNoData.get(i)[cellIndex] == 0) {
                VortexGrid grid;
                if (previousCache != null && previousCache.index == i) {
                    grid = previousCache.vortexGrid;
                } else {
                    // Need to load grid from reader
                    int gridIndex = metadata.indices.get(i);
                    grid = (VortexGrid) reader.getDto(gridIndex);
                    previousCache = new IndexedGrid(i, grid);
                }
                float value = grid.data()[cellIndex];

                return new TimeValuePair(metadata.startTimes.get(i), value);
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
     * @param reader       The data reader for loading additional grids if needed
     * @return A TimeValuePair containing the next valid value and its time, or null if none found
     */
    private TimeValuePair findNextValidValue(int currentIndex, int cellIndex,
                                             GridMetadata metadata,
                                             DataReader reader) {
        for (int i = currentIndex + 1; i < metadata.isNoData.size(); i++) {
            if (metadata.isNoData.get(i)[cellIndex] == 0) {
                VortexGrid grid;
                if (nextCache != null && nextCache.index == i) {
                    grid = nextCache.vortexGrid;
                } else {
                    // Need to load grid from reader
                    int gridIndex = metadata.indices.get(i);
                    grid = (VortexGrid) reader.getDto(gridIndex);
                    nextCache = new IndexedGrid(i, grid);
                }
                float value = grid.data()[cellIndex];

                return new TimeValuePair(metadata.startTimes.get(i), value);
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

    private record IndexedGrid(int index, VortexGrid vortexGrid) {
    }

    /**
     * Inner class to hold metadata about a series of grids.
     */
    protected static class GridMetadata {
        final List<ZonedDateTime> startTimes = new ArrayList<>();
        final List<Integer> indices = new ArrayList<>();
        final Set<ZonedDateTime> hasNoData = new HashSet<>();
        final Set<Integer> cellsNeedingInterpolation = new HashSet<>();
        final List<byte[]> isNoData = new ArrayList<>();

        final Set<Double> noDataValues = new HashSet<>();
        final Set<Duration> intervals = new HashSet<>();
        final Set<Integer> sizes = new HashSet<>();
        final Set<Double> dx = new HashSet<>();
        final Set<Double> dy = new HashSet<>();
        final Set<Integer> nx = new HashSet<>();
        final Set<Integer> ny = new HashSet<>();
        final Set<Double> originX = new HashSet<>();
        final Set<Double> originY = new HashSet<>();
        final Set<String> wkt = new HashSet<>();
        final Set<String> units = new HashSet<>();
        final Set<String> shortName = new HashSet<>();
        final Set<VortexDataType> dataType = new HashSet<>();

        GridMetadata() {
        }

        private Duration getInterval() {
            if (intervals.size() != 1) {
                throw new IllegalStateException("Intervals size must be 1");
            }
            return intervals.iterator().next();
        }

        private int getSize() {
            if (sizes.size() != 1) {
                throw new IllegalStateException("Sizes size must be 1");
            }
            return sizes.iterator().next();
        }

        private double getNoDataValue() {
            if (noDataValues.size() != 1) {
                throw new IllegalStateException("No data values size must be 1");
            }
            return noDataValues.iterator().next();
        }

        private double getOriginX() {
            if (originX.size() != 1) {
                throw new IllegalStateException("originX size must be 1");
            }
            return originX.iterator().next();
        }

        private double getOriginY() {
            if (originY.size() != 1) {
                throw new IllegalStateException("originY size must be 1");
            }
            return originY.iterator().next();
        }

        private double getDx() {
            if (dx.size() != 1) {
                throw new IllegalStateException("dx size must be 1");
            }
            return dx.iterator().next();
        }

        private double getDy() {
            if (dy.size() != 1) {
                throw new IllegalStateException("dy size must be 1");
            }
            return dy.iterator().next();
        }

        private int getNx() {
            if (nx.size() != 1) {
                throw new IllegalStateException("nx size must be 1");
            }
            return nx.iterator().next();
        }

        private int getNy() {
            if (ny.size() != 1) {
                throw new IllegalStateException("ny size must be 1");
            }
            return ny.iterator().next();
        }

        private String getWkt() {
            if (wkt.size() != 1) {
                throw new IllegalStateException("wkt size must be 1");
            }
            return wkt.iterator().next();
        }

        private String getUnits() {
            if (units.size() != 1) {
                throw new IllegalStateException("units size must be 1");
            }
            return units.iterator().next();
        }

        private String getShortName() {
            if (shortName.size() != 1) {
                throw new IllegalStateException("shortName size must be 1");
            }
            return shortName.iterator().next();
        }

        private VortexDataType getVortexDataType() {
            if (dataType.size() != 1) {
                throw new IllegalStateException("dataType size must be 1");
            }
            return dataType.iterator().next();
        }
    }
}