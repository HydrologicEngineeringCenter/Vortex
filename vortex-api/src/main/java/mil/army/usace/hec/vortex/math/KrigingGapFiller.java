package mil.army.usace.hec.vortex.math;

import mil.army.usace.hec.vortex.Message;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.VortexProperty;
import mil.army.usace.hec.vortex.io.DataReadException;
import mil.army.usace.hec.vortex.io.DataReader;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Fills gaps in gridded time series using ordinary kriging over spatiotemporal
 * neighbors. For each grid with gaps, a spherical variogram model is fit from
 * valid cells in the current grid and its temporal neighbors (+/-1 and +/-2
 * time steps). Each gap cell is then predicted by solving a local ordinary
 * kriging system built from its nearest valid neighbors.
 * Falls back to {@link FocalMeanGapFiller} for grids with too few valid cells.
 */
class KrigingGapFiller extends BatchGapFiller {

    private static final Logger LOGGER = Logger.getLogger(KrigingGapFiller.class.getName());
    private static final int MIN_TRAINING_SAMPLES = 10;
    private static final int MAX_VARIOGRAM_SAMPLES = 300;
    private static final int MAX_VARIOGRAM_PAIRS = MAX_VARIOGRAM_SAMPLES * 4;
    private static final int MIN_NEIGHBORS = 8;
    private static final int MAX_NEIGHBORS = 24;
    private static final int INITIAL_SEARCH_RADIUS = 3;
    private static final int SEARCH_RADIUS_INCREMENT = 2;
    private static final int NUM_LAG_BINS = 15;
    private static final double TIME_WEIGHT = 1.0;

    KrigingGapFiller(Builder builder) {
        super(builder);
    }

    @Override
    protected String notifyStartMessage() {
        return Message.format("kriging_filler_begin");
    }

    @Override
    protected String notifyCompleteMessage(int processed) {
        if (processed <= 0) return null;
        return Message.format("kriging_filler_end", processed, destination);
    }

