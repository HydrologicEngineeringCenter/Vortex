package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.GdalRegister;
import mil.army.usace.hec.vortex.VortexData;
import org.gdal.gdal.gdal;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

public class BilZipDataReader extends DataReader {
    static {
        GdalRegister.getInstance();
    }

    BilZipDataReader(DataReaderBuilder builder) {
        super(builder);
    }

    @Override
    public List<VortexData> getDtos() {
        String vPath = getVirtualPath(path);

        Vector fileList = gdal.ReadDir(vPath);
        List<VortexData> dtos = new ArrayList<>();
        for (Object o : fileList) {
            String fileName = o.toString();
            if (fileName.endsWith(".bil")) {
                DataReader reader = DataReader.builder()
                        .path(vPath + fileName)
                        .variable(variableName)
                        .build();

                dtos.addAll(reader.getDtos());
            }
        } // Loop through directory

        return dtos;
    } // getDtos() BIL

    @Override
    public int getDtoCount() {
        String vPath = getVirtualPath(path);

        Vector fileList = gdal.ReadDir(vPath);

        AtomicInteger count = new AtomicInteger();
        for (Object o : fileList) {
            String name = o.toString();
            if (name.endsWith(".bil"))
                count.incrementAndGet();
        } // Loop through directory
        return count.get();
    } // getDtoCount()

    @Override
    public VortexData getDto(int idx) {
        String vPath = getVirtualPath(path);

        Vector fileList = gdal.ReadDir(vPath);
        int count = 0;
        for (Object o : fileList) {
            String fileName = o.toString();
            if (fileName.endsWith(".bil")) {
                count++;
                if (count - 1 == idx){
                    DataReader reader = DataReader.builder()
                            .path(vPath + fileName)
                            .variable(variableName)
                            .build();

                    return reader.getDto(0);
                }
            }
        }
        return null;
    }

    private String getVirtualPath (String pathToArchive){
        // GDAL Virtual path for zip/gzip/tar/tgz files
        String vPath;
        if (pathToArchive.contains(".zip")) {
            vPath = "/vsizip/" + path + File.separator;
        } else if (pathToArchive.contains(".gzip")) {
            vPath = "/vsigzip/" + path + File.separator;
        } else if (pathToArchive.contains(".tar") || pathToArchive.contains(".tgz")) {
            vPath = "/vsitar/" + path + File.separator;
        } else {
            throw new IllegalStateException("File is not *.zip, *.gzip, *.tar, or *.tgz");
        }
        return vPath;
    }
}
