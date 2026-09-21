package mil.army.usace.hec.vortex.math;

import hec.heclib.dss.HecDataManager;
import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.io.DataReader;
import mil.army.usace.hec.vortex.io.DataWriter;
import mil.army.usace.hec.vortex.util.DssUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validation tests comparing RegressionGapFiller (ML), KrigingGapFiller, LinearInterpGapFiller, and
 * FocalMeanGapFiller using real DSS datasets.
 *
 * Methodology: Read real grids, hold out known cell values as synthetic gaps, run each
 * filler, and compare predictions against ground truth using RMSE.
 * Timing tests compare actual batch implementations, including DSS I/O,
 * after warm-up; timings are reported without machine-dependent thresholds.
 */
class GapFillerValidationTest {

    private static final Logger LOGGER = Logger.getLogger(GapFillerValidationTest.class.getName());

    private static List<VortexGrid> loadGrids(String resourcePath) {
        String pathToDss = new File(GapFillerValidationTest.class.getResource(resourcePath).getFile()).toString();

        Set<String> rawVariables = DataReader.getVariables(pathToDss);
        assertFalse(rawVariables.isEmpty(), "Dataset should have at least one variable");

        Set<String> condensed = DssUtil.condenseVariables(pathToDss, rawVariables);
        String variable = condensed.iterator().next();
        LOGGER.info(() -> "Loading " + resourcePath + ", variable: " + variable);

        List<VortexGrid> grids = new ArrayList<>();
        try (DataReader reader = DataReader.builder()
                .path(pathToDss)
                .variable(variable)
                .build()) {

            int count = reader.getDtoCount();
            assertTrue(count >= 3, "Dataset should have at least 3 timesteps, found: " + count);

            for (int i = 0; i < count; i++) {
                grids.add((VortexGrid) reader.getDto(i));
            }
        } catch (Exception e) {
            fail("Failed to load dataset: " + e.getMessage());
        }

        LOGGER.info(() -> String.format("Loaded %d grids, nx=%d, ny=%d",
                grids.size(), grids.getFirst().nx(), grids.getFirst().ny()));
        return grids;
    }

