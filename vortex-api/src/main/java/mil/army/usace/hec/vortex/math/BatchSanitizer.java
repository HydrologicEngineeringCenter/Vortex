package mil.army.usace.hec.vortex.math;

import mil.army.usace.hec.vortex.Options;
import mil.army.usace.hec.vortex.io.DataReader;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class BatchSanitizer {
    private final String pathToInput;
    private final Set<String> variables;
    private final double minimumThreshold;
    private final double maximumThreshold;
    private final float minimumReplacementValue;
    private final float maximumReplacementValue;
    private final Path destination;
    private final Map<String, String> writeOptions;

    private BatchSanitizer(Builder builder){
        pathToInput = builder.pathToInput;
        variables = builder.variables;
        minimumThreshold = builder.minimumThreshold;
        maximumThreshold = builder.maximumThreshold;
        minimumReplacementValue = builder.minimumReplacementValue;
        maximumReplacementValue = builder.maximumReplacementValue;
        destination = builder.destination;
        writeOptions = builder.writeOptions;
    }

    public static class Builder {

        private String pathToInput;
        private Set<String> variables;
        boolean isSelectAll;
        private double minimumThreshold = -Double.MAX_VALUE;
        private double maximumThreshold = Double.MAX_VALUE;
        private float minimumReplacementValue = Float.NaN;
        private float maximumReplacementValue = Float.NaN;
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

        public BatchSanitizer build() {
            if(isSelectAll){
                variables = new HashSet<>();
                variables.addAll(DataReader.getVariables(pathToInput));
            }
            return new BatchSanitizer(this);
        }
    }

    public static Builder builder(){
        return new Builder();
    }

    public void process(){
        List<SanitizableUnit> units = new ArrayList<>();
        variables.forEach(variable -> {
            if (DataReader.getVariables(pathToInput).contains(variable)) {

                DataReader reader = DataReader.builder()
                        .path(pathToInput)
                        .variable(variable)
                        .build();

                SanitizableUnit unit = SanitizableUnit.builder()
                        .reader(reader)
                        .minimumThreshold(minimumThreshold)
                        .minimumReplacementValue(minimumReplacementValue)
                        .maximumThreshold(maximumThreshold)
                        .maximumReplacementValue(maximumReplacementValue)
                        .destination(destination)
                        .writeOptions(writeOptions)
                        .build();

                units.add(unit);
            }
        });
        units.parallelStream().forEach(SanitizableUnit::process);
    }
}
