package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.geo.RasterUtils;
import mil.army.usace.hec.vortex.util.ImageUtils;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.TranslateOptions;
import org.gdal.gdal.gdal;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;

class AscDataWriter extends DataWriter {
    private static final double NO_DATA_VALUE = -9999;

    AscDataWriter(Builder builder) {
        super(builder);
    }

    @Override
    public void write() {
        List<VortexGrid> grids = data.stream()
                .filter(vortexData -> vortexData instanceof VortexGrid)
                .map(vortexData -> (VortexGrid) vortexData)
                .collect(Collectors.toList());

        grids.forEach(grid -> {
            boolean isExpandFileName = Boolean.parseBoolean(options.getOrDefault("expandName", "false"));

            String pathToDestination = isExpandFileName ?
                        ImageUtils.expandFileName(destination.toString(), grid, ImageFileType.ASC) : destination.toString();

            VortexGrid gridOut;
            if (grid.dy() < 0){
                gridOut = grid;
            } else {
                float[] flipped = RasterUtils.flipVertically(grid.data(), grid.nx());
                gridOut = VortexGrid.toBuilder(grid)
                        .originY(grid.originY() + grid.dy() * grid.ny())
                        .data(flipped)
                        .build();
            }

            Dataset dataset = RasterUtils.getDatasetFromVortexGrid(
                    RasterUtils.resetNoDataValue(gridOut, NO_DATA_VALUE)
            );
            ArrayList<String> gdalOptions = new ArrayList<>();
            gdalOptions.add("-of");
            gdalOptions.add("AAIGrid");
            gdalOptions.add("-co");
            gdalOptions.add("FORCE_CELLSIZE=TRUE");
            TranslateOptions translateOptions = new TranslateOptions(new Vector<>(gdalOptions));
            Dataset out = gdal.Translate(pathToDestination, dataset, translateOptions);
            out.FlushCache();
            out.delete();
        });
    }
}
