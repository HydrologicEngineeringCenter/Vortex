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

public class TiffDataWriter extends DataWriter {
    private static final double NO_DATA_VALUE = -9999;

    TiffDataWriter(Builder builder) {
        super(builder);
    }

    @Override
    public void write() {
        List<VortexGrid> grids = data.stream()
                .filter(vortexData -> vortexData instanceof VortexGrid)
                .map(vortexData -> (VortexGrid) vortexData)
                .collect(Collectors.toList());

        grids.forEach(grid -> {
            Dataset dataset = RasterUtils.getDatasetFromVortexGrid(
                    RasterUtils.resetNoDataValue(grid, NO_DATA_VALUE)
            );

            boolean isExpandFileName = Boolean.parseBoolean(options.getOrDefault("expandName", "false"));

            String pathToDestination = isExpandFileName ?
                    ImageUtils.expandFileName(destination.toString(), grid, ImageFileType.TIFF) : destination.toString();

            ArrayList<String> gdalOptions = new ArrayList<>();
            gdalOptions.add("-of");
            gdalOptions.add("Gtiff");
            gdalOptions.add("-ot");
            gdalOptions.add("Float32");
            gdalOptions.add("-co");
            gdalOptions.add("TILED=YES");
            gdalOptions.add("-co");
            gdalOptions.add("COMPRESS=DEFLATE");
            gdalOptions.add("-co");
            gdalOptions.add("ZLEVEL=1");
            gdalOptions.add("-co");
            gdalOptions.add("BIGTIFF=YES");
            gdalOptions.add("-a_nodata");
            gdalOptions.add(Double.toString(NO_DATA_VALUE));
            TranslateOptions translateOptions = new TranslateOptions(new Vector<>(gdalOptions));
            Dataset out = gdal.Translate(pathToDestination, dataset, translateOptions);
            out.FlushCache();
            out.delete();
        });
    }
}
