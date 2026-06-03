package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.util.FilenameUtil;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Base class for all Vortex data readers.
 *
 * <h2>API migration: checked read failures</h2>
 *
 * As of this release, the read methods declare a checked
 * {@link DataReadException}:
 * <ul>
 *   <li>{@link #getDtos()}</li>
 *   <li>{@link #getDto(int)}</li>
 *   <li>{@link #getDataIntervals()}</li>
 *   <li>{@link DataReaderBuilder#build()}</li>
 *   <li>{@link #copy(DataReader)}</li>
 * </ul>
 *
 * <p>Previously these methods silently returned {@code null} on failure;
 * callers had no way to distinguish a missing record from an I/O error.
 * Now read failures surface as {@link DataReadException}, which carries a
 * {@link DataReadException.Kind Kind} so callers can decide whether to skip
 * the record or abort the operation.
 *
 * <p>Migration for downstream consumers:
 * <ul>
 *   <li>{@code DataReadException} extends {@link java.io.IOException}, so
 *       callers that already declare {@code throws IOException} or catch
 *       {@code IOException} continue to compile unchanged.</li>
 *   <li>Callers that previously checked for {@code null} from
 *       {@code getDto(int)} or filtered nulls out of {@code getDtos()} should
 *       now either let the exception propagate or catch
 *       {@code DataReadException} and switch on
 *       {@link DataReadException#getKind()}.</li>
 * </ul>
 */
public abstract class DataReader implements AutoCloseable {
    private static final Pattern SUPPORTED_ARCHIVE_PATTERN = Pattern.compile(
            "snodas.*\\.tar$|bil\\.zip$|asc\\.zip$",
            Pattern.CASE_INSENSITIVE
    );

    private static final Set<String> ARCHIVE_EXTENSIONS = Set.of(
            ".zip", ".tar", ".tar.gz", ".tgz", ".tar.bz2", ".tbz2",
            ".tar.xz", ".txz", ".tar.zst", ".tzst", ".tar.z",
            ".7z", ".rar", ".gz", ".bz2", ".xz", ".zst", ".lz4"
    );

    final PropertyChangeSupport support;

    final String path;
    final String variableName;

    DataReader(DataReaderBuilder builder) {
        this.path = builder.path;
        this.variableName = builder.variableName;

        support = new PropertyChangeSupport(this);
    } // DataReader builder()

    public static class DataReaderBuilder {
        private String path;
        private String variableName;

        public DataReaderBuilder path(final String path) {
            this.path = path;
            return this;
        } // Get path()

        public DataReaderBuilder variable(final String variable) {
            this.variableName = variable;
            return this;
        } // Get variable()

        public DataReader build() throws DataReadException {
            if (path == null) {
                throw new IllegalStateException("DataReader requires a path to data source file.");
            }

            if (path.matches(".*\\.(asc|tif|tiff)$")) {
                return new AscDataReader(this);
            }

            if (path.toLowerCase().contains("snodas") && path.endsWith(".tar")) {
                return new SnodasTarDataReader(this);
            }

            if (path.endsWith(".dat")) {
                return new SnodasDataReader(this);
            }

            if (path.matches(".*\\.bil")) {
                return new BilDataReader(this);
            }

            if (path.matches(".*bil.zip")) {
                return new BilZipDataReader(this);
            }

            if (path.matches(".*asc.zip")) {
                return new AscZipDataReader(this);
            }

            if (variableName == null) {
                throw new IllegalStateException("This DataReader requires a variableName.");
            }

            if (path.matches(".*\\.dss")) {
                return new DssDataReader(this);
            }

            return NetcdfDataReader.createInstance(path, variableName);
        } // build()
    } // DataReaderBuilder class

    public static DataReaderBuilder builder() {
        return new DataReaderBuilder();
    }

    public static DataReader copy(DataReader dataReader) throws DataReadException {
        return DataReader.builder()
                .path(dataReader.path)
                .variable(dataReader.variableName)
                .build();
    }

    /**
     * Reads every record this reader can produce.
     *
     * @return all records in catalog order; never {@code null}.
     * @throws DataReadException if the underlying source cannot be read or
     *         a record fails to decode. The reader does not return a partial
     *         result on failure.
     */
    public abstract List<VortexData> getDtos() throws DataReadException;

    public static Set<String> getVariables(String path) {
        String fileName = new File(path).getName().toLowerCase();

        if (FilenameUtil.endsWithExtensions(fileName, ".asc", ".tif", ".tiff", "asc.zip")) {
            return AscDataReader.getVariables(path);
        } else if (FilenameUtil.endsWithExtensions(fileName, ".bil", "bil.zip")) {
            return BilDataReader.getVariables(path);
        } else if (fileName.contains("snodas") && FilenameUtil.endsWithExtensions(fileName, ".dat", ".tar", ".tar.gz")) {
            return SnodasDataReader.getVariables(path);
        } else if (FilenameUtil.endsWithExtensions(fileName, ".dss")) {
            return DssDataReader.getVariables(path);
        } else {
            return NetcdfDataReader.getVariables(path);
        }
    } // builder()

    /**
     * Number of records this reader exposes — equivalent to {@code getDtos().size()}
     * but without paying the cost of reading every record's payload.
     */
    public abstract int getDtoCount();

    /**
     * Reads a single record by zero-based index.
     *
     * @param idx record index in catalog order, in {@code [0, getDtoCount())}.
     * @return the record at {@code idx}; never {@code null}.
     * @throws IndexOutOfBoundsException if {@code idx} is outside
     *         {@code [0, getDtoCount())}. Out-of-range is a programmer error,
     *         not a read failure.
     * @throws DataReadException if the record exists in the catalog but the
     *         underlying read fails (status code, JNI failure, decoder error).
     */
    public abstract VortexData getDto(int idx) throws DataReadException;

    /**
     * Returns the time intervals associated with this reader's records, in
     * the same order as {@link #getDto(int)} would return them. Implementations
     * that can derive intervals from metadata alone may avoid reading record
     * payloads; implementations that can't will load records and thus may
     * fail with {@link DataReadException}.
     */
    public abstract List<VortexDataInterval> getDataIntervals() throws DataReadException;

    public static boolean isVariableRequired(String pathToFile) {
        String fileName = new File(pathToFile).getName().toLowerCase();

        return !fileName.matches(".*\\.(asc|tif|tiff|bil|bil.zip|asc.zip)$");
    }

    public static boolean isArchive(String pathToFile) {
        if (pathToFile == null) return false;

        String lower = pathToFile.toLowerCase();

        return ARCHIVE_EXTENSIONS.stream().anyMatch(lower::endsWith);
    }

    public static boolean isSupportedArchive(String pathToFile) {
        if (pathToFile == null)
            return false;

        return SUPPORTED_ARCHIVE_PATTERN.matcher(pathToFile).matches();
    }

    public abstract Validation isValid();

    public String getVariableName() {
        return variableName;
    }

    public void addPropertyChangeListener(PropertyChangeListener pcl) {
        support.addPropertyChangeListener(pcl);
    }

    public void removePropertyChangeListener(PropertyChangeListener pcl) {
        support.removePropertyChangeListener(pcl);
    }
} // DataReader class
