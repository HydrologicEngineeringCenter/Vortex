package mil.army.usace.hec.vortex.io;

import java.io.IOException;
import java.io.Serial;
import java.util.Objects;

/**
 * Thrown when a {@link DataReader} implementation fails to read a record from
 * its underlying source (e.g. a non-zero DSS status code, a JNI failure, an
 * unsupported record type). This is distinct from "no data found" — callers
 * that need to differentiate should treat an empty result as a legitimate
 * absence and this exception as a read failure.
 *
 * <p>Checked because every caller in the read path has a real decision to
 * make: surface the error to the user, abort the operation, or substitute a
 * fallback. Silent skipping is never correct for this class of failure.
 *
 * <p>Carries enough context to log meaningfully without leaking the underlying
 * provider's API: a coarse {@link Kind} for control-flow decisions, the raw
 * native status code for diagnostics, the data source path, and the
 * record-level pathname when available.
 */
public final class DataReadException extends IOException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Coarse classification of the failure mode. Callers can switch on this
     * for control-flow decisions (e.g. "skip {@code MISSING_RECORD}, abort on
     * anything else") without having to interpret native status codes.
     * Inspect {@link DataReadException#getStatusCode()} and
     * {@link DataReadException#getCause()} for finer-grained diagnostics.
     *
     * <p>New values may be added in future versions; clients switching on
     * {@code Kind} should always include a {@code default} case.
     */
    public enum Kind {
        /** Record or dataset doesn't exist at the requested location. */
        MISSING_RECORD,
        /** Record exists but is in a format this reader doesn't handle. */
        UNSUPPORTED,
        /** Generic I/O failure — file lock, JNI crash, network drop, etc. */
        IO_ERROR
    }

    private final Kind kind;
    private final String dataPath;
    private final String recordPathname;
    private final int statusCode;

    DataReadException(Kind kind, String dataPath, String recordPathname, int statusCode,
                      String message, Throwable cause) {
        super(message, cause);
        this.kind = Objects.requireNonNull(kind, "kind");
        this.dataPath = dataPath;
        this.recordPathname = recordPathname;
        this.statusCode = statusCode;
    }

    /**
     * Canonical entry point when the {@link Kind} is computed at runtime
     * (e.g., classifying a native status code into a category). Prefer the
     * named factories when the Kind is known statically.
     *
     * @param kind            coarse failure classification; must not be null.
     * @param dataPath        absolute path of the underlying source file (DSS file, NetCDF dataset, etc.).
     * @param recordPathname  provider-specific locator of the failing record within the source
     *                        (e.g., a DSS pathname). {@code null} when the failure is at the
     *                        source level and not tied to a specific record.
     * @param statusCode      raw provider-specific status code, or {@code 0} when none applies.
     * @param message         human-readable summary; should include the data path so log lines
     *                        are self-contained.
     * @param cause           underlying exception, or {@code null} if there isn't one.
     */
    public static DataReadException of(Kind kind, String dataPath, String recordPathname,
                                       int statusCode, String message, Throwable cause) {
        return new DataReadException(kind, dataPath, recordPathname, statusCode, message, cause);
    }

    /**
     * Indicates a generic I/O failure — JNI exception, file lock, network
     * drop, or anything else the provider couldn't classify more specifically.
     */
    public static DataReadException ioError(String dataPath, String recordPathname,
                                            String message, Throwable cause) {
        return new DataReadException(Kind.IO_ERROR, dataPath, recordPathname, 0, message, cause);
    }

    /** I/O failure with no underlying cause to attach. */
    public static DataReadException ioError(String dataPath, String recordPathname, String message) {
        return ioError(dataPath, recordPathname, message, null);
    }

    public Kind getKind() {
        return kind;
    }

    public String getDataPath() {
        return dataPath;
    }

    public String getRecordPathname() {
        return recordPathname;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
