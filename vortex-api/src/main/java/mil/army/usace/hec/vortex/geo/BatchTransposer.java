package mil.army.usace.hec.vortex.geo;

import mil.army.usace.hec.vortex.Options;
import mil.army.usace.hec.vortex.io.DataReader;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BatchTransposer {
    private String pathToInput;
    private Set<String> variables;
    private double angle;
    private Double stormCenterX;
    private Double stormCenterY;
    private Path destination;
    private Options writeOptions;

    private BatchTransposer(BatchTransposerBuilder builder){
        pathToInput = builder.pathToInput;
        variables = builder.variables;
        angle = builder.angle;
        stormCenterX = builder.stormCenterX;
        stormCenterY = builder.stormCenterY;
        destination = builder.destination;
        writeOptions = builder.writeOptions;
    }

    public static class BatchTransposerBuilder {

        private String pathToInput;
        private Set<String> variables;
        boolean isSelectAll;
        private double angle;
        private Double stormCenterX;
        private Double stormCenterY;
        private Path destination;
        private Options writeOptions;

        public BatchTransposerBuilder pathToInput(String pathToInput) {
            this.pathToInput = pathToInput;
            return this;
        }

        public BatchTransposerBuilder variables(List<String> variables) {
            this.variables = new HashSet<>(variables);
            return this;
        }

        public BatchTransposerBuilder selectAllVariables(){
            this.isSelectAll = true;
            return this;
        }

        public BatchTransposerBuilder angle(double angle) {
            this.angle = angle;
            return this;
        }

        public BatchTransposerBuilder stormCenterX(Double stormCenterX) {
            this.stormCenterX = stormCenterX;
            return this;
        }

        public BatchTransposerBuilder stormCenterY(Double stormCenterY) {
            this.stormCenterY = stormCenterY;
            return this;
        }

        public BatchTransposerBuilder destination(String destination) {
            this.destination = Paths.get(destination);
            return this;
        }

        public BatchTransposerBuilder writeOptions(Options writeOptions) {
            this.writeOptions = writeOptions;
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

    public static BatchTransposerBuilder builder(){
        return new BatchTransposerBuilder();
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
                        .destination(destination)
                        .writeOptions(writeOptions)
                        .build();

                units.add(unit);
            }
        });
        units.parallelStream().forEach(TransposableUnit::process);
    }
}
