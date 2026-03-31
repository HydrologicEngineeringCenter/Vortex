package mil.army.usace.hec.vortex.math;

import mil.army.usace.hec.vortex.Message;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.VortexProperty;
import mil.army.usace.hec.vortex.io.DataReader;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Fills gaps (missing data) in time series grids using linear interpolation
 * between the nearest valid temporal neighbors for each cell.
 */
class LinearInterpGapFiller extends BatchGapFiller {

    private static final Logger LOGGER = Logger.getLogger(LinearInterpGapFiller.class.getName());

    LinearInterpGapFiller(Builder builder) {
        super(builder);
    }

    @Override
    protected String notifyStartMessage() {
        return Message.format("linear_interp_filler_begin");
    }

    @Override
    protected String notifyCompleteMessage(int processed) {
        if (processed <= 0)
            return null;

        return Message.format("linear_interp_filler_end", processed, destination);
    }

    @Override
    protected int processVariable(String variable) throws Exception {
        try (DataReader reader = DataReader.builder()
                .path(source)
                .variable(variable)
                .build()) {

            int dtoCount = reader.getDtoCount();
            if (dtoCount == 0) {
                String message = Message.format("linear_interp_filler_error_no_data", variable);
                LOGGER.info(() -> message);
                support.firePropertyChange(VortexProperty.STATUS.toString(), null, message);
                return 0;
            }

            String analyzeMsg = Message.format("linear_interp_filler_info_analyzing", dtoCount, variable);
            LOGGER.fine(() -> analyzeMsg);

            GridMetadata metadata = analyzeGridData(reader);

            if (metadata.hasNoData.isEmpty()) {
                String noneMsg = Message.format("linear_interp_filler_info_none", variable);
                LOGGER.fine(() -> noneMsg);
                support.firePropertyChange(VortexProperty.STATUS.toString(), null, noneMsg);
                copyGridsIfNeeded(variable);
                return 0;
            }

            String processMsg = Message.format("linear_interp_filler_info_processing", metadata.hasNoData.size(), variable);
            LOGGER.fine(() -> processMsg);
            return processGrids(reader, metadata);
        }
    }

    private void copyGridsIfNeeded(String variable) {
        if (isSourceEqualToDestination) return;

        try (DataReader reader = DataReader.builder()
                .path(source)
                .variable(variable)
                .build()) {

            int dtoCount = reader.getDtoCount();
            for (int i = 0; i < dtoCount; i++) {
                writeGrid((VortexGrid) reader.getDto(i));
            }
        } catch (Exception e) {
            String message = Message.format("time_step_filler_error_copy", source, destination);
            LOGGER.log(Level.SEVERE, message, e);
            support.firePropertyChange(VortexProperty.ERROR.toString(), null, message);
        }
    }

    private int processGrids(DataReader reader, GridMetadata metadata) {
        String variable = reader.getVariableName();
        int variableIndex = variables.indexOf(variable);
        float variableProgress = (float) variableIndex / variables.size();

        int size = metadata.indices.size();
        int processedCount = 0;
        VortexGrid cachedGrid = null;
        int cachedIndex = -1;

        for (int i = 0; i < size; i++) {
            int readerIndex = metadata.indices.get(i);
            ZonedDateTime startTime = metadata.startTimes.get(i);

            VortexGrid grid;
            if (readerIndex < 0) {
                grid = createEmptyGrid(metadata, startTime);
            } else if (readerIndex == cachedIndex && cachedGrid != null) {
                grid = cachedGrid;
            } else {
                grid = (VortexGrid) reader.getDto(readerIndex);
                cachedGrid = grid;
                cachedIndex = readerIndex;
            }

            if (metadata.hasNoData.contains(startTime)) {
                VortexGrid filled = fillGridGaps(grid, i, metadata, reader);
                writeGrid(filled);
                processedCount++;
            } else if (!isSourceEqualToDestination) {
                writeGrid(grid);
            }

            float gridProgress = (float) i / size / variables.size();
            int progressPercent = (int) ((variableProgress + gridProgress) * 100);
            support.firePropertyChange(VortexProperty.PROGRESS.toString(), null, progressPercent);
        }

        return processedCount;
    }

