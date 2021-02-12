package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.geo.RasterUtils;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.TranslateOptions;
import org.gdal.gdal.gdal;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;

public class TiffDataWriter extends DataWriter {

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
            Dataset dataset = RasterUtils.getDatasetFromVortexGrid(grid);
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
            TranslateOptions translateOptions = new TranslateOptions(new Vector<>(gdalOptions));
            Dataset out = gdal.Translate(destination.toString(), dataset, translateOptions);
            out.FlushCache();
            out.delete();
        });
    }
}