    @Override
    protected int processVariable(String variable) throws Exception {
        try (DataReader reader = DataReader.builder()
                .path(source)
                .variable(variable)
                .build()) {

            int dtoCount = reader.getDtoCount();
            if (dtoCount == 0) {
                String message = Message.format("kriging_filler_error_no_data", variable);
                LOGGER.info(() -> message);
                support.firePropertyChange(VortexProperty.STATUS.toString(), null, message);
                return 0;
            }

            GridMetadata metadata = analyzeGridData(reader);

            if (metadata.hasNoData.isEmpty()) {
                String message = Message.format("kriging_filler_info_none", variable);
                LOGGER.fine(() -> message);
                support.firePropertyChange(VortexProperty.STATUS.toString(), null, message);
                copyGridsIfNeeded(variable);
                return 0;
            }

            String processMsg = Message.format("kriging_filler_info_processing", metadata.hasNoData.size(), variable);
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

        float[][] temporalData = {data, prevData, nextData, prev2Data, next2Data};
        double[] temporalOffsets = {0.0, -1.0, 1.0, -2.0, 2.0};

        Variogram variogram = fitVariogram(validCells, temporalData, temporalOffsets, nx, ny, noDataValue, gridIndex);

        float[] filled = data.clone();
        int cellCount = 0;

        if (variogram == null) {
            // Near-constant field: no spatial structure to krige against, fall back to the mean.
            float mean = computeGlobalMean(data, validCells);
            for (int[] cell : gapCells) {
                filled[cell[2]] = mean;
                cellCount++;
            }
        } else {
            int maxRadius = Math.max(nx, ny);

            for (int[] cell : gapCells) {
                int row = cell[0];
                int col = cell[1];
                int idx = cell[2];

                List<Neighbor> neighbors = findNeighbors(row, col, temporalData, temporalOffsets,
                        nx, ny, noDataValue, maxRadius);

                filled[idx] = (float) krige(neighbors, variogram);
                cellCount++;
            }
        }

        if (cellCount > 0) {
            String message = Message.format("kriging_filler_info_filled", cellCount, grid.startTime());
            LOGGER.fine(() -> message);
        }

        return VortexGrid.toBuilder(grid).data(filled).build();
    }

    private static List<Neighbor> findNeighbors(int row, int col, float[][] temporalData, double[] temporalOffsets,
                                                 int nx, int ny, float noDataValue, int maxRadius) {
        double gapNormRow = normCoord(row, ny);
        double gapNormCol = normCoord(col, nx);

        List<Neighbor> found = new ArrayList<>();
        int radius = INITIAL_SEARCH_RADIUS;

        while (true) {
            found.clear();
            int rowStart = Math.max(0, row - radius);
            int rowEnd = Math.min(ny - 1, row + radius);
            int colStart = Math.max(0, col - radius);
            int colEnd = Math.min(nx - 1, col + radius);

            for (int t = 0; t < temporalData.length; t++) {
                float[] tData = temporalData[t];
                if (tData == null) continue;
                double normTime = normTime(temporalOffsets[t]);

                for (int r = rowStart; r <= rowEnd; r++) {
                    int rowOffset = r * nx;
                    for (int c = colStart; c <= colEnd; c++) {
                        if (t == 0 && r == row && c == col) continue;
                        float value = tData[rowOffset + c];
                        if (Float.compare(value, noDataValue) == 0) continue;

                        double nr = normCoord(r, ny);
                        double nc = normCoord(c, nx);
                        double dist = distance(gapNormRow, gapNormCol, 0.0, nr, nc, normTime);
                        found.add(new Neighbor(nr, nc, normTime, value, dist));
                    }
                }
            }

            if (found.size() >= MIN_NEIGHBORS || radius >= maxRadius) break;
            radius += SEARCH_RADIUS_INCREMENT;
        }

        found.sort(Comparator.comparingDouble(n -> n.distance));
        if (found.size() > MAX_NEIGHBORS) {
            return new ArrayList<>(found.subList(0, MAX_NEIGHBORS));
        }
        return found;
    }

    private static double krige(List<Neighbor> neighbors, Variogram variogram) {
        int k = neighbors.size();
        if (k == 0) return 0.0;
        if (k == 1) return neighbors.get(0).value;

        int n = k + 1;
        double[][] gamma = new double[n][n];
        double[] rhs = new double[n];

        for (int i = 0; i < k; i++) {
            Neighbor ni = neighbors.get(i);
            for (int j = 0; j < k; j++) {
                Neighbor nj = neighbors.get(j);
                double d = distance(ni.normRow, ni.normCol, ni.normTime, nj.normRow, nj.normCol, nj.normTime);
                gamma[i][j] = variogram.semivariance(d);
            }
            gamma[i][k] = 1.0;
            gamma[k][i] = 1.0;
            rhs[i] = variogram.semivariance(ni.distance);
        }
        gamma[k][k] = 0.0;
        rhs[k] = 1.0;

        double[] weights = solveLinearSystem(gamma, rhs);
        if (weights == null) {
            return inverseDistanceWeight(neighbors);
        }

        double predicted = 0.0;
        for (int i = 0; i < k; i++) {
            predicted += weights[i] * neighbors.get(i).value;
        }
        return predicted;
    }

    private static double inverseDistanceWeight(List<Neighbor> neighbors) {
        double weightSum = 0.0;
        double valueSum = 0.0;
        for (Neighbor n : neighbors) {
            double w = 1.0 / (n.distance * n.distance + 1e-6);
            weightSum += w;
            valueSum += w * n.value;
        }
        return weightSum > 0 ? valueSum / weightSum : neighbors.get(0).value;
    }

    /**
     * Solves the linear system Ax=b via Gaussian elimination with partial pivoting.
     * Returns null if the system is (numerically) singular.
     */
    private static double[] solveLinearSystem(double[][] a, double[] b) {
        int n = b.length;
        double[][] m = new double[n][n + 1];
        for (int i = 0; i < n; i++) {
            System.arraycopy(a[i], 0, m[i], 0, n);
            m[i][n] = b[i];
        }

        for (int col = 0; col < n; col++) {
            int pivotRow = col;
            double maxAbs = Math.abs(m[col][col]);
            for (int r = col + 1; r < n; r++) {
                if (Math.abs(m[r][col]) > maxAbs) {
                    maxAbs = Math.abs(m[r][col]);
                    pivotRow = r;
                }
            }
            if (maxAbs < 1e-10) return null;

            if (pivotRow != col) {
                double[] tmp = m[col];
                m[col] = m[pivotRow];
                m[pivotRow] = tmp;
            }

            for (int r = col + 1; r < n; r++) {
                double factor = m[r][col] / m[col][col];
                for (int c = col; c <= n; c++) {
                    m[r][c] -= factor * m[col][c];
                }
            }
        }

        double[] x = new double[n];
        for (int r = n - 1; r >= 0; r--) {
            double sum = m[r][n];
            for (int c = r + 1; c < n; c++) {
                sum -= m[r][c] * x[c];
            }
            x[r] = sum / m[r][r];
        }
        return x;
    }

    /**
     * Fits a spherical variogram model (nugget, sill, range) from an empirical
     * variogram built from valid cells in the current grid and its temporal
     * neighbors. Returns null if the sampled values have effectively no variance
     * (a near-constant field, for which kriging is unnecessary).
     */
    private static Variogram fitVariogram(List<int[]> validCells, float[][] temporalData, double[] temporalOffsets,
                                           int nx, int ny, float noDataValue, int gridIndex) {
        List<int[]> sampleCells = validCells;
        if (validCells.size() > MAX_VARIOGRAM_SAMPLES) {
            sampleCells = new ArrayList<>(validCells);
            Collections.shuffle(sampleCells, new Random(gridIndex));
            sampleCells = sampleCells.subList(0, MAX_VARIOGRAM_SAMPLES);
        }

        List<double[]> samplePoints = new ArrayList<>();
        for (int[] cell : sampleCells) {
            int row = cell[0];
            int col = cell[1];
            double normRow = normCoord(row, ny);
            double normCol = normCoord(col, nx);

            for (int t = 0; t < temporalData.length; t++) {
                float[] tData = temporalData[t];
                if (tData == null) continue;
                float value = tData[row * nx + col];
                if (Float.compare(value, noDataValue) == 0) continue;
                samplePoints.add(new double[]{normRow, normCol, normTime(temporalOffsets[t]), value});
            }
        }

        int n = samplePoints.size();
        if (n < 2) return null;

        double mean = 0.0;
        for (double[] p : samplePoints) mean += p[3];
        mean /= n;

        double variance = 0.0;
        for (double[] p : samplePoints) {
            double diff = p[3] - mean;
            variance += diff * diff;
        }
        variance /= n;

        if (variance < 1e-9) return null;

        List<double[]> pairs = new ArrayList<>();
        if ((long) n * (n - 1) / 2 <= MAX_VARIOGRAM_PAIRS) {
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    pairs.add(pairLagAndSemivariance(samplePoints.get(i), samplePoints.get(j)));
                }
            }
        } else {
            Random random = new Random(gridIndex);
            for (int p = 0; p < MAX_VARIOGRAM_PAIRS; p++) {
                int i = random.nextInt(n);
                int j = random.nextInt(n);
                if (i == j) continue;
                pairs.add(pairLagAndSemivariance(samplePoints.get(i), samplePoints.get(j)));
            }
        }

