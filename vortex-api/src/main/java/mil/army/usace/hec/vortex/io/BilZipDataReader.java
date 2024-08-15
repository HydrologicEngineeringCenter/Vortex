package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.GdalRegister;
import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.temporal.VortexTimeRecord;
import org.gdal.gdal.gdal;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

class BilZipDataReader extends DataReader implements VirtualFileSystem{
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
    } // // Extended getDtos(): Bil Zip

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
    } // // Extended getDtoCount(): Bil

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
    } // Extended getDto(): Bil Zip

    @Override
    public List<VortexTimeRecord> getTimeRecords() {
        return getDtos().stream()
                .map(VortexTimeRecord::of)
                .toList();
    }

} // BilZipDataReader class
