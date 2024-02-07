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

public class GridCalculatableUnit {
    private final DataReader reader;
    private final VortexGrid raster;
    private final Operation operation;
    private final Path destination;
    private final Map<String, String> writeOptions;

    private final PropertyChangeSupport support;

    private GridCalculatableUnit(Builder builder) {
        reader = builder.reader;
        raster = builder.raster;
        operation = builder.operation;
        destination = builder.destination;
        writeOptions = builder.writeOptions;

        support = new PropertyChangeSupport(this);
    }

    public static class Builder {
        private DataReader reader;
        private VortexGrid raster;
        private Operation operation;
        private Path destination;
        private final Map<String, String> writeOptions = new HashMap<>();

        public Builder reader(DataReader reader) {
            this.reader = reader;
            return this;
        }
        
        public Builder raster(VortexGrid raster){
            this.raster = raster;
            return this;
        }

        public Builder operation(Operation operation) {
            this.operation = operation;
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

        public GridCalculatableUnit build() {
            if (reader == null) {
                throw new IllegalArgumentException("DataReader must be provided");
            }
            if (destination == null) {
                throw new IllegalArgumentException("Destination must be provided");
            }
            return new GridCalculatableUnit(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public void process() {
        List<VortexGrid> grids = reader.getDtos().stream()
                .map(grid -> (VortexGrid) grid)
                .collect(Collectors.toList());

        grids.forEach(grid -> {
            GridCalculator calculator = GridCalculator.builder()
                    .data(grid.data())
                    .rasterData(raster.data())
                    .operation(operation)
                    .build();

            float[] calculated = calculator.calculate();

            VortexGrid output = VortexGrid.toBuilder(grid)
                    .data(calculated)
                    .build();

            List<VortexData> data = new ArrayList<>();
            data.add(output);

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
