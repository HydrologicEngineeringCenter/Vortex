package mil.army.usace.hec.vortex.geo;

import mil.army.usace.hec.vortex.MessageStore;
import mil.army.usace.hec.vortex.Options;
import mil.army.usace.hec.vortex.VortexProperty;
import mil.army.usace.hec.vortex.io.DataReader;
import mil.army.usace.hec.vortex.util.Stopwatch;
import org.locationtech.jts.geom.Envelope;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class BatchSubsetter implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(BatchSubsetter.class.getName());

    private final String pathToInput;
    private final Set<String> variables;
    private final Path destination;
    private final Map<String, String> writeOptions;
    private final PropertyChangeSupport support;

    private final Envelope envelope;
    private final String envelopeWkt;

    private BatchSubsetter(Builder builder){
        pathToInput = builder.pathToInput;
        variables = builder.variables;
        destination = builder.destination;
        writeOptions = builder.writeOptions;
        this.support = new PropertyChangeSupport(this);

        Path envelopeDataSource = Path.of(builder.envelopeDataSource);
        envelope = VectorUtils.getEnvelope(envelopeDataSource);
        envelopeWkt = VectorUtils.getWkt(envelopeDataSource);
    }

    public static class Builder {
        private String pathToInput;
        private Set<String> variables;
        private boolean isSelectAll;
        private String envelopeDataSource;
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

        public Builder setEnvelopeDataSource(String envelopeDataSource) {
            this.envelopeDataSource = envelopeDataSource;
            return this;
        }

        public Builder destination(String destination) {
            this.destination = Path.of(destination);
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

    @Override
    public void run() {
        process();
    }

    public void process(){
        Stopwatch stopwatch = new Stopwatch();
        stopwatch.start();
        
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

        AtomicInteger processed = new AtomicInteger();
        int totalCount = units.size();

        String templateBegin = MessageStore.getInstance().getMessage("clipper_begin");
        String messageBegin = String.format(templateBegin, totalCount);
        support.firePropertyChange(VortexProperty.STATUS.toString(), null, messageBegin);

        units.parallelStream().forEach(unit -> {
            int newValue = (int) (((float) processed.incrementAndGet() / totalCount) * 100);
            support.firePropertyChange("progress", null, newValue);
            unit.process();
        });

        stopwatch.end();
        String timeMessage = "Batch subset time: " + stopwatch;
        LOGGER.info(timeMessage);

        String templateEnd = MessageStore.getInstance().getMessage("clipper_end");
        String messageEnd = String.format(templateEnd, processed, destination);
        support.firePropertyChange(VortexProperty.COMPLETE.toString(), null, messageEnd);

        String templateTime = MessageStore.getInstance().getMessage("clipper_time");
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
