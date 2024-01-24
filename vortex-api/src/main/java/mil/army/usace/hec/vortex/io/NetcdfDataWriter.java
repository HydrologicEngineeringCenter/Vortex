package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.VortexProperty;
import ucar.nc2.Attribute;
import ucar.nc2.write.Nc4Chunking;
import ucar.nc2.write.Nc4ChunkingStrategy;
import ucar.nc2.write.NetcdfFileFormat;
import ucar.nc2.write.NetcdfFormatWriter;

import java.beans.PropertyChangeListener;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NetcdfDataWriter extends DataWriter {
    private final List<VortexGrid> vortexGridList;

    private static final Map<String, Boolean> fileFirstWriteCompleted = Collections.synchronizedMap(new HashMap<>());
    private final boolean overwriteExistingFile;

    // NetCDF4 Settings
    public static final Nc4Chunking.Strategy CHUNKING_STRATEGY = Nc4Chunking.Strategy.standard;
    public static final int DEFLATE_LEVEL = 9;
    public static final boolean SHUFFLE = false;
    public static final NetcdfFileFormat NETCDF_FORMAT = NetcdfFileFormat.NETCDF4;

    /* Constructor */
    NetcdfDataWriter(Builder builder) {
        super(builder);

        synchronized (NetcdfDataWriter.class) {
            // Check if file exists and get user preference for overwriting
            boolean fileExists = Files.exists(destination);
            boolean userPrefersOverwrite = getOverwritePreference();

            String pathToFile = destination.toString();
            boolean firstWriteCompleted = fileFirstWriteCompleted.getOrDefault(pathToFile, false);

            if (!fileExists || userPrefersOverwrite && !firstWriteCompleted) {
                overwriteExistingFile = true;
                fileFirstWriteCompleted.put(pathToFile, true);
            } else {
                overwriteExistingFile = false;
            }
        }


        vortexGridList = data.stream()
                .filter(VortexGrid.class::isInstance)
                .map(VortexGrid.class::cast)
                .toList();
    }

    private boolean getOverwritePreference() {
        String overwriteOption = options.get("isOverwrite");
        return overwriteOption == null || Boolean.parseBoolean(overwriteOption);
    }

    /* Write */
    @Override
    public void write() {
        if (overwriteExistingFile) overwriteData();
        else appendData();
    }

    private void overwriteData() {
        NetcdfFormatWriter.Builder writerBuilder = initWriterBuilder();
        addGlobalAttributes(writerBuilder);

        NetcdfGridWriter gridWriter = new NetcdfGridWriter(vortexGridList);
        gridWriter.addListener(writerPropertyListener());
        gridWriter.write(writerBuilder);
    }

    public void appendData() {
        NetcdfFormatWriter.Builder writerBuilder = initWriterBuilder();

        NetcdfGridWriter gridWriter = new NetcdfGridWriter(vortexGridList);
        gridWriter.addListener(writerPropertyListener());
        gridWriter.appendData(writerBuilder);
    }

    private NetcdfFormatWriter.Builder initWriterBuilder() {
        Nc4Chunking chunker = Nc4ChunkingStrategy.factory(CHUNKING_STRATEGY, DEFLATE_LEVEL, SHUFFLE);
        return NetcdfFormatWriter.builder()
                .setNewFile(overwriteExistingFile)
                .setFormat(NETCDF_FORMAT)
                .setLocation(destination.toString())
                .setChunker(chunker);
    }

    private PropertyChangeListener writerPropertyListener() {
        return e -> {
            String propertyName = e.getPropertyName();

            if (propertyName.equals(VortexProperty.ERROR)) {
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
