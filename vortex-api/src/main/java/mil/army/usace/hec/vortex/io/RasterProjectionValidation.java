package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.GdalRegister;
import mil.army.usace.hec.vortex.Message;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.geo.ReferenceUtils;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Vector;
import java.util.logging.Logger;

/** Checks source projections; raster, DSS, and NetCDF readers inspect metadata without loading cell values. */
public final class RasterProjectionValidation {
    private static final Logger logger = Logger.getLogger(RasterProjectionValidation.class.getName());
    static {
        GdalRegister.getInstance();
    }

    private RasterProjectionValidation() {}

    /**
     * Validates a raster's coordinate system using GDAL without reading cell values.
     * Accepts filesystem paths and GDAL virtual paths. An unreadable raster or a
     * missing or invalid projection returns an invalid result with a source-specific message.
     * Native resources are released before returning; no dialogs are displayed.
     */
    public static Validation validateRaster(String path) {
        Objects.requireNonNull(path, "path must not be null");
        Dataset dataset = null;
        try {
            dataset = gdal.Open(path);
            if (dataset == null) {
                return Validation.of(false, Message.format("error_invalid_file", path));
            }
            String wkt = dataset.GetProjection();
            return validateProjection(wkt, path);
        } catch (RuntimeException e) {
            logger.fine(e::getMessage);
            return Validation.of(false, Message.format("error_invalid_file", path));
        } finally {
            if (dataset != null) {
                dataset.delete();
            }
        }
    }

    /**
     * Checks a source before processing. Raster readers inspect metadata, including
     * archive entries. DSS readers check every matching record's projection metadata;
     * NetCDF readers use coordinate-system metadata for each selected variable.
     * Other readers validate the first grid of each selected variable.
     * This deliberately does not present or change unrelated reader warnings.
     */
    public static Validation validateSource(String path, Collection<String> variables) {
        Objects.requireNonNull(path, "path must not be null");
        Objects.requireNonNull(variables, "variables must not be null");
        List<String> messages = new ArrayList<>();
        try {
            if (!DataReader.isVariableRequired(path)) {
                try (DataReader reader = DataReader.builder().path(path).build()) {
                    return reader.isValid();
                }
            }
            for (String variable : variables) {
                try (DataReader reader = DataReader.builder().path(path).variable(variable).build()) {
                    messages.addAll(validateReaderProjection(reader).getMessages());
                }
            }
        } catch (Exception e) {
            logger.fine(e::getMessage);
            return Validation.of(false, Message.format("error_projection_read", path, e.getMessage()));
        }
        return Validation.of(messages.isEmpty(), messages);
    }

    static Validation validateReaderProjection(DataReader reader) throws DataReadException {
        if (reader instanceof DssDataReader) {
            return reader.isValid();
        }
        // Empty time series are handled by the existing reader warnings.
        if (reader.getDtoCount() == 0) {
            return Validation.of(true);
        }
        String source = reader.path + " : " + reader.variableName;
        if (reader instanceof NetcdfDataReader netcdfReader) {
            return validateProjection(netcdfReader.getCrs(), source);
        }
        if (reader.getDto(0) instanceof VortexGrid grid) {
            return validateProjection(grid.wkt(), source);
        }
        return Validation.of(true);
    }

    static Validation validateProjection(String wkt, String source) {
        if (wkt == null || wkt.isBlank()) {
            return Validation.of(false, Message.format("warn_raster_missing_projection", source));
        }
        if (!ReferenceUtils.isValidProjection(wkt)) {
            return Validation.of(false, Message.format("warn_raster_invalid_projection", source));
        }
        return Validation.of(true);
    }

    static Validation validateArchive(String virtualPath, String extension) {
        Vector<?> entries = gdal.ReadDir(virtualPath);
        if (entries == null) {
            return Validation.of(false, Message.format("error_invalid_file", virtualPath));
        }
        boolean valid = true;
        List<String> messages = new ArrayList<>();
        for (Object entry : entries) {
            String name = entry.toString();
            if (name.endsWith(extension)) {
                Validation validation = validateRaster(virtualPath + name);
                valid &= validation.isValid();
                messages.addAll(validation.getMessages());
            }
        }
        return Validation.of(valid, messages);
    }
}
