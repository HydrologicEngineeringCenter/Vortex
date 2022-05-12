package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.geo.RasterUtils;
import mil.army.usace.hec.vortex.util.MatrixUtils;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.TranslateOptions;
import org.gdal.gdal.gdal;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;

public class AscDataWriter extends DataWriter {
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
            VortexGrid gridOut;
            if (grid.dy() < 0){
                gridOut = grid;
            } else {
                float[] flipped = MatrixUtils.flipArray(grid.data(), grid.nx(), grid.ny());
                gridOut = VortexGrid.builder()
                        .dx(grid.dx())
                        .dy(grid.dy())
                        .nx(grid.nx())
                        .ny(grid.ny())
                        .originX(grid.originX())
                        .originY(grid.originY() + grid.dy() * grid.ny())
                        .wkt(grid.wkt())
                        .data(flipped)
                        .noDataValue(grid.noDataValue())
                        .units(grid.units())
                        .fileName(grid.fileName())
                        .shortName(grid.shortName())
                        .fullName(grid.fullName())
                        .description(grid.description())
                        .startTime(grid.startTime())
                        .endTime(grid.endTime())
                        .interval(grid.interval())
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
            Dataset out = gdal.Translate(destination.toString(), dataset, translateOptions);
            out.FlushCache();
            out.delete();
        });
    }
}