    private void runComparison(List<VortexGrid> grids, String datasetName, double gapFraction) {
        long seed = 42L;
        FocalMeanGapFiller focalMeanFiller = FocalMeanGapFiller.newInstance();

        double regressionRmseSum = 0;
        double krigingRmseSum = 0;
        double linearRmseSum = 0;
        double focalMeanRmseSum = 0;
        int evaluatedGrids = 0;

        for (int i = 1; i < grids.size() - 1; i++) {
            VortexGrid grid = grids.get(i);
            float[] originalData = grid.data();
            float noDataValue = (float) grid.noDataValue();

            List<Integer> validIndices = new ArrayList<>();
            for (int j = 0; j < originalData.length; j++) {
                if (Float.compare(originalData[j], noDataValue) != 0) {
                    validIndices.add(j);
                }
            }

            if (validIndices.size() < 20) continue;

            Random rng = new Random(seed + i);
            Collections.shuffle(validIndices, rng);
            int holdoutCount = Math.max(1, (int) (validIndices.size() * gapFraction));
            Set<Integer> holdoutSet = new HashSet<>(validIndices.subList(0, holdoutCount));

            float[] gappedData = originalData.clone();
            for (int idx : holdoutSet) {
                gappedData[idx] = noDataValue;
            }
            VortexGrid gappedGrid = VortexGrid.toBuilder(grid).data(gappedData).build();

            // --- Regression filler ---
            VortexGrid prevGrid = grids.get(i - 1);
            VortexGrid nextGrid = grids.get(i + 1);
            VortexGrid prev2Grid = i >= 2 ? grids.get(i - 2) : null;
            VortexGrid next2Grid = i + 2 < grids.size() ? grids.get(i + 2) : null;

            BatchGapFiller.Builder regressionBuilder = BatchGapFiller.builder()
                    .source("dummy.dss")
                    .destination("dummy-out.dss")
                    .variables(List.of("test"))
                    .method(GapFillMethod.REGRESSION);
            RegressionGapFiller regressionFiller = new RegressionGapFiller(regressionBuilder);

            VortexGrid regressionResult = regressionFiller.fillGridGaps(
                    gappedGrid, i, grids.size(), prevGrid, nextGrid, prev2Grid, next2Grid);

            // --- Kriging filler ---
            BatchGapFiller.Builder krigingBuilder = BatchGapFiller.builder()
                    .source("dummy.dss")
                    .destination("dummy-out.dss")
                    .variables(List.of("test"))
                    .method(GapFillMethod.KRIGING);
            KrigingGapFiller krigingFiller = new KrigingGapFiller(krigingBuilder);

            VortexGrid krigingResult = krigingFiller.fillGridGaps(
                    gappedGrid, i, grids.size(), prevGrid, nextGrid, prev2Grid, next2Grid);

            // --- Linear interpolation ---
            float[] linearFilled = gappedData.clone();
            float[] prevData = prevGrid.data();
            float[] nextData = nextGrid.data();
            long prevEpoch = prevGrid.startTime().toEpochSecond();
            long currEpoch = grid.startTime().toEpochSecond();
            long nextEpoch = nextGrid.startTime().toEpochSecond();

            for (int idx : holdoutSet) {
                float prevVal = prevData[idx];
                float nextVal = nextData[idx];
                if (Float.compare(prevVal, noDataValue) != 0 && Float.compare(nextVal, noDataValue) != 0) {
                    linearFilled[idx] = prevVal + ((currEpoch - prevEpoch) * (nextVal - prevVal))
                            / (float) (nextEpoch - prevEpoch);
                }
            }

            // --- Focal mean filler ---
            VortexGrid focalMeanResult = focalMeanFiller.fill(gappedGrid);

            // --- Compute RMSE ---
            double regressionSse = 0;
            double krigingSse = 0;
            double linearSse = 0;
            double focalMeanSse = 0;
            int comparedCells = 0;

            for (int idx : holdoutSet) {
                float truth = originalData[idx];
                float regressionPred = regressionResult.data()[idx];
                float krigingPred = krigingResult.data()[idx];
                float linearPred = linearFilled[idx];
                float focalMeanPred = focalMeanResult.data()[idx];

                if (Float.compare(regressionPred, noDataValue) != 0
                        && Float.compare(krigingPred, noDataValue) != 0
                        && Float.compare(linearPred, noDataValue) != 0
                        && Float.compare(focalMeanPred, noDataValue) != 0) {
                    regressionSse += (regressionPred - truth) * (regressionPred - truth);
                    krigingSse += (krigingPred - truth) * (krigingPred - truth);
                    linearSse += (linearPred - truth) * (linearPred - truth);
                    focalMeanSse += (focalMeanPred - truth) * (focalMeanPred - truth);
                    comparedCells++;
                }
            }

            if (comparedCells > 0) {
                regressionRmseSum += Math.sqrt(regressionSse / comparedCells);
                krigingRmseSum += Math.sqrt(krigingSse / comparedCells);
                linearRmseSum += Math.sqrt(linearSse / comparedCells);
                focalMeanRmseSum += Math.sqrt(focalMeanSse / comparedCells);
                evaluatedGrids++;
            }
        }

        assertTrue(evaluatedGrids > 0, "Should have evaluated at least one grid");

        final double regressionAvgRmse = regressionRmseSum / evaluatedGrids;
        final double krigingAvgRmse = krigingRmseSum / evaluatedGrids;
        final double linearAvgRmse = linearRmseSum / evaluatedGrids;
        final double focalMeanAvgRmse = focalMeanRmseSum / evaluatedGrids;
        final int totalEvaluated = evaluatedGrids;

        LOGGER.info(() -> String.format(
                "%s - Validation results over %d grids (%.0f%% holdout):%n" +
                "  Focal Mean      avg RMSE: %.6f%n" +
                "  Linear Interp   avg RMSE: %.6f%n" +
                "  ML Regression   avg RMSE: %.6f%n" +
                "  Kriging         avg RMSE: %.6f%n" +
                "  Regression vs Linear:  %.2f%% improvement%n" +
                "  Regression vs Focal:   %.2f%% improvement%n" +
                "  Kriging vs Linear:     %.2f%% improvement%n" +
                "  Kriging vs Focal:      %.2f%% improvement",
                datasetName, totalEvaluated, gapFraction * 100,
                focalMeanAvgRmse, linearAvgRmse, regressionAvgRmse, krigingAvgRmse,
                (1.0 - regressionAvgRmse / linearAvgRmse) * 100,
                (1.0 - regressionAvgRmse / focalMeanAvgRmse) * 100,
                (1.0 - krigingAvgRmse / linearAvgRmse) * 100,
                (1.0 - krigingAvgRmse / focalMeanAvgRmse) * 100));

        assertTrue(Double.isFinite(regressionAvgRmse), "Regression RMSE should be finite");
        assertTrue(Double.isFinite(krigingAvgRmse), "Kriging RMSE should be finite");
        assertTrue(Double.isFinite(linearAvgRmse), "Linear RMSE should be finite");
        assertTrue(Double.isFinite(focalMeanAvgRmse), "Focal mean RMSE should be finite");
        assertTrue(regressionAvgRmse >= 0, "Regression RMSE should be non-negative");
        assertTrue(krigingAvgRmse >= 0, "Kriging RMSE should be non-negative");
        assertTrue(linearAvgRmse >= 0, "Linear RMSE should be non-negative");
        assertTrue(focalMeanAvgRmse >= 0, "Focal mean RMSE should be non-negative");
    }

