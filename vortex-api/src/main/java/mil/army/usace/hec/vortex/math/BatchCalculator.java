package mil.army.usace.hec.vortex.math;

import mil.army.usace.hec.vortex.MessageStore;
import mil.army.usace.hec.vortex.Options;
import mil.army.usace.hec.vortex.VortexProperty;
import mil.army.usace.hec.vortex.io.DataReader;
import mil.army.usace.hec.vortex.util.Stopwatch;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BatchCalculator implements Runnable {
    private static final Logger logger = Logger.getLogger(BatchCalculator.class.getName());

    private final String pathToInput;
    private final Set<String> variables;
    private final float multiplyValue;
    private final float divideValue;
    private final float addValue;
    private final float subtractValue;
    private final Path destination;
    private final Map<String, String> writeOptions;

    private final PropertyChangeSupport support;

    private BatchCalculator(Builder builder){
        pathToInput = builder.pathToInput;
        variables = builder.variables;
        multiplyValue = builder.multiplyValue;
        divideValue = builder.divideValue;
        addValue = builder.addValue;
        subtractValue = builder.subtractValue;
        destination = builder.destination;
        writeOptions = builder.writeOptions;
        support = new PropertyChangeSupport(this);

        logger.setLevel(Level.INFO);
    }

    public static class Builder {

        private String pathToInput;
        private Set<String> variables;
        boolean isSelectAll;
        private float multiplyValue = Float.NaN;
        private float divideValue = Float.NaN;
        private float addValue = Float.NaN;
        private float subtractValue = Float.NaN;
        private Path destination;
        private Map<String, String> writeOptions = new HashMap<>();

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

        public Builder writeOptions(Map<String, String> writeOptions) {
            this.writeOptions = writeOptions;
            return this;
        }

        public BatchCalculator build() {
            if(isSelectAll){
                variables = new HashSet<>();
                variables.addAll(DataReader.getVariables(pathToInput));
            }
            return new BatchCalculator(this);
        }
    }

    public static Builder builder(){
        return new Builder();
    }

    @Override
    public void run() {
        process();
    }

    public void process(){
        Stopwatch stopwatch = new Stopwatch();
        stopwatch.start();

        List<CalculatableUnit> units = new ArrayList<>();
        variables.forEach(variable -> {
            if (DataReader.getVariables(pathToInput).contains(variable)) {

                DataReader reader = DataReader.builder()
                        .path(pathToInput)
                        .variable(variable)
                        .build();

                CalculatableUnit unit = CalculatableUnit.builder()
                        .reader(reader)
                        .multiplyValue(multiplyValue)
                        .divideValue(divideValue)
                        .addValue(addValue)
                        .subtractValue(subtractValue)
                        .destination(destination)
                        .writeOptions(writeOptions)
                        .build();

                units.add(unit);
            }
        });

        AtomicInteger processed = new AtomicInteger();
        int totalCount = units.size();

        String templateBegin = MessageStore.getInstance().getMessage("calculator_begin");
        String messageBegin = String.format(templateBegin, totalCount);
        support.firePropertyChange(VortexProperty.STATUS.toString(), null, messageBegin);

        units.parallelStream().forEach(unit -> {
            unit.addPropertyChangeListener(evt -> {
                int newValue = (int) (((float) processed.incrementAndGet() / totalCount) * 100);
                support.firePropertyChange("progress", null, newValue);
            });

            unit.process();
        });

        stopwatch.end();
        String timeMessage = "Batch calculator time: " + stopwatch;
        logger.info(timeMessage);

        String templateEnd = MessageStore.getInstance().getMessage("calculator_end");
        String messageEnd = String.format(templateEnd, processed, destination);
        support.firePropertyChange(VortexProperty.COMPLETE.toString(), null, messageEnd);

        String templateTime = MessageStore.getInstance().getMessage("calculator_time");
        String messageTime = String.format(templateTime, stopwatch);
        support.firePropertyChange(VortexProperty.STATUS.toString(), null, messageTime);
    }

    public void addPropertyChangeListener(PropertyChangeListener pcl) {
        this.support.addPropertyChangeListener(pcl);
    }

    public void removePropertyChangeListener(PropertyChangeListener pcl) {
        this.support.removePropertyChangeListener(pcl);
    }
}
