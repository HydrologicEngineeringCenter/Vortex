package mil.army.usace.hec.vortex.io;

import hec.heclib.dss.HecDSSFileAccess;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

/**
 * DSS file version checks.
 *
 * <p>Vortex reads and writes DSS version 7. Handing a version 6 file to the
 * native layer upgrades it in place on open, rewriting a file the caller never
 * asked to have rewritten and, for gridded records, not necessarily preserving
 * the values it held. Callers are stopped at the door instead, and pointed at
 * the DSS 7 migration utility, where the conversion is explicit.
 */
public final class DssVersion {

    /** What every read and write path tells the caller when it refuses. */
    static final String DSS6_MESSAGE =
            "%s is a DSS version 6 file. Vortex reads and writes DSS version 7. "
                    + "Migrate it with the Vortex DSS 7 Migration utility, then retry.";

    private DssVersion() {
    }

    /**
     * True when {@code path} names an existing DSS version 6 file. Anything
     * else -- a version 7 file, a file that does not exist yet, a NetCDF
     * source, an unreadable path -- is false, leaving those cases to the
     * caller's own validation.
     */
    public static boolean isDss6(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        if (!path.toLowerCase().endsWith(".dss")) {
            return false;
        }
        try {
            if (!Files.isRegularFile(Path.of(path))) {
                return false;
            }
        } catch (InvalidPathException e) {
            return false;
        }
        return HecDSSFileAccess.getDssFileVersion(path) == 6;
    }

    /** {@link #isDss6(String)} for a {@link Path}. */
    public static boolean isDss6(Path path) {
        return path != null && isDss6(path.toString());
    }

    /**
     * Throws if {@code path} is a DSS version 6 file. For the read path, whose
     * callers already handle {@link DataReadException}.
     */
    static void requireNotDss6ForRead(String path) throws DataReadException {
        if (isDss6(path)) {
            throw DataReadException.of(DataReadException.Kind.UNSUPPORTED, path, null, 0,
                    String.format(DSS6_MESSAGE, path), null);
        }
    }

    /**
     * Throws if {@code path} is a DSS version 6 file. Unchecked, for the entry
     * points that declare no checked exception -- {@code DataWriter.write()}
     * and the static catalog reads -- where widening the signature would break
     * every caller. Matches the {@code IllegalStateException} the writer's
     * builder already throws for an unusable destination.
     *
     * <p>Deliberately not a quiet empty result: a batch that silently found no
     * variables would report success having done nothing, which is harder to
     * diagnose than a refusal that names the file.
     */
    static void requireNotDss6(String path) {
        if (isDss6(path)) {
            throw new IllegalStateException(String.format(DSS6_MESSAGE, path));
        }
    }

    /** {@link #requireNotDss6(String)} for a {@link Path}. */
    static void requireNotDss6(Path path) {
        if (path != null) {
            requireNotDss6(path.toString());
        }
    }
}
