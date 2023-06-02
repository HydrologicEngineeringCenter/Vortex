package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.VortexGrid;
import ucar.nc2.Attribute;
import ucar.nc2.write.Nc4Chunking;
import ucar.nc2.write.Nc4ChunkingStrategy;
import ucar.nc2.write.NetcdfFileFormat;
import ucar.nc2.write.NetcdfFormatWriter;

import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.stream.Collectors;

public class NetcdfDataWriter extends DataWriter {
    private final List<VortexGrid> vortexGridList;
    private final boolean isOverwrite;

    // NetCDF4 Settings
    public static final Nc4Chunking.Strategy CHUNKING_STRATEGY = Nc4Chunking.Strategy.standard;
    public static final int DEFLATE_LEVEL = 9;
    public static final boolean SHUFFLE = false;
    public static final NetcdfFileFormat NETCDF_FORMAT = NetcdfFileFormat.NETCDF4;

    /* Constructor */
    NetcdfDataWriter(Builder builder) {
        super(builder);
        String overwriteOption = options.get("isOverwrite");
        isOverwrite = overwriteOption == null || Boolean.parseBoolean(overwriteOption);
        vortexGridList = data.stream()
                .filter(VortexGrid.class::isInstance)
                .map(VortexGrid.class::cast)
                .collect(Collectors.toList());
    }

    /* Write */
    @Override
    public void write() {
        if (isOverwrite) overwriteData();
        else appendData();
    }

    private void overwriteData() {
        NetcdfFormatWriter.Builder writerBuilder = initWriterBuilder(isOverwrite);
        addGlobalAttributes(writerBuilder);

        NetcdfGridWriter gridWriter = new NetcdfGridWriter(vortexGridList);
        gridWriter.addListener(writerPropertyListener());
        gridWriter.write(writerBuilder);
    }

    public void appendData() {
        NetcdfFormatWriter.Builder writerBuilder = initWriterBuilder(isOverwrite);
        NetcdfGridWriter gridWriter = new NetcdfGridWriter(vortexGridList);
        gridWriter.addListener(writerPropertyListener());
        gridWriter.appendData(writerBuilder);
    }

    private NetcdfFormatWriter.Builder initWriterBuilder(boolean isOverwrite) {
        Nc4Chunking chunker = Nc4ChunkingStrategy.factory(CHUNKING_STRATEGY, DEFLATE_LEVEL, SHUFFLE);
        return NetcdfFormatWriter.builder()
                .setNewFile(isOverwrite)
                .setFormat(NETCDF_FORMAT)
                .setLocation(destination.toString())
                .setChunker(chunker);
    }

    private PropertyChangeListener writerPropertyListener() {
        return e -> {
            String propertyName = e.getPropertyName();
            if (propertyName.equals(DataWriter.WRITE_COMPLETED)) {
                fireWriteCompleted();
            }

            if (propertyName.equals(DataWriter.WRITE_ERROR)) {
                String errorMessage = String.valueOf(e.getNewValue());
                fireWriteError(errorMessage);
            }
        };
    }

    /* Add Global Attributes */
    private void addGlobalAttributes(NetcdfFormatWriter.Builder writerBuilder) {
        writerBuilder.addAttribute(new Attribute("Conventions", "CF-1.10"));
    }
}
