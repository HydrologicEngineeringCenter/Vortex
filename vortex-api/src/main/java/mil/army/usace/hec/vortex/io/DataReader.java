package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.util.FilenameUtil;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.List;
import java.util.Set;

public abstract class DataReader {
    final PropertyChangeSupport support;

    final String path;
    final String variableName;

    DataReader(DataReaderBuilder builder){
        this.path = builder.path;
        this.variableName = builder.variableName;

        support = new PropertyChangeSupport(this);
    } // DataReader builder()

    public static class DataReaderBuilder{
        private String path;
        private String variableName;

        public DataReaderBuilder path (final String path){
            this.path = path;
            return this;
        } // Get path()

        public DataReaderBuilder variable (final String variable){
            this.variableName = variable;
            return this;
        } // Get variable()

        public DataReader build(){
            if (path == null){
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

            if (variableName == null){
                throw new IllegalStateException("This DataReader requires a variableName.");
            }

            if (path.matches(".*\\.dss")) {
                return new DssDataReader(this);
            }

            return NetcdfDataReader.createInstance(path, variableName);
        } // build()
    } // DataReaderBuilder class

    public static DataReaderBuilder builder(){return new DataReaderBuilder();}

    public abstract List<VortexData> getDtos();

    public static Set<String> getVariables(String path){
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

    public abstract int getDtoCount();

    public abstract VortexData getDto(int idx);

    public abstract List<VortexDataInterval> getDataIntervals();

    public static boolean isVariableRequired(String pathToFile) {
        String fileName = new File(pathToFile).getName().toLowerCase();

        return !fileName.matches(".*\\.(asc|tif|tiff|bil|bil.zip|asc.zip)$");
    }

    public void addPropertyChangeListener(PropertyChangeListener pcl) {
        support.addPropertyChangeListener(pcl);
    }

    public void removePropertyChangeListener(PropertyChangeListener pcl) {
        support.removePropertyChangeListener(pcl);
    }
} // DataReader class
