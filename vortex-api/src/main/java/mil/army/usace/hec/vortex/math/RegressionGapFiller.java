package mil.army.usace.hec.vortex.math;

import mil.army.usace.hec.vortex.Message;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.VortexProperty;
import mil.army.usace.hec.vortex.io.DataReadException;
import mil.army.usace.hec.vortex.io.DataReader;
import org.tribuo.Example;
import org.tribuo.Feature;
import org.tribuo.MutableDataset;
import org.tribuo.Prediction;
import org.tribuo.datasource.ListDataSource;
import org.tribuo.impl.ArrayExample;
import org.tribuo.provenance.SimpleDataSourceProvenance;
import org.tribuo.regression.RegressionFactory;
import org.tribuo.regression.Regressor;
import org.tribuo.regression.rtree.CARTRegressionTrainer;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Fills gaps in gridded time series using a CART regression tree trained on
 * spatiotemporal features. For each grid with gaps, valid cells across the
 * current grid and its temporal neighbors are used as training data.
 * Falls back to {@link FocalMeanGapFiller} for grids with too few valid cells.
 */
class RegressionGapFiller extends BatchGapFiller {

    private static final Logger LOGGER = Logger.getLogger(RegressionGapFiller.class.getName());
    private static final int MIN_TRAINING_SAMPLES = 10;
    private static final int MAX_TRAINING_SAMPLES = 5000;
    private static final int MAX_DEPTH = 8;
    private static final String OUTPUT_NAME = "value";

    private final RegressionFactory factory = new RegressionFactory();
    private final CARTRegressionTrainer trainer = new CARTRegressionTrainer(MAX_DEPTH);

    RegressionGapFiller(Builder builder) {
        super(builder);
    }

    @Override
    protected String notifyStartMessage() {
        return Message.format("regression_filler_begin");
    }

    @Override
    protected String notifyCompleteMessage(int processed) {
        if (processed <= 0) return null;
        return Message.format("regression_filler_end", processed, destination);
    }

    @Override
    protected int processVariable(String variable) throws Exception {
        try (DataReader reader = DataReader.builder()
                .path(source)
                .variable(variable)
                .build()) {

            int dtoCount = reader.getDtoCount();
            if (dtoCount == 0) {
                String message = Message.format("regression_filler_error_no_data", variable);
                LOGGER.info(() -> message);
                support.firePropertyChange(VortexProperty.STATUS.toString(), null, message);
                return 0;
            }

            GridMetadata metadata = analyzeGridData(reader);

            if (metadata.hasNoData.isEmpty()) {
                String message = Message.format("regression_filler_info_none", variable);
                LOGGER.fine(() -> message);
                support.firePropertyChange(VortexProperty.STATUS.toString(), null, message);
                copyGridsIfNeeded(variable);
                return 0;
            }

            String processMsg = Message.format("regression_filler_info_processing", metadata.hasNoData.size(), variable);
            LOGGER.fine(() -> processMsg);
            return processGrids(reader, metadata);
        }
    }