        if (pairs.isEmpty()) return null;

        double maxLag = 0.0;
        for (double[] pair : pairs) maxLag = Math.max(maxLag, pair[0]);
        if (maxLag < 1e-9) return null;

        double[] binGammaSum = new double[NUM_LAG_BINS];
        int[] binCount = new int[NUM_LAG_BINS];
        double[] binLagSum = new double[NUM_LAG_BINS];
        double binWidth = maxLag / NUM_LAG_BINS;

        for (double[] pair : pairs) {
            int bin = Math.min(NUM_LAG_BINS - 1, (int) (pair[0] / binWidth));
            binLagSum[bin] += pair[0];
            binGammaSum[bin] += pair[1];
            binCount[bin]++;
        }

        double nugget = 0.0;
        double range = maxLag;
        boolean nuggetSet = false;
        boolean rangeSet = false;

        for (int bin = 0; bin < NUM_LAG_BINS; bin++) {
            if (binCount[bin] == 0) continue;
            double avgLag = binLagSum[bin] / binCount[bin];
            double avgGamma = binGammaSum[bin] / binCount[bin];

            if (!nuggetSet) {
                nugget = Math.min(avgGamma, variance * 0.9);
                nuggetSet = true;
            }
            if (!rangeSet && avgGamma >= 0.95 * variance) {
                range = avgLag;
                rangeSet = true;
            }
        }

        nugget = Math.max(0.0, nugget);
        range = Math.max(range, 1e-3);

        return new Variogram(nugget, variance, range);
    }

    private static double[] pairLagAndSemivariance(double[] a, double[] b) {
        double d = distance(a[0], a[1], a[2], b[0], b[1], b[2]);
        double semivariance = 0.5 * (a[3] - b[3]) * (a[3] - b[3]);
        return new double[]{d, semivariance};
    }

    private static double distance(double r1, double c1, double t1, double r2, double c2, double t2) {
        double dr = r1 - r2;
        double dc = c1 - c2;
        double dt = TIME_WEIGHT * (t1 - t2);
        return Math.sqrt(dr * dr + dc * dc + dt * dt);
    }

    private static double normCoord(int idx, int size) {
        return size > 1 ? (double) idx / (size - 1) : 0.5;
    }

    private static double normTime(double offset) {
        return offset / 2.0;
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
            String message = Message.format("kriging_filler_error_copy", source, destination);
            LOGGER.log(Level.SEVERE, message, e);
            support.firePropertyChange(VortexProperty.ERROR.toString(), null, message);
        }
    }

    /** A candidate point used as a kriging predictor, with its distance to the target gap cell. */
    private static final class Neighbor {
        final double normRow;
        final double normCol;
        final double normTime;
        final float value;
        final double distance;

        Neighbor(double normRow, double normCol, double normTime, float value, double distance) {
            this.normRow = normRow;
            this.normCol = normCol;
            this.normTime = normTime;
            this.value = value;
            this.distance = distance;
        }
    }

    /** A spherical variogram model: gamma(h) = nugget + (sill-nugget) * sphericalRamp(h/range). */
    private static final class Variogram {
        final double nugget;
        final double sill;
        final double range;

        Variogram(double nugget, double sill, double range) {
            this.nugget = nugget;
            this.sill = sill;
            this.range = range;
        }

        double semivariance(double h) {
            if (h <= 1e-9) return 0.0;
            if (h >= range) return sill;
            double ratio = h / range;
            return nugget + (sill - nugget) * (1.5 * ratio - 0.5 * ratio * ratio * ratio);
        }
    }
}
