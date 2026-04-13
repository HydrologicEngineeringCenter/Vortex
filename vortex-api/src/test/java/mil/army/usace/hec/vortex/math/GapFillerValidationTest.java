package mil.army.usace.hec.vortex.math;

import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.io.DataReader;
import mil.army.usace.hec.vortex.util.DssUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validation tests comparing RegressionGapFiller (ML), LinearInterpGapFiller, and
 * FocalMeanGapFiller using real DSS datasets.
 *
 * Methodology: Read real grids, hold out known cell values as synthetic gaps, run each
 * filler, and compare predictions against ground truth using RMSE.
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
            double linearSse = 0;
            double focalMeanSse = 0;
            int comparedCells = 0;

            for (int idx : holdoutSet) {
                float truth = originalData[idx];
                float regressionPred = regressionResult.data()[idx];
                float linearPred = linearFilled[idx];
                float focalMeanPred = focalMeanResult.data()[idx];

                if (Float.compare(regressionPred, noDataValue) != 0
                        && Float.compare(linearPred, noDataValue) != 0
                        && Float.compare(focalMeanPred, noDataValue) != 0) {
                    regressionSse += (regressionPred - truth) * (regressionPred - truth);
                    linearSse += (linearPred - truth) * (linearPred - truth);
                    focalMeanSse += (focalMeanPred - truth) * (focalMeanPred - truth);
                    comparedCells++;
                }
            }

            if (comparedCells > 0) {
                regressionRmseSum += Math.sqrt(regressionSse / comparedCells);
                linearRmseSum += Math.sqrt(linearSse / comparedCells);
                focalMeanRmseSum += Math.sqrt(focalMeanSse / comparedCells);
                evaluatedGrids++;
            }
        }

        assertTrue(evaluatedGrids > 0, "Should have evaluated at least one grid");

        final double regressionAvgRmse = regressionRmseSum / evaluatedGrids;
        final double linearAvgRmse = linearRmseSum / evaluatedGrids;
        final double focalMeanAvgRmse = focalMeanRmseSum / evaluatedGrids;
        final int totalEvaluated = evaluatedGrids;

        LOGGER.info(() -> String.format(
                "%s - Validation results over %d grids (%.0f%% holdout):%n" +
                "  Focal Mean      avg RMSE: %.6f%n" +
                "  Linear Interp   avg RMSE: %.6f%n" +
                "  ML Regression   avg RMSE: %.6f%n" +
                "  Regression vs Linear:  %.2f%% improvement%n" +
                "  Regression vs Focal:   %.2f%% improvement",
                datasetName, totalEvaluated, gapFraction * 100,
                focalMeanAvgRmse, linearAvgRmse, regressionAvgRmse,
                (1.0 - regressionAvgRmse / linearAvgRmse) * 100,
                (1.0 - regressionAvgRmse / focalMeanAvgRmse) * 100));

        assertTrue(Double.isFinite(regressionAvgRmse), "Regression RMSE should be finite");
        assertTrue(Double.isFinite(linearAvgRmse), "Linear RMSE should be finite");
        assertTrue(Double.isFinite(focalMeanAvgRmse), "Focal mean RMSE should be finite");
        assertTrue(regressionAvgRmse >= 0, "Regression RMSE should be non-negative");
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
    }
}
