package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.GdalRegister;
import mil.army.usace.hec.vortex.VortexData;
import org.gdal.gdal.gdal;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

class AscZipDataReader extends DataReader implements VirtualFileSystem {
    static {
        GdalRegister.getInstance();
    }

    AscZipDataReader(DataReaderBuilder builder) { super(builder); }

    @Override
    public List<VortexData> getDtos() {
        String vPath = getVirtualPath(path);

        Vector fileList = gdal.ReadDir(vPath);
        List<VortexData> dtos = new ArrayList<>();
        for (Object o : fileList) {
            String fileName = o.toString();
            if (fileName.endsWith(".asc")) {
                DataReader reader = DataReader.builder()
                        .path(vPath + fileName)
                        .variable(variableName)
                        .build();

                dtos.addAll(reader.getDtos());
            }
        }
        return dtos;
    } // Extended getDtos(): Asc Zip

    @Override
    public int getDtoCount() {
        String vPath = getVirtualPath(path);

        Vector fileList = gdal.ReadDir(vPath);

        AtomicInteger count = new AtomicInteger();
        for (Object o : fileList) {
            String name = o.toString();
            if (name.endsWith(".asc"))
                count.incrementAndGet();
        }
        return count.get();
    } // Extended getDtoCount(): Asc

    @Override
    public VortexData getDto(int idx) {
        String vPath = getVirtualPath(path);

        Vector fileList = gdal.ReadDir(vPath);
        int count = 0;
        for (Object o : fileList) {
            String fileName = o.toString();
            if (fileName.endsWith(".asc")) {
                count++;
                if (count - 1 == idx) {
                    DataReader reader = DataReader.builder()
                            .path(vPath + fileName)
                            .variable(variableName)
                            .build();

                    return reader.getDto(0);
                }
            }
        }
        return null;
    } // Extended getDto(): Asc Zip

    @Override
    public List<VortexDataInterval> getDataIntervals() {
        return getDtos().stream()
                .map(VortexDataInterval::of)
                .toList();
    }

} // AscZipDataReader class