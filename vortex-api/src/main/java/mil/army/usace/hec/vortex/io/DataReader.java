package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.VortexData;

import java.io.File;
import java.util.List;
import java.util.Set;

public abstract class DataReader {
    final String path;
    final String variableName;

    DataReader(DataReaderBuilder builder){
        this.path = builder.path;
        this.variableName = builder.variableName;
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

            if (path.matches(".*\\.asc")) {
                return new AscDataReader(this);
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

            return new NetcdfDataReader(this);
        } // build()
    } // DataReaderBuilder class

    public static DataReaderBuilder builder(){return new DataReaderBuilder();}

    public abstract List<VortexData> getDtos();

    public static Set<String> getVariables(String path){
        String fileName = new File(path).getName().toLowerCase();

        if (fileName.endsWith(".asc")){
            return AscDataReader.getVariables(path);
        }
        if (fileName.endsWith(".bil") || fileName.endsWith("bil.zip")){
            return BilDataReader.getVariables(path);
        }
        if (fileName.endsWith(".dss")){
            return DssDataReader.getVariables(path);
        }
        return NetcdfDataReader.getVariables(path);
    } // builder()

    protected String getVirtualPath (String pathToArchive){
        // GDAL Virtual path for zip/gzip/tar/tgz files
        String vPath;
        if (pathToArchive.contains(".zip")) {
            vPath = "/vsizip/" + path + File.separator;
        } else if (pathToArchive.contains(".gzip")) {
            vPath = "/vsigzip/" + path + File.separator;
        } else if (pathToArchive.contains(".tar") || pathToArchive.contains(".tgz")) {
            vPath = "/vsitar/" + path + File.separator;
        } else {
            throw new IllegalStateException("File is not *.zip, *.gzip, *.tar, or *.tgz");
        }
        return vPath;
    } // getVirtualPath()

    public abstract int getDtoCount();

    public abstract VortexData getDto(int idx);
} // DataReader class
