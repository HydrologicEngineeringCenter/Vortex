package mil.army.usace.hec.vortex.geo;

import mil.army.usace.hec.vortex.Options;
import mil.army.usace.hec.vortex.io.DataReadException;
import mil.army.usace.hec.vortex.io.DataReadExceptions;
import mil.army.usace.hec.vortex.io.DataReader;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

public class BatchTransposer {
    private static final Logger LOGGER = Logger.getLogger(BatchTransposer.class.getName());

    private final String pathToInput;
    private final Set<String> variables;
    private final double angle;
    private final Double stormCenterX;
    private final Double stormCenterY;
    private final Double scaleFactor;
    private final Path destination;
    private final Map<String, String> writeOptions;

    private final PropertyChangeSupport support = new PropertyChangeSupport(this);

    private BatchTransposer(Builder builder){
        pathToInput = builder.pathToInput;
        variables = builder.variables;
        angle = builder.angle;
        stormCenterX = builder.stormCenterX;
        stormCenterY = builder.stormCenterY;
        scaleFactor = builder.scaleFactor;
        destination = builder.destination;
        writeOptions = builder.writeOptions;
    }

    public void addPropertyChangeListener(PropertyChangeListener pcl) {
        support.addPropertyChangeListener(pcl);
    }

    public void removePropertyChangeListener(PropertyChangeListener pcl) {
        support.removePropertyChangeListener(pcl);
    }

    public static class Builder {

        private String pathToInput;
        private Set<String> variables;
        boolean isSelectAll;
        private double angle;
        private Double stormCenterX;
        private Double stormCenterY;
        private Double scaleFactor;
        private Path destination;
        private final Map<String, String> writeOptions = new HashMap<>();

        public Builder pathToInput(String pathToInput) {
            this.pathToInput = pathToInput;
            return this;
        }

        public Builder variables(List<String> variables) {
            this.variables = new HashSet<>(variables);
            return this;
        }

        public Builder selectAllVariables(){
            this.isSelectAll = true;
            return this;
        }

        public Builder angle(double angle) {
            this.angle = angle;
            return this;
        }

        public Builder stormCenterX(Double stormCenterX) {
            this.stormCenterX = stormCenterX;
            return this;
        }

        public Builder stormCenterY(Double stormCenterY) {
            this.stormCenterY = stormCenterY;
            return this;
        }

        public Builder scaleFactor(Double scaleFactor){
            this.scaleFactor = scaleFactor;
            return this;
        }

        public Builder destination(String destination) {
            this.destination = Paths.get(destination);
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

        public BatchTransposer build() {
            if(isSelectAll){
                variables = new HashSet<>();
                variables.addAll(DataReader.getVariables(pathToInput));
            }
            return new BatchTransposer(this);
        }
    }

    public static Builder builder(){
        return new Builder();
    }

    public void process() throws DataReadException {
        // Catalog the source once — getVariables can do a full DSS catalog
        // scan, so calling it inside the per-variable loop was an O(N²) hot
        // spot for sources with many records.
        Set<String> availableVariables = DataReader.getVariables(pathToInput);
        List<TransposableUnit> units = new ArrayList<>();
        for (String variable : variables) {
            if (!availableVariables.contains(variable)) {
                continue;
            }
            DataReader reader = DataReader.builder()
                    .path(pathToInput)
                    .variable(variable)
                    .build();
            TransposableUnit unit = TransposableUnit.builder()
                    .reader(reader)
                    .angle(angle)
                    .stormCenterX(stormCenterX)
                    .stormCenterY(stormCenterY)
                    .scaleFactor(scaleFactor)
                    .destination(destination)
                    .writeOptions(writeOptions)
                    .build();
            units.add(unit);
        }
        units.parallelStream().forEach(unit -> {
            try {
                unit.process();
            } catch (DataReadException e) {
                DataReadExceptions.reportTo(LOGGER, support, e);
            }
        });
    }
}