    @Test
    @DisplayName("QPE precipitation: compare gap fill methods")
    void compareRmsePrecipitation() {
        List<VortexGrid> grids = loadGrids("/truckee/truckee_river_qpe.dss");
        runComparison(grids, "Precipitation (QPE)", 0.10);
    }

    @Test
    @DisplayName("Temperature: compare gap fill methods")
    void compareRmseTemperature() {
        List<VortexGrid> grids = loadGrids("/truckee/truckee_temperature.dss");
        runComparison(grids, "Temperature", 0.10);
    }

    @Test
    @DisplayName("QPE precipitation: compare gap fill processing time")
    void compareProcessingTimePrecipitation(@TempDir Path directory) throws Exception {
        try {
            compareProcessingTime(loadGrids("/truckee/truckee_river_qpe.dss"),
                    "Precipitation (QPE)", directory);
        } finally {
            // DSS caches native file handles beyond DataReader.close().
            HecDataManager.closeAllFiles();
        }
    }

    @Test
    @DisplayName("Temperature: compare gap fill processing time")
    void compareProcessingTimeTemperature(@TempDir Path directory) throws Exception {
        try {
            compareProcessingTime(loadGrids("/truckee/truckee_temperature.dss"),
                    "Temperature", directory);
        } finally {
            HecDataManager.closeAllFiles();
        }
    }

    private void compareProcessingTime(List<VortexGrid> grids, String datasetName,
                                       Path directory) throws Exception {
        // Prepare one immutable input for every method outside the timed region.
        List<VortexData> inputGrids = new ArrayList<>(grids);
        int gapGrids = 0;
        int gapCells = 0;
        for (int i = 1; i < grids.size() - 1; i++) {
            VortexGrid grid = grids.get(i);
            float noDataValue = (float) grid.noDataValue();
            List<Integer> validIndices = new ArrayList<>();
            for (int j = 0; j < grid.data().length; j++) {
                if (Float.compare(grid.data()[j], noDataValue) != 0) {
                    validIndices.add(j);
                }
            }
            if (validIndices.size() < 20) continue;

            Collections.shuffle(validIndices, new Random(42L + i));
            int holdoutCount = Math.max(1, (int) (validIndices.size() * 0.10));
            float[] gappedData = grid.data().clone();
            for (int j = 0; j < holdoutCount; j++) {
                gappedData[validIndices.get(j)] = noDataValue;
            }
            inputGrids.set(i, VortexGrid.toBuilder(grid).data(gappedData).build());
            gapGrids++;
            gapCells += holdoutCount;
        }
        assertTrue(gapGrids > 0, "Timing comparison needs grids with synthetic gaps");

        Path source = directory.resolve("input.dss");
        DataWriter.builder().data(inputGrids).destination(source).build().write();
        Set<String> variables = DssUtil.condenseVariables(source.toString(),
                DataReader.getVariables(source.toString()));
        assertEquals(1, variables.size(), "Timing input should contain one variable");
        String variable = variables.iterator().next();

        List<GapFillMethod> methods = List.of(GapFillMethod.FOCAL_MEAN,
                GapFillMethod.LINEAR_INTERPOLATION, GapFillMethod.REGRESSION, GapFillMethod.KRIGING);
        int measuredRuns = 3;
        Map<GapFillMethod, long[]> timings = new EnumMap<>(GapFillMethod.class);
        for (GapFillMethod method : methods) {
            timings.put(method, new long[measuredRuns]);
        }

        // One unmeasured pass warms each implementation. Rotate the order between
        // measured passes and use fresh destinations so outputs cannot be reused.
        for (int run = -1; run < measuredRuns; run++) {
            for (int offset = 0; offset < methods.size(); offset++) {
                GapFillMethod method = methods.get((offset + Math.max(run, 0)) % methods.size());
                Path destination = directory.resolve(method + "-" + run + ".dss");
                BatchGapFiller filler = BatchGapFiller.builder()
                        .source(source.toString())
                        .destination(destination.toString())
                        .variables(List.of(variable))
                        .method(method)
                        .build();

                long start = System.nanoTime();
                // Call directly so processing exceptions fail the test rather than
                // being caught and logged by BatchGapFiller.run().
                int processed = filler.processVariable(variable);
                long elapsed = System.nanoTime() - start;

                assertTrue(processed >= gapGrids, method + " should process the gapped grids");
                assertTrue(elapsed > 0, method + " should have a positive elapsed time");
                try (DataReader reader = DataReader.builder()
                        .path(destination.toString()).variable(variable).build()) {
                    assertEquals(grids.size(), reader.getDtoCount(),
                            method + " should write the complete time series");
                }
                if (run >= 0) timings.get(method)[run] = elapsed;
            }
        }

        StringBuilder report = new StringBuilder(String.format(Locale.ROOT,
                "%s - Processing time over %d grids (%d with synthetic gaps, %d held-out cells, 10%% holdout):%n"
                        + "  Median of %d runs after one warm-up; includes DSS reads/writes.%n"
                        + "  Excludes input preparation, filler construction, and output verification.%n",
                datasetName, grids.size(), gapGrids, gapCells, measuredRuns));
        for (GapFillMethod method : methods) {
            long[] samples = timings.get(method);
            Arrays.sort(samples);
            double medianMs = samples[measuredRuns / 2] / 1_000_000.0;
            report.append(String.format(Locale.ROOT, "  %-20s %10.3f ms total, %8.3f ms/grid%n",
                    method, medianMs, medianMs / grids.size()));
        }
        LOGGER.info(report.toString());
    }

