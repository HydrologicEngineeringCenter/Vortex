package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.Options;
import mil.army.usace.hec.vortex.VortexData;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;

public abstract class DataWriter {
    final Path destination;
    final List<VortexData> data;
    final Options options;

    DataWriter(DataWriterBuilder builder){
        this.destination = builder.destination;
        this.data = builder.data;
        this.options = builder.options;
    }

    public static class DataWriterBuilder{
        private Path destination;
        private List<VortexData> data;
        private Options options;

        public DataWriterBuilder destination (final Path destination){
            this.destination = destination;
            return this;
        }

        public DataWriterBuilder data (final List<VortexData> data){
            this.data = data;
            return this;
        }

        public DataWriterBuilder options (final Options options){
            this.options = options;
            return this;
        }

        public DataWriter build(){
            if (destination == null){
                throw new IllegalStateException("Invalid destination.");
            }

            if (data == null){
                throw new IllegalStateException("Invalid data.");
            }

            PathMatcher dssMatcher = FileSystems.getDefault().getPathMatcher("glob:**/*.dss");
            if (dssMatcher.matches(destination)) {
                return new DssDataWriter(this);
            }

            PathMatcher tiffMatcher = FileSystems.getDefault().getPathMatcher("glob:**/*.tiff");
            if (tiffMatcher.matches(destination)) {
                return new TiffDataWriter(this);
            }

            throw new IllegalStateException("Invalid destination.");
        }
    }

    public static DataWriterBuilder builder(){return new DataWriterBuilder();}

    public abstract void write();

}

