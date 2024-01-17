package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.Options;
import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexProperty;

import java.beans.PropertyChangeSupport;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class DataWriter {
    final PropertyChangeSupport support = new PropertyChangeSupport(this);
    final Path destination;
    final List<VortexData> data;
    final Map<String, String> options = new HashMap<>();

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

            PathMatcher tiffMatcher = FileSystems.getDefault().getPathMatcher("glob:**/*.{tif,tiff}");
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

            //trying to match a RAS plan output e.g. MyModel.p01.hdf or MyModel.p12.tmp.hdf
            PathMatcher hdf5Matcher = FileSystems.getDefault().getPathMatcher("glob:**/*.p[0-9][0-9].{hdf,tmp.hdf}");
            if (hdf5Matcher.matches(destination)) {
                return new Hdf5RasPrecipDataWriter(this);
            }

            throw new IllegalStateException("Invalid destination: " + destination);
        }
    }

    public static Builder builder(){return new Builder();}

    public abstract void write();

    void fireWriteError(String errorMessage) {
        support.firePropertyChange(VortexProperty.ERROR, null, errorMessage);
    }
}

