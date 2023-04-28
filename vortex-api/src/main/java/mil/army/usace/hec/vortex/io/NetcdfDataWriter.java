package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.VortexGridCollection;
import mil.army.usace.hec.vortex.convert.NetcdfGridWriter;
import ucar.nc2.Attribute;
import ucar.nc2.write.Nc4Chunking;
import ucar.nc2.write.Nc4ChunkingStrategy;
import ucar.nc2.write.NetcdfFileFormat;
import ucar.nc2.write.NetcdfFormatWriter;

import java.util.List;
import java.util.stream.Collectors;

public class NetcdfDataWriter extends DataWriter {
    private final VortexGridCollection collection;

    // NetCDF4 Settings
    private static final Nc4Chunking.Strategy CHUNKING_STRATEGY = Nc4Chunking.Strategy.standard;
    private static final int DEFLATE_LEVEL = 9;
    private static final boolean SHUFFLE = false;
    private static final NetcdfFileFormat NETCDF_FORMAT = NetcdfFileFormat.NETCDF4;

    /* Constructor */
    NetcdfDataWriter(Builder builder) {
        super(builder);

        List<VortexGrid> grids = data.stream()
                .filter(VortexGrid.class::isInstance)
                .map(VortexGrid.class::cast)
                .collect(Collectors.toList());
        collection = new VortexGridCollection(grids);
    }

    /* Write */
    @Override
    public void write() {
        Nc4Chunking chunker = Nc4ChunkingStrategy.factory(CHUNKING_STRATEGY, DEFLATE_LEVEL, SHUFFLE);
        NetcdfFormatWriter.Builder writerBuilder = NetcdfFormatWriter.builder()
                .setNewFile(true)
                .setFormat(NETCDF_FORMAT)
                .setLocation(destination.toString())
                .setChunker(chunker);
        addGlobalAttributes(writerBuilder);

        NetcdfGridWriter gridWriter = new NetcdfGridWriter(collection);
        gridWriter.addListener(e -> fireWriteCompleted());
        gridWriter.write(writerBuilder);
    }

    @Override
    public boolean canSupportConcurrentWrite() {
        return false;
    }

    /* Add Global Attributes */
    private void addGlobalAttributes(NetcdfFormatWriter.Builder writerBuilder) {
        writerBuilder.addAttribute(new Attribute("Conventions", "CF-1.10"));
    }
}
