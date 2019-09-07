package mil.army.usace.hec.vortex.io;

import hec.heclib.dss.DSSPathname;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.geo.RasterUtils;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.TranslateOptions;
import org.gdal.gdal.gdal;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;

public class TiffDataWriter extends DataWriter {

    TiffDataWriter(DataWriterBuilder builder) {
        super(builder);
    }

    @Override
    public void write() {
        List<VortexGrid> grids = data.stream()
                .filter(vortexData -> vortexData instanceof VortexGrid)
                .map(vortexData -> (VortexGrid) vortexData)
                .collect(Collectors.toList());

        grids.forEach(grid -> {
            String directory = destination.getParent().toString();
            String fileName = autoGenerateFileName(grid);

            Path finalDestination = Paths.get(directory, fileName);

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
            Dataset out = gdal.Translate(finalDestination.toString(), dataset, translateOptions);
            out.FlushCache();
            out.delete();
        });
    }

    public static String autoGenerateFileName(String prefix, VortexGrid grid){
        String fileNameBase;

        PathMatcher dssMatcher = FileSystems.getDefault().getPathMatcher("glob:**/*.dss");
        if (dssMatcher.matches(Paths.get(grid.fileName()))) {
            String baseName;
            if (prefix != null && !prefix.isEmpty()){
                baseName = prefix;
            } else {
                String fileNameIn = Paths.get(grid.fileName()).getFileName().toString();
                baseName = fileNameIn.substring(0, fileNameIn.lastIndexOf('.'));
            }
            DSSPathname pathname = new DSSPathname(grid.fullName());
            StringBuilder fileNameBuilder = new StringBuilder();
            fileNameBuilder.append(baseName);
            fileNameBuilder.append("_");
            if (!pathname.getAPart().isEmpty()){
                fileNameBuilder.append(pathname.getAPart().toLowerCase());
                fileNameBuilder.append("_");
            }
            if (!pathname.getBPart().isEmpty()){
                fileNameBuilder.append(pathname.getBPart().toLowerCase());
                fileNameBuilder.append("_");
            }
            if (!pathname.getCPart().isEmpty()){
                fileNameBuilder.append(pathname.getCPart().toLowerCase());
                fileNameBuilder.append("_");
            }
            if (!pathname.getDPart().isEmpty()){
                String dateString = pathname.getDPart();
                DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                        .parseCaseInsensitive()
                        .appendPattern("ddMMMuuuu:HHmm")
                        .toFormatter();
                LocalDateTime date = LocalDateTime.parse(dateString, formatter);
                fileNameBuilder.append(date.toString().replace(":", ""));
                fileNameBuilder.append("_");
            }
            if (!pathname.getEPart().isEmpty()){
                String dateString = pathname.getEPart();
                DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                        .parseCaseInsensitive()
                        .appendPattern("ddMMMuuuu:HHmm")
                        .toFormatter();
                LocalDateTime date = LocalDateTime.parse(dateString, formatter);
                fileNameBuilder.append(date.toString().replace(":", ""));
                fileNameBuilder.append("_");
            }
            if (!pathname.getFPart().isEmpty()){
                fileNameBuilder.append(pathname.getFPart().toLowerCase());
            }
            fileNameBase = fileNameBuilder.toString().replaceAll("[^a-zA-Z0-9]", "_") + ".tiff";
        } else {
            String fileNameIn = Paths.get(grid.fileName()).getFileName().toString();
            String fileNameInSansExt = fileNameIn.substring(0, fileNameIn.lastIndexOf('.'));
            StringBuilder fileNameBuilder = new StringBuilder();
            fileNameBuilder.append(fileNameInSansExt);
            fileNameBuilder.append("_");
            if (grid.shortName() != null){
                fileNameBuilder.append(grid.shortName().toLowerCase());
                fileNameBuilder.append("_");
            }
            if (grid.startTime() != null){
                fileNameBuilder.append(grid.startTime().toString());
                fileNameBuilder.append("_");
            }
            if (grid.endTime() != null && !grid.endTime().equals(grid.startTime())){
                fileNameBuilder.append(grid.endTime().toString());
                fileNameBuilder.append("_");
            }
            fileNameBase = fileNameBuilder.toString().replaceAll("[^a-zA-Z0-9]", "_") + ".tiff";
        }
        return fileNameBase.replaceAll("_\\.", ".");
    }

    public static String autoGenerateFileName(VortexGrid grid){
        return autoGenerateFileName(null, grid);
    }
}
