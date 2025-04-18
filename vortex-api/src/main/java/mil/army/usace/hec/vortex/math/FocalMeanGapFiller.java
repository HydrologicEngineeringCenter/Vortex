package mil.army.usace.hec.vortex.math;

import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.geo.RasterUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Fills gaps in a grid using an adaptive focal mean approach.
 * The algorithm uses progressively larger kernels until valid data is found.
 */
class FocalMeanGapFiller implements GapFiller {

    private final int maxKernelSize;
    private final int initialKernelSize;
    private final int kernelSizeIncrement;

    private FocalMeanGapFiller(int initialKernelSize, int maxKernelSize, int kernelSizeIncrement) {
        this.initialKernelSize = initialKernelSize;
        this.maxKernelSize = maxKernelSize;
        this.kernelSizeIncrement = kernelSizeIncrement;
    }

    /**
     * Creates a new gap filler with default parameters.
     */
    static FocalMeanGapFiller newInstance() {
        return new FocalMeanGapFiller(3, Integer.MAX_VALUE, 2);
    }

    /**
     * Creates a new gap filler with custom parameters.
     */
    static FocalMeanGapFiller newInstance(int initialKernelSize, int maxKernelSize, int kernelSizeIncrement) {
        if (initialKernelSize % 2 == 0 || maxKernelSize % 2 == 0 || kernelSizeIncrement % 2 != 0) {
            throw new IllegalArgumentException("Invalid kernel parameters");
        }
        return new FocalMeanGapFiller(initialKernelSize, maxKernelSize, kernelSizeIncrement);
    }

    /**
     * Represents a value to fill a gap at a specific index.
     */
    private record FillValue(int index, float value) {
    }

    @Override
    public VortexGrid fill(VortexGrid grid) {
        if (grid == null) {
            throw new IllegalArgumentException("Grid cannot be null");
        }

        int nx = grid.nx();
        int ny = grid.ny();
        float noDataValue = (float) grid.noDataValue();
        float[] data = grid.data().clone();

        int effectiveMaxKernelSize = Math.min(maxKernelSize, Math.max(nx, ny));
        float[][] data2d = RasterUtils.convert1DTo2D(data, nx, ny);
        List<FillValue> fillValues = new ArrayList<>();

        for (int i = 0; i < data.length; i++) {
            if (Float.compare(noDataValue, data[i]) == 0) {
                int row = i / nx;
                int col = i % nx;

                int kernelSize = initialKernelSize;
                float focalMean = noDataValue;

                while (Float.compare(noDataValue, focalMean) == 0 && kernelSize <= effectiveMaxKernelSize) {
                    int kernelRadius = kernelSize / 2;
                    focalMean = computeMean(data2d, row, col, kernelRadius, noDataValue);
                    kernelSize += kernelSizeIncrement;
                }

                // Only add if we found a valid value
                if (Float.compare(noDataValue, focalMean) != 0) {
                    fillValues.add(new FillValue(i, focalMean));
                }
            }
        }

        for (FillValue fillValue : fillValues) {
            data[fillValue.index()] = fillValue.value();
        }

        return VortexGrid.toBuilder(grid)
                .data(data)
                .build();
    }

    /**
     * Computes the mean of valid values in a kernel around the specified cell.
     */
    private static float computeMean(float[][] grid, int centerRow, int centerCol, int kernelRadius, float noDataValue) {
        int rows = grid.length;
        int cols = grid[0].length;

        float sum = 0;
        int count = 0;

        int rowStart = Math.max(0, centerRow - kernelRadius);
        int rowEnd = Math.min(rows - 1, centerRow + kernelRadius);
        int colStart = Math.max(0, centerCol - kernelRadius);
        int colEnd = Math.min(cols - 1, centerCol + kernelRadius);

        for (int i = rowStart; i <= rowEnd; i++) {
            for (int j = colStart; j <= colEnd; j++) {
                float value = grid[i][j];
                if (Float.compare(noDataValue, value) != 0) {
                    sum += value;
                    count++;
                }
            }
        }

        return (count > 0) ? (sum / count) : noDataValue;
    }
}