package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexTimeRecord;
import mil.army.usace.hec.vortex.io.reader.*;

import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.List;
import java.util.Set;

public final class DataReader implements PropertyChangeNotifier {
    private final PropertyChangeSupport support = new PropertyChangeSupport(this);
    private final String path;
    private final String variableName;
    private final FileDataReader fileDataReader;

    DataReader(DataReaderBuilder builder){
        this.path = builder.path;
        this.variableName = builder.variableName;
        this.fileDataReader = builder.fileDataReader;
    } // DataReader builder()

    @Override
    public PropertyChangeSupport getPropertyChangeSupport() {
        return support;
    }

    public static class DataReaderBuilder{
        private String path;
        private String variableName;
        private FileDataReader fileDataReader;

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

            fileDataReader = FileDataReaderPool.get(path, variableName);
            return new DataReader(this);
        } // build()
    } // DataReaderBuilder class

    public static DataReaderBuilder builder(){
        return new DataReaderBuilder();
    }

    public List<VortexData> getDtos() {
        return fileDataReader.getDtos();
    }

    public static Set<String> getVariables(String path){
        return FileDataReaderPool.getVariables(path);
    } // builder()

    public int getDtoCount() {
        return fileDataReader.getDtoCount();
    }

    public VortexData getDto(int idx) {
        return fileDataReader.getDto(idx);
    }

    public List<VortexTimeRecord> getTimeRecords() {
        return fileDataReader.getTimeRecords();
    }

    public static boolean isVariableRequired(String pathToFile) {
        String fileName = new File(pathToFile).getName().toLowerCase();
        return !fileName.matches(".*\\.(asc|tif|tiff|bil|bil.zip|asc.zip)$");
    }

    public String getPath() {
        return path;
    }

    public String getVariableName() {
        return variableName;
    }
}
