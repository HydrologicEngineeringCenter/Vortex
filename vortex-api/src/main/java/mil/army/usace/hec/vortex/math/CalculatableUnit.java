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

public class CalculatableUnit {
    private final DataReader reader;
    private final float multiplyValue;
    private final float divideValue;
    private final float addValue;
    private final float subtractValue;
    private final Path destination;
    private final Map<String, String> writeOptions;

    private final PropertyChangeSupport support;

    private CalculatableUnit(Builder builder) {
        reader = builder.reader;
        multiplyValue = builder.multiplyValue;
        divideValue = builder.divideValue;
        addValue = builder.addValue;
        subtractValue = builder.subtractValue;
        destination = builder.destination;
        writeOptions = builder.writeOptions;

        support = new PropertyChangeSupport(this);
    }

    public static class Builder {
        private DataReader reader;
        private float multiplyValue = Float.NaN;
        private float divideValue = Float.NaN;
        private float addValue = Float.NaN;
        private float subtractValue = Float.NaN;
        private Path destination;
        private final Map<String, String> writeOptions = new HashMap<>();

        public Builder reader(DataReader reader) {
            this.reader = reader;
            return this;
        }

        public Builder multiplyValue(float multiplyValue) {
            this.multiplyValue = multiplyValue;
            return this;
        }

        public Builder divideValue(float divideValue) {
            this.divideValue = divideValue;
            return this;
        }

        public Builder addValue(float addValue) {
            this.addValue = addValue;
            return this;
        }

        public Builder subtractValue(float subtractValue) {
            this.subtractValue = subtractValue;
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

        public CalculatableUnit build() {
            if (reader == null) {
                throw new IllegalArgumentException("DataReader must be provided");
            }
            if (destination == null) {
                throw new IllegalArgumentException("Destination must be provided");
            }
            return new CalculatableUnit(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public void process() {
        List<VortexGrid> grids = reader.getDtos().stream().map(grid -> (VortexGrid) grid).collect(Collectors.toList());

        grids.forEach(grid -> {
            Calculator calculator = Calculator.builder()
                    .inputGrid(grid)
                    .multiplyValue(multiplyValue)
                    .divideValue(divideValue)
                    .addValue(addValue)
                    .subtractValue(subtractValue)
                    .build();

            VortexGrid calculated = calculator.calculate();

            List<VortexData> data = new ArrayList<>();
            data.add(calculated);

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