    @Test
    @DisplayName("All fillers should fill all synthetic gaps in real QPE data")
    void allFillersShouldFillAllGaps() {
        List<VortexGrid> grids = loadGrids("/truckee/truckee_river_qpe.dss");

        VortexGrid grid = grids.get(grids.size() / 2);
        float[] originalData = grid.data();
        float noDataValue = (float) grid.noDataValue();

        List<Integer> validIndices = new ArrayList<>();
        for (int j = 0; j < originalData.length; j++) {
            if (Float.compare(originalData[j], noDataValue) != 0) {
                validIndices.add(j);
            }
        }

        assertTrue(validIndices.size() >= 20, "Grid should have enough valid cells");

        Random rng = new Random(123L);
        Collections.shuffle(validIndices, rng);
        int holdoutCount = Math.max(1, (int) (validIndices.size() * 0.05));
        Set<Integer> holdoutSet = new HashSet<>(validIndices.subList(0, holdoutCount));

        float[] gappedData = originalData.clone();
        for (int idx : holdoutSet) {
            gappedData[idx] = noDataValue;
        }
        VortexGrid gappedGrid = VortexGrid.toBuilder(grid).data(gappedData).build();

        int midIndex = grids.size() / 2;
        VortexGrid prevGrid = grids.get(midIndex - 1);
        VortexGrid nextGrid = grids.get(midIndex + 1);
        VortexGrid prev2Grid = midIndex >= 2 ? grids.get(midIndex - 2) : null;
        VortexGrid next2Grid = midIndex + 2 < grids.size() ? grids.get(midIndex + 2) : null;

        BatchGapFiller.Builder builder = BatchGapFiller.builder()
                .source("dummy.dss")
                .destination("dummy-out.dss")
                .variables(List.of("test"))
                .method(GapFillMethod.REGRESSION);
        RegressionGapFiller regressionFiller = new RegressionGapFiller(builder);

        VortexGrid regressionResult = regressionFiller.fillGridGaps(
                gappedGrid, midIndex, grids.size(), prevGrid, nextGrid, prev2Grid, next2Grid);

        int unfilledRegression = 0;
        for (int idx : holdoutSet) {
            if (Float.compare(regressionResult.data()[idx], noDataValue) == 0) {
                unfilledRegression++;
            }
        }

        assertEquals(0, unfilledRegression,
                "Regression filler should fill all synthetic gaps, but left " + unfilledRegression + " unfilled");

        LOGGER.info(() -> String.format("Regression filler filled all %d synthetic gaps", holdoutCount));

        BatchGapFiller.Builder krigingBuilder = BatchGapFiller.builder()
                .source("dummy.dss")
                .destination("dummy-out.dss")
                .variables(List.of("test"))
                .method(GapFillMethod.KRIGING);
        KrigingGapFiller krigingFiller = new KrigingGapFiller(krigingBuilder);

        VortexGrid krigingResult = krigingFiller.fillGridGaps(
                gappedGrid, midIndex, grids.size(), prevGrid, nextGrid, prev2Grid, next2Grid);

        int unfilledKriging = 0;
        for (int idx : holdoutSet) {
            if (Float.compare(krigingResult.data()[idx], noDataValue) == 0) {
                unfilledKriging++;
            }
        }

        assertEquals(0, unfilledKriging,
                "Kriging filler should fill all synthetic gaps, but left " + unfilledKriging + " unfilled");

        LOGGER.info(() -> String.format("Kriging filler filled all %d synthetic gaps", holdoutCount));
    }
}
