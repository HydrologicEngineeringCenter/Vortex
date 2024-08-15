package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.VortexProperty;
import ucar.nc2.write.Nc4Chunking;
import ucar.nc2.write.Nc4ChunkingStrategy;
import ucar.nc2.write.NetcdfFileFormat;
import ucar.nc2.write.NetcdfFormatWriter;

import java.beans.PropertyChangeListener;
import java.util.List;

class NetcdfDataWriter extends DataWriter {
    private final List<VortexGrid> vortexGridList;

    // NetCDF4 Settings
    public static final Nc4Chunking.Strategy CHUNKING_STRATEGY = Nc4Chunking.Strategy.standard;
    public static final int DEFLATE_LEVEL = 9;
    public static final boolean SHUFFLE = false;
    public static final NetcdfFileFormat NETCDF_FORMAT = NetcdfFileFormat.NETCDF4;

    /* Constructor */
    NetcdfDataWriter(Builder builder) {
        super(builder);
        vortexGridList = data.stream()
                .filter(VortexGrid.class::isInstance)
                .map(VortexGrid.class::cast)
                .toList();
    }

    /* Write */
    @Override
    public void write() {
        appendData();
    }

    public void appendData() {
        NetcdfFormatWriter.Builder writerBuilder = initAppendWriterBuilder(destination.toString());

        NetcdfGridWriter gridWriter = new NetcdfGridWriter(vortexGridList);
        gridWriter.addListener(writerPropertyListener());
        gridWriter.appendData(writerBuilder);
    }

    private static NetcdfFormatWriter.Builder initAppendWriterBuilder(String ncDestination) {
        Nc4Chunking chunker = Nc4ChunkingStrategy.factory(CHUNKING_STRATEGY, DEFLATE_LEVEL, SHUFFLE);
        return NetcdfFormatWriter.builder()
                .setNewFile(false)
                .setFormat(NETCDF_FORMAT)
                .setLocation(ncDestination)
                .setChunker(chunker);
    }

    private PropertyChangeListener writerPropertyListener() {
        return e -> {
            String propertyName = e.getPropertyName();

            if (VortexProperty.ERROR.equals(propertyName)) {
                String errorMessage = String.valueOf(e.getNewValue());
                fireWriteError(errorMessage);
            }
        };
    }

}
