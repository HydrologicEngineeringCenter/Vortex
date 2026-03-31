package mil.army.usace.hec.vortex.math;

import mil.army.usace.hec.vortex.VortexGrid;

/**
 * Fills gaps in a grid using an adaptive focal mean approach.
 * The algorithm uses progressively larger kernels until valid data is found.
 */
class FocalMeanGapFiller implements GapFiller {

    private final int initialKernelSize;
    private final int maxKernelSize;
    private final int kernelSizeIncrement;

    private FocalMeanGapFiller(int initialKernelSize, int maxKernelSize, int kernelSizeIncrement) {
        this.initialKernelSize = initialKernelSize;
        this.maxKernelSize = maxKernelSize;
        this.kernelSizeIncrement = kernelSizeIncrement;
    }

    static FocalMeanGapFiller newInstance() {
        return new FocalMeanGapFiller(3, Integer.MAX_VALUE, 2);
    }

    static FocalMeanGapFiller newInstance(int initialKernelSize, int maxKernelSize, int kernelSizeIncrement) {
        if (initialKernelSize % 2 == 0 || maxKernelSize % 2 == 0 || kernelSizeIncrement % 2 != 0) {
            throw new IllegalArgumentException("Invalid kernel parameters");
        }
        return new FocalMeanGapFiller(initialKernelSize, maxKernelSize, kernelSizeIncrement);
    }

    @Override
    public VortexGrid fill(VortexGrid grid) {
        if (grid == null) {
            throw new IllegalArgumentException("Grid cannot be null");
        }

        int nx = grid.nx();
        int ny = grid.ny();
        float noDataValue = (float) grid.noDataValue();
        float[] original = grid.data();
        float[] filled = null;
        int effectiveMaxKernel = Math.min(maxKernelSize, Math.max(nx, ny));

        for (int i = 0; i < original.length; i++) {
            if (Float.compare(original[i], noDataValue) != 0) continue;

            int row = i / nx;
            int col = i % nx;
            float mean = computeMean(original, nx, ny, row, col, noDataValue, effectiveMaxKernel);

            if (Float.compare(mean, noDataValue) != 0) {
                if (filled == null) filled = original.clone();
                filled[i] = mean;
            }
        }

        if (filled == null) return grid;
        return VortexGrid.toBuilder(grid).data(filled).build();
    }

    private float computeMean(float[] data, int nx, int ny,
                              int centerRow, int centerCol,
                              float noDataValue, int effectiveMaxKernel) {
        for (int kernelSize = initialKernelSize; kernelSize <= effectiveMaxKernel; kernelSize += kernelSizeIncrement) {
            int radius = kernelSize / 2;

            int rowStart = Math.max(0, centerRow - radius);
            int rowEnd = Math.min(ny - 1, centerRow + radius);
            int colStart = Math.max(0, centerCol - radius);
            int colEnd = Math.min(nx - 1, centerCol + radius);

            float sum = 0;
            int count = 0;

            for (int r = rowStart; r <= rowEnd; r++) {
                int rowOffset = r * nx;
                for (int c = colStart; c <= colEnd; c++) {
                    float value = data[rowOffset + c];
                    if (Float.compare(value, noDataValue) != 0) {
                        sum += value;
                        count++;
                    }
                }
            }

            if (count > 0) {
                return sum / count;
            }
        }

        return noDataValue;
    }
}
