package mil.army.usace.hec.vortex.geo;

import mil.army.usace.hec.vortex.GdalRegister;
import mil.army.usace.hec.vortex.Options;
import mil.army.usace.hec.vortex.io.DataReader;
import org.locationtech.jts.geom.Envelope;

import java.awt.geom.Rectangle2D;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BatchSubsetter {
    static {
        GdalRegister.getInstance();
    }
    private final String pathToInput;
    private final Set<String> variables;
    private final Path destination;
    private final Options writeOptions;

    private final Envelope envelope;
    private final String envelopeWkt;

    private BatchSubsetter(Builder builder){
        pathToInput = builder.pathToInput;
        variables = builder.variables;
        destination = builder.destination;
        writeOptions = builder.writeOptions;

        String envelopeDataSource = builder.envelopeDataSource;
        Rectangle2D rectangle = VectorUtils.getEnvelope(Paths.get(envelopeDataSource));
        envelope = VectorUtils.toEnvelope(rectangle);
        envelopeWkt = VectorUtils.getWkt(Paths.get(envelopeDataSource));
    }

    public static class Builder {

        private String pathToInput;
        private Set<String> variables;
        boolean isSelectAll;
        private String envelopeDataSource;
        private Path destination;
        private Options writeOptions;

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

        public Builder setEnvelopeDataSource(String envelopeDataSource) {
            this.envelopeDataSource = envelopeDataSource;
            return this;
        }

        public Builder destination(String destination) {
            this.destination = Paths.get(destination);
            return this;
        }

        public Builder writeOptions(Options writeOptions) {
            this.writeOptions = writeOptions;
            return this;
        }

        public BatchSubsetter build() {
            if(isSelectAll){
                variables = new HashSet<>();
                variables.addAll(DataReader.getVariables(pathToInput));
            }
            return new BatchSubsetter(this);
        }
    }

    public static Builder builder(){
        return new Builder();
    }

    public void process(){


        List<SubsettableUnit> units = new ArrayList<>();
        variables.forEach(variable -> {
            if (DataReader.getVariables(pathToInput).contains(variable)) {

                DataReader reader = DataReader.builder()
                        .path(pathToInput)
                        .variable(variable)
                        .build();

                SubsettableUnit unit = SubsettableUnit.builder()
                        .reader(reader)
                        .setEnvelope(envelope)
                        .setEnvelopeWkt(envelopeWkt)
                        .destination(destination)
                        .writeOptions(writeOptions)
                        .build();

                units.add(unit);
            }
        });
        units.parallelStream().forEach(SubsettableUnit::process);
    }
}
