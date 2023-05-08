package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.Options;
import mil.army.usace.hec.vortex.VortexData;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.*;

public abstract class DataWriter {
    final PropertyChangeSupport support = new PropertyChangeSupport(this);
    final Path destination;
    final List<VortexData> data;
    final Map<String, String> options = new HashMap<>();

    public static final String WRITE_ERROR = "WriteError";
    public static final String WRITE_COMPLETED = "WriteCompleted";

    DataWriter(Builder builder){
        this.destination = builder.destination;
        this.data = builder.data;
        this.options.putAll(builder.options);
    }

    public static class Builder {
        private Path destination;
        private List<VortexData> data;
        private final Map<String, String> options = new HashMap<>();

        public Builder destination (final Path destination){
            this.destination = destination;
            return this;
        }

        public Builder destination (final String destination){
            this.destination = Paths.get(destination);
            return this;
        }

        public Builder data (final List<VortexData> data){
            this.data = data;
            return this;
        }

        /**
         * @deprecated since 0.10.16, replaced by {@link #options}
         * @param options  the geographic options
         * @return the builder
         */
        @Deprecated
        public Builder options (final Options options) {
            Optional.ofNullable(options).ifPresent(o -> this.options.putAll(o.getOptions()));
            return this;
        }

        public Builder options (final Map<String, String> options) {
            this.options.putAll(options);
            return this;
        }

        public DataWriter build() {
            if (destination == null){
                throw new IllegalStateException("Invalid destination.");
            }

            if (data == null) {
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

            PathMatcher ascMatcher = FileSystems.getDefault().getPathMatcher("glob:**/*.asc");
            if (ascMatcher.matches(destination)) {
                return new AscDataWriter(this);
            }

            PathMatcher ncMatcher = FileSystems.getDefault().getPathMatcher("glob:**/*.nc*");
            if (ncMatcher.matches(destination)) {
                return new NetcdfDataWriter(this);
            }

            throw new IllegalStateException("Invalid destination: " + destination);
        }
    }

    public static Builder builder(){return new Builder();}

    public abstract void write();

    /* Property Change */
    void fireWriteCompleted() {
        support.firePropertyChange(DataWriter.WRITE_COMPLETED, null, null);
    }

    void fireWriteError(String errorMessage) {
        support.firePropertyChange(WRITE_ERROR, null, errorMessage);
    }

    public void addListener(PropertyChangeListener pcl) {
        support.addPropertyChangeListener(pcl);
    }

    public void removeListener(PropertyChangeListener pcl) {
        support.removePropertyChangeListener(pcl);
    }
}

