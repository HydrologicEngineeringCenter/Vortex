package mil.army.usace.hec.vortex.geo;

import mil.army.usace.hec.vortex.Options;
import mil.army.usace.hec.vortex.io.DataReader;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class BatchTransposer {
    private final String pathToInput;
    private final Set<String> variables;
    private final double angle;
    private final Double stormCenterX;
    private final Double stormCenterY;
    private final Double scaleFactor;
    private final Path destination;
    private final Map<String, String> writeOptions;

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

    public void process(){
        List<TransposableUnit> units = new ArrayList<>();
        variables.forEach(variable -> {
            if (DataReader.getVariables(pathToInput).contains(variable)) {

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
        });
        units.parallelStream().forEach(TransposableUnit::process);
    }
}
