package mil.army.usace.hec.vortex.math;

import mil.army.usace.hec.vortex.Options;
import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.io.DataReader;
import mil.army.usace.hec.vortex.io.DataWriter;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class SanitizableUnit {
    private final DataReader reader;
    private final double minimumThreshold;
    private final double maximumThreshold;
    private final float minimumReplacementValue;
    private final float maximumReplacementValue;
    private final Path destination;
    private final Map<String, String> writeOptions;

    private final PropertyChangeSupport support;

    private SanitizableUnit(Builder builder) {
        reader = builder.reader;
        minimumThreshold = builder.minimumThreshold;
        maximumThreshold = builder.maximumThreshold;
        minimumReplacementValue = builder.minimumReplacementValue;
        maximumReplacementValue = builder.maximumReplacementValue;
        destination = builder.destination;
        writeOptions = builder.writeOptions;

        support = new PropertyChangeSupport(this);
    }

    public static class Builder {
        private DataReader reader;
        private double minimumThreshold = -Double.MAX_VALUE;
        private double maximumThreshold = Double.MAX_VALUE;
        private float minimumReplacementValue = Float.NaN;
        private float maximumReplacementValue = Float.NaN;
        private Path destination;
        private final Map<String, String> writeOptions = new HashMap<>();

        public Builder reader(DataReader reader) {
            this.reader = reader;
            return this;
        }

        public Builder minimumThreshold(double minimumThreshold) {
            this.minimumThreshold = minimumThreshold;
            return this;
        }

        public Builder maximumThreshold(double maximumThreshold) {
            this.maximumThreshold = maximumThreshold;
            return this;
        }

        public Builder minimumReplacementValue(float minimumReplacementValue) {
            this.minimumReplacementValue = minimumReplacementValue;
            return this;
        }

        public Builder maximumReplacementValue(float maximumReplacementValue) {
            this.maximumReplacementValue = maximumReplacementValue;
            return this;
        }

        public Builder destination(Path destination) {
            this.destination = destination;
            return this;
        }

        /**
         * @deprecated since 0.10.16, replaced by {@link #writeOptions}
         * @param writeOptions  the file write options
         * @return the builder
         */
        @Deprecated
        public Builder writeOptions(final Options writeOptions){
            Optional.ofNullable(writeOptions).ifPresent(o -> this.writeOptions.putAll(o.getOptions()));
            return this;
        }

        public Builder writeOptions(Map<String, String> writeOptions){
            this.writeOptions.putAll(writeOptions);
            return this;
        }

        public SanitizableUnit build() {
            if (reader == null) {
                throw new IllegalArgumentException("DataReader must be provided");
            }
            if (destination == null) {
                throw new IllegalArgumentException("Destination must be provided");
            }
            return new SanitizableUnit(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public void process() {
        List<VortexGrid> grids = reader.getDtos().stream().map(grid -> (VortexGrid) grid).collect(Collectors.toList());

        grids.forEach(grid -> {
            Sanitizer sanitizer = Sanitizer.builder()
                    .inputGrid(grid)
                    .maximumThreshold(maximumThreshold)
                    .maximumReplacementValue(maximumReplacementValue)
                    .minimumThreshold(minimumThreshold)
                    .minimumReplacementValue(minimumReplacementValue)
                    .build();

            VortexGrid sanitized = sanitizer.sanitize();

            List<VortexData> data = new ArrayList<>();
            data.add(sanitized);

            DataWriter writer = DataWriter.builder()
                    .data(data)
                    .destination(destination)
                    .options(writeOptions)
                    .build();

            writer.write();
        });

        support.firePropertyChange("complete", null, null);
    }

    public void addPropertyChangeListener(PropertyChangeListener pcl) {
        this.support.addPropertyChangeListener(pcl);
    }

    public void removePropertyChangeListener(PropertyChangeListener pcl) {
        this.support.removePropertyChangeListener(pcl);
    }
}
