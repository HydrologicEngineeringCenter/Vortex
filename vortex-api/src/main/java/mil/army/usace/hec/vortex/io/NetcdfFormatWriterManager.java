package mil.army.usace.hec.vortex.io;

import ucar.nc2.write.NetcdfFormatWriter;

import java.io.IOException;
import java.util.logging.Logger;

public final class NetcdfFormatWriterManager {
    private static final Logger logger = Logger.getLogger(NetcdfFormatWriterManager.class.getName());
    private static final AutoclosableCacheManager<WriterKey, NetcdfFormatWriter> manager = AutoclosableCacheManager.create();

    private NetcdfFormatWriterManager() {
        // Utility Class
    }

    public record WriterKey(String ncDestination, boolean overwriteExisting) {}

    public static NetcdfFormatWriter getOrCompute(WriterKey key, NetcdfFormatWriter.Builder builder) {
        NetcdfFormatWriter foundWriter = manager.get(key);
        if (foundWriter != null) {
            return foundWriter;
        }

        NetcdfFormatWriter computedWriter = computeWriter(builder);
        manager.put(key, computedWriter);
        return computedWriter;
    }

    public static void remove(WriterKey key) {
        manager.remove(key);
    }

    /* Helpers */
    private static NetcdfFormatWriter computeWriter(NetcdfFormatWriter.Builder builder) {
        try {
            return builder.build();
        } catch (IOException e) {
            logger.warning(e.getMessage());
            return null;
        }
    }
}
