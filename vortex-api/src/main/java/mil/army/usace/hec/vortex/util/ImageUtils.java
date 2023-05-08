package mil.army.usace.hec.vortex.util;

import hec.heclib.dss.DSSPathname;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.io.ImageFileType;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

public class ImageUtils {

    private ImageUtils(){}

    public static String generateFileName(String prefix, VortexGrid grid, ImageFileType type){
        String fileNameBase;
        PathMatcher dssMatcher = FileSystems.getDefault().getPathMatcher("glob:**/*.dss");
        if (dssMatcher.matches(Paths.get(grid.fileName()))) {
            DSSPathname pathname = new DSSPathname(grid.fullName());
            StringBuilder fileNameBuilder = new StringBuilder();
            fileNameBuilder.append(prefix);
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
            fileNameBase = fileNameBuilder.toString();
        } else {
            StringBuilder fileNameBuilder = new StringBuilder();
            fileNameBuilder.append(prefix);
            fileNameBuilder.append("_");
            if (grid.shortName() != null){
                fileNameBuilder.append(grid.shortName().toLowerCase());
                fileNameBuilder.append("_");
            }
            if (grid.startTime() != null){
                fileNameBuilder.append(grid.startTime().toString().replace(":", ""));
                fileNameBuilder.append("_");
            }
            if (grid.endTime() != null && !grid.endTime().equals(grid.startTime())){
                fileNameBuilder.append(grid.endTime().toString().replace(":", ""));
                fileNameBuilder.append("_");
            }
            fileNameBase = fileNameBuilder.toString();
        }
        String fileNameClean = fileNameBase.replaceAll("[^A-Za-z0-9]", "_");
        return (fileNameClean + "." + type.label.toLowerCase()).replaceAll("_\\.", ".");
    }

    public static String expandFileName(String pathToFile, VortexGrid grid, ImageFileType type) {
        Path filePath = Path.of(pathToFile);
        String directory = filePath.getParent().toString();
        String fileName = filePath.getFileName().toString();
        String fileNameSansExt = removeExtension(fileName);

        String expandedFileName = generateFileName(fileNameSansExt, grid, type);

        return Paths.get(directory, expandedFileName).toString();
    }

    private static String removeExtension(final String s) {
        return s != null && s.lastIndexOf(".") > 0 ? s.substring(0, s.lastIndexOf(".")) : s;
    }
}
