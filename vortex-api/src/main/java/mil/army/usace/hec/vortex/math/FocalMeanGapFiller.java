package mil.army.usace.hec.vortex.math;

import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.geo.RasterUtils;

import java.util.ArrayList;
import java.util.List;

class FocalMeanGapFiller implements GapFiller {

    private FocalMeanGapFiller() {
    }

    static FocalMeanGapFiller newInstance() {
        return new FocalMeanGapFiller();
    }

    private record FillValue(int index, float value) {
    }

    @Override
    public VortexGrid fill(VortexGrid grid) {
        int nx = grid.nx();
        int ny = grid.ny();

        float noDataValue = (float) grid.noDataValue();
        float[] data = grid.data();
        float[][] data2d = RasterUtils.convert1DTo2D(data, nx, ny);

        List<FillValue> fillValues = new ArrayList<>();

        for (int i = 0; i < data.length; i++) {
            float value = data[i];
            if (Double.compare(noDataValue, value) == 0) {
                int row = i / nx;
                int col = i % nx;
                int maxDim = Math.max(nx, ny);
                int kernalSize = 3;
                float focalMean = noDataValue;
                while (Float.compare(noDataValue, focalMean) == 0 && kernalSize <= maxDim) {
                    int offset = kernalSize / 2;
                    focalMean = computeMean(data2d, row, col, offset, noDataValue);
                    kernalSize += 2;
                }
                fillValues.add(new FillValue(i, focalMean));
            }
        }

        for (FillValue fillValue : fillValues) {
            int i = fillValue.index;
            float value = fillValue.value;
            data[i] = value;
        }

        return VortexGrid.toBuilder(grid)
                .data(data)
                .build();
    }

    private static float computeMean(float[][] grid, int x, int y, int offset, float noDataValue) {
        int rows = grid.length;
        int cols = grid[0].length;

        float sum = 0;
        int count = 0;

        for (int i = x - offset; i <= x + offset; i++) {
            for (int j = y - offset; j <= y + offset; j++) {
                float value = getValue(grid, i, j, rows, cols, noDataValue);
                if (Float.compare(noDataValue, value) == 0)
                    continue;

                sum += value;
                count++;
            }
        }

        if (count == 0)
            return noDataValue;

        return sum / count;
    }


    private static float getValue(float[][] grid, int i, int j, int rows, int cols, float noDataValue) {
        if (i < 0 || i >= rows || j < 0 || j >= cols) {
            return noDataValue;
        }
        return grid[i][j];
    }
}
