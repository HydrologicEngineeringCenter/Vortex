package mil.army.usace.hec.vortex.io.reader;

import mil.army.usace.hec.vortex.io.DataSourceType;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class FileDataReaderPool {
    private static final Map<String, FileDataReader> readerPool = new ConcurrentHashMap<>();

    private FileDataReaderPool() {
        // Hidden
    }

    public static FileDataReader get(String pathToFile) {
        return readerPool.computeIfAbsent(pathToFile, FileDataReaderPool::create);
    }

    public static FileDataReader get(String pathToFile, String pathToData) {
        return readerPool.computeIfAbsent(pathToFile, k -> create(k, pathToData));
    }

    public static Set<String> getVariables(String pathToFile) {
        DataSourceType dataSourceType = DataSourceType.fromPathToFile(pathToFile);
        return switch (dataSourceType) {
            case ASC, ASC_ZIP -> AscDataReader.getVariables(pathToFile);
            case SNODAS_DAT, SNODAS_TAR -> SnodasDataReader.getVariables(pathToFile);
            case BIL, BIL_ZIP -> BilDataReader.getVariables(pathToFile);
            case DSS -> DssDataReader.getVariables(pathToFile);
            case NETCDF -> NetcdfDataReader.getVariables(pathToFile);
            default -> Collections.emptySet();
        };
    }

    private static FileDataReader create(String pathToFile) {
        DataSourceType dataSourceType = DataSourceType.fromPathToFile(pathToFile);
        return switch (dataSourceType) {
            case ASC -> new AscDataReader(pathToFile);
            case ASC_ZIP -> new AscZipDataReader(pathToFile, "*");
            case SNODAS_DAT -> new SnodasDataReader(pathToFile);
            case SNODAS_TAR -> new SnodasTarDataReader(pathToFile, "*");
            case BIL -> new BilDataReader(pathToFile);
            case BIL_ZIP -> new BilZipDataReader(pathToFile, "*");
            case DSS -> new DssDataReader(pathToFile, "*");
            case NETCDF -> NetcdfDataReader.createInstance(pathToFile, "*");
            default -> null;
        };
    }

    private static FileDataReader create(String pathToFile, String pathToData) {
        DataSourceType dataSourceType = DataSourceType.fromPathToFile(pathToFile);
        return switch (dataSourceType) {
            case ASC -> new AscDataReader(pathToFile);
            case ASC_ZIP -> new AscZipDataReader(pathToFile, pathToData);
            case SNODAS_DAT -> new SnodasDataReader(pathToFile);
            case SNODAS_TAR -> new SnodasTarDataReader(pathToFile, pathToData);
            case BIL -> new BilDataReader(pathToFile);
            case BIL_ZIP -> new BilZipDataReader(pathToFile, pathToData);
            case DSS -> new DssDataReader(pathToFile, pathToData);
            case NETCDF -> NetcdfDataReader.createInstance(pathToFile, pathToData);
            default -> null;
        };
    }
}