    private VortexGrid fillGridGaps(VortexGrid grid, int gridIndex,
                                    GridMetadata metadata, DataReader reader) {
        float[] data = grid.data().clone();
        byte[] noDataFlags = metadata.isNoData.get(gridIndex);
        long currentEpoch = grid.startTime().toEpochSecond();
        int cellCount = 0;

        VortexGrid prevGrid = null;
        int prevGridIdx = -1;
        VortexGrid nextGrid = null;
        int nextGridIdx = -1;

        for (int cellIndex : metadata.cellsNeedingInterpolation) {
            if (noDataFlags[cellIndex] != 1) continue;

            int prevIdx = findValidIndex(gridIndex, cellIndex, metadata, -1);
            if (prevIdx < 0) continue;

            int nextIdx = findValidIndex(gridIndex, cellIndex, metadata, 1);
            if (nextIdx < 0) continue;

            if (prevIdx != prevGridIdx) {
                prevGrid = (VortexGrid) reader.getDto(metadata.indices.get(prevIdx));
                prevGridIdx = prevIdx;
            }
            if (nextIdx != nextGridIdx) {
                nextGrid = (VortexGrid) reader.getDto(metadata.indices.get(nextIdx));
                nextGridIdx = nextIdx;
            }

            long prevEpoch = metadata.startTimes.get(prevIdx).toEpochSecond();
            long nextEpoch = metadata.startTimes.get(nextIdx).toEpochSecond();
            float prevValue = prevGrid.data()[cellIndex];
            float nextValue = nextGrid.data()[cellIndex];

            data[cellIndex] = interpolate(currentEpoch, prevEpoch, prevValue, nextEpoch, nextValue);
            cellCount++;
        }

        if (cellCount > 0) {
            String message = Message.format("linear_interp_filler_info_filled", cellCount, grid.startTime());
            LOGGER.fine(() -> message);
        }

        return VortexGrid.toBuilder(grid).data(data).build();
    }

    private static int findValidIndex(int fromIndex, int cellIndex,
                                      GridMetadata metadata, int direction) {
        int size = metadata.isNoData.size();
        for (int i = fromIndex + direction; i >= 0 && i < size; i += direction) {
            if (metadata.isNoData.get(i)[cellIndex] == 0) {
                return i;
            }
        }
        return -1;
    }

    private static float interpolate(long x, long x1, float y1, long x2, float y2) {
        if (x1 == x2) {
            throw new IllegalArgumentException("Interpolation endpoints have the same time");
        }
        return y1 + ((x - x1) * (y2 - y1)) / (x2 - x1);
    }

    static VortexGrid createEmptyGrid(GridMetadata metadata, ZonedDateTime startTime) {
        double noDataValue = metadata.getNoDataValue();
        float[] data = new float[metadata.getSize()];
        Arrays.fill(data, (float) noDataValue);

        return VortexGrid.builder()
                .startTime(startTime)
                .endTime(startTime.plus(metadata.getInterval()))
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

    protected GridMetadata analyzeGridData(DataReader reader) {
        int dtoCount = reader.getDtoCount();
        GridMetadata metadata = new GridMetadata();

        for (int i = 0; i < dtoCount; i++) {
            VortexGrid grid = (VortexGrid) reader.getDto(i);

            metadata.indices.add(i);
            metadata.startTimes.add(grid.startTime());

            float[] data = grid.data();
            metadata.sizes.add(data.length);

            double noDataValue = grid.noDataValue();
            metadata.noDataValues.add(noDataValue);

            metadata.originX.add(grid.originX());
            metadata.originY.add(grid.originY());
            metadata.dx.add(grid.dx());
            metadata.dy.add(grid.dy());
            metadata.nx.add(grid.nx());
            metadata.ny.add(grid.ny());
            metadata.wkt.add(grid.wkt());
            metadata.units.add(grid.units());
            metadata.shortName.add(grid.shortName());
            metadata.dataType.add(grid.dataType());

            byte[] noDataFlags = new byte[data.length];
            metadata.isNoData.add(noDataFlags);

            for (int j = 0; j < data.length; j++) {
                if (Double.compare(noDataValue, data[j]) == 0) {
                    noDataFlags[j] = 1;
                    metadata.hasNoData.add(grid.startTime());
                    metadata.cellsNeedingInterpolation.add(j);
                }
            }
        }

        return metadata;
    }
}