    private GridMetadata analyzeGridData(DataReader reader) throws DataReadException {
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

    private int processGrids(DataReader reader, GridMetadata metadata) throws DataReadException {
        String variable = reader.getVariableName();
        int variableIndex = variables.indexOf(variable);
        float variableProgress = (float) variableIndex / variables.size();

        int size = metadata.indices.size();
        int processedCount = 0;

        // Sliding window cache: keeps grids at indices [i-2..i+2] to avoid re-reads.
        // Key = grid index in metadata, Value = VortexGrid
        Map<Integer, VortexGrid> cache = new HashMap<>();

        for (int i = 0; i < size; i++) {
            ZonedDateTime startTime = metadata.startTimes.get(i);
            VortexGrid grid = cachedGetGrid(i, metadata, reader, cache);

            if (metadata.hasNoData.contains(startTime)) {
                VortexGrid prevGrid = cachedGetGrid(i - 1, metadata, reader, cache);
                VortexGrid nextGrid = cachedGetGrid(i + 1, metadata, reader, cache);
                VortexGrid prev2Grid = cachedGetGrid(i - 2, metadata, reader, cache);
                VortexGrid next2Grid = cachedGetGrid(i + 2, metadata, reader, cache);

                VortexGrid filled = fillGridGaps(grid, i, size, prevGrid, nextGrid, prev2Grid, next2Grid);
                writeGrid(filled);
                processedCount++;
            } else if (!isSourceEqualToDestination) {
                writeGrid(grid);
            }

            // Evict entries that are no longer reachable (more than 2 behind current position)
            int evictBefore = i - 2;
            cache.keySet().removeIf(k -> k < evictBefore);

            float gridProgress = (float) i / size / variables.size();
            int progressPercent = (int) ((variableProgress + gridProgress) * 100);
            support.firePropertyChange(VortexProperty.PROGRESS.toString(), null, progressPercent);
        }

        return processedCount;
    }

    private static VortexGrid cachedGetGrid(int index, GridMetadata metadata,
                                             DataReader reader, Map<Integer, VortexGrid> cache) throws DataReadException {
        int size = metadata.indices.size();
        if (index < 0 || index >= size) {
            return null;
        }

        VortexGrid grid = cache.get(index);
        if (grid == null) {
            grid = (VortexGrid) reader.getDto(metadata.indices.get(index));
            cache.put(index, grid);
        }
        return grid;
    }

    VortexGrid fillGridGaps(VortexGrid grid, int gridIndex, int totalGrids,
                             VortexGrid prevGrid, VortexGrid nextGrid,
                             VortexGrid prev2Grid, VortexGrid next2Grid) {
        int nx = grid.nx();
        int ny = grid.ny();
        float[] data = grid.data();
        float noDataValue = (float) grid.noDataValue();

        float[] prevData = prevGrid != null ? prevGrid.data() : null;
        float[] nextData = nextGrid != null ? nextGrid.data() : null;
        float[] prev2Data = prev2Grid != null ? prev2Grid.data() : null;
        float[] next2Data = next2Grid != null ? next2Grid.data() : null;
        double normTime = totalGrids > 1 ? (double) gridIndex / (totalGrids - 1) : 0.5;

        List<int[]> validCells = new ArrayList<>();
        List<int[]> gapCells = new ArrayList<>();

        for (int i = 0; i < data.length; i++) {
            int row = i / nx;
            int col = i % nx;
            if (Float.compare(data[i], noDataValue) == 0) {
                gapCells.add(new int[]{row, col, i});
            } else {
                validCells.add(new int[]{row, col, i});
            }
        }

        if (gapCells.isEmpty()) return grid;

        if (validCells.isEmpty() || validCells.size() < MIN_TRAINING_SAMPLES) {
            return FocalMeanGapFiller.newInstance().fill(grid);
        }

        float globalMean = computeGlobalMean(data, validCells);

        // Subsample training data for large grids to speed up CART training
        List<int[]> trainingCells = validCells;
        if (validCells.size() > MAX_TRAINING_SAMPLES) {
            trainingCells = new ArrayList<>(validCells);
            Collections.shuffle(trainingCells, new Random(gridIndex));
            trainingCells = trainingCells.subList(0, MAX_TRAINING_SAMPLES);
        }

        List<Example<Regressor>> trainingExamples = new ArrayList<>(trainingCells.size());

        for (int[] cell : trainingCells) {
            int row = cell[0];
            int col = cell[1];
            int idx = cell[2];
            trainingExamples.add(createExample(
                    row, col, data[idx], data, nx, ny, noDataValue, globalMean,
                    prevData, nextData, prev2Data, next2Data, normTime));
        }

        MutableDataset<Regressor> dataset = new MutableDataset<>(
                new ListDataSource<>(trainingExamples, factory,
                        new SimpleDataSourceProvenance("grid-gap-fill", factory)));

        var model = trainer.train(dataset);

        float[] filled = data.clone();
        int cellCount = 0;

        for (int[] cell : gapCells) {
            int row = cell[0];
            int col = cell[1];
            int idx = cell[2];
            Example<Regressor> example = createExample(
                    row, col, 0f, data, nx, ny, noDataValue, globalMean,
                    prevData, nextData, prev2Data, next2Data, normTime);
            Prediction<Regressor> prediction = model.predict(example);
            filled[idx] = (float) prediction.getOutput().getValues()[0];
            cellCount++;
        }

        if (cellCount > 0) {
            String message = Message.format("regression_filler_info_filled", cellCount, grid.startTime());
            LOGGER.fine(() -> message);
        }

        return VortexGrid.toBuilder(grid).data(filled).build();
    }

    private static Example<Regressor> createExample(int row, int col, float value,
                                                     float[] data, int nx, int ny,
                                                     float noDataValue, float globalMean,
                                                     float[] prevData, float[] nextData,
                                                     float[] prev2Data, float[] next2Data,
                                                     double normTime) {
        double normRow = ny > 1 ? (double) row / (ny - 1) : 0.5;
        double normCol = nx > 1 ? (double) col / (nx - 1) : 0.5;
        int idx = row * nx + col;

        double[] neighborStats = computeNeighborStats(data, nx, ny, row, col, noDataValue, globalMean);

        double prevValue = getTemporalValue(prevData, idx, noDataValue, globalMean);
        double nextValue = getTemporalValue(nextData, idx, noDataValue, globalMean);
        double prev2Value = getTemporalValue(prev2Data, idx, noDataValue, globalMean);
        double next2Value = getTemporalValue(next2Data, idx, noDataValue, globalMean);
        double temporalAvailable = (prevData != null ? 1.0 : 0.0) + (nextData != null ? 1.0 : 0.0)
                + (prev2Data != null ? 1.0 : 0.0) + (next2Data != null ? 1.0 : 0.0);

        Regressor target = new Regressor(OUTPUT_NAME, value);
        ArrayExample<Regressor> example = new ArrayExample<>(target);

        // Spatial features
        example.add(new Feature("norm_row", normRow));
        example.add(new Feature("norm_col", normCol));
        example.add(new Feature("row_sq", normRow * normRow));
        example.add(new Feature("col_sq", normCol * normCol));
        example.add(new Feature("row_col", normRow * normCol));
        example.add(new Feature("dist_center", computeDistCenter(normRow, normCol)));
        example.add(new Feature("neighbor_mean", neighborStats[0]));
        example.add(new Feature("neighbor_count", neighborStats[1]));

        // Temporal features
        example.add(new Feature("prev_value", prevValue));
        example.add(new Feature("next_value", nextValue));
        example.add(new Feature("prev2_value", prev2Value));
        example.add(new Feature("next2_value", next2Value));
        example.add(new Feature("norm_time", normTime));
        example.add(new Feature("temporal_available", temporalAvailable));

        return example;
    }

    private static double getTemporalValue(float[] temporalData, int idx,
                                            float noDataValue, float globalMean) {
        if (temporalData == null) return globalMean;
        float v = temporalData[idx];
        return Float.compare(v, noDataValue) == 0 ? globalMean : v;
    }

    private static double computeDistCenter(double normRow, double normCol) {
        double dr = normRow - 0.5;
        double dc = normCol - 0.5;
        return Math.sqrt(dr * dr + dc * dc);
    }

    private static double[] computeNeighborStats(float[] data, int nx, int ny,
                                                  int row, int col,
                                                  float noDataValue, float globalMean) {
        float sum = 0;
        int count = 0;

        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                int r = row + dr;
                int c = col + dc;
                if (r < 0 || r >= ny || c < 0 || c >= nx) continue;
                float v = data[r * nx + c];
                if (Float.compare(v, noDataValue) != 0) {
                    sum += v;
                    count++;
                }
            }
        }

        double mean = count > 0 ? sum / count : globalMean;
        return new double[]{mean, count};
    }

    private static float computeGlobalMean(float[] data, List<int[]> validCells) {
        double sum = 0;
        for (int[] cell : validCells) {
            sum += data[cell[2]];
        }
        return (float) (sum / validCells.size());
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
            String message = Message.format("regression_filler_error_copy", source, destination);
            LOGGER.log(Level.SEVERE, message, e);
            support.firePropertyChange(VortexProperty.ERROR.toString(), null, message);
        }
    }
}
