package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.VortexData;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.Set;

public abstract class DataReader {
    final Path path;
    final String variableName;

    DataReader(DataReaderBuilder builder){
        this.path = builder.path;
        this.variableName = builder.variableName;
    }

    public static class DataReaderBuilder{
        private Path path;
        private String variableName;

        public DataReaderBuilder path (final Path path){
            this.path = path;
            return this;
        }

        public DataReaderBuilder variable (final String variable){
            this.variableName = variable;
            return this;
        }

        public DataReader build(){
            if (path == null){
                throw new IllegalStateException("DataReader requires a path to data source file.");
            }

            PathMatcher ascMatcher = FileSystems.getDefault().getPathMatcher("glob:**/*.asc");
            if (ascMatcher.matches(path)) {
                return new AscDataReader(this);
            }

            if (variableName == null){
                throw new IllegalStateException("This DataReader requires a variableName.");
            }

            PathMatcher dssMatcher = FileSystems.getDefault().getPathMatcher("glob:**/*.dss");
            if (dssMatcher.matches(path)) {
                return new DssDataReader(this);
            }

            return new NetcdfDataReader(this);
        }
    }

    public static DataReaderBuilder builder(){return new DataReaderBuilder();}

    public abstract List<VortexData> getDtos();

    public static Set<String> getVariables(Path path){
        String fileName = path.getFileName().toString().toLowerCase();

        if (fileName.endsWith(".asc")){
            return AscDataReader.getVariables(path);
        }
        if (fileName.endsWith(".dss")){
            return DssDataReader.getVariables(path);
        }
        return NetcdfDataReader.getVariables(path);
    }

    public abstract int getDtoCount();

    public abstract VortexData getDto(int idx);
}
