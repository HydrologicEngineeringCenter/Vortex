package mil.army.usace.hec.vortex.math;

import mil.army.usace.hec.vortex.MessageStore;
import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.VortexProperty;
import mil.army.usace.hec.vortex.io.DataReader;
import mil.army.usace.hec.vortex.io.DataWriter;
import mil.army.usace.hec.vortex.io.TemporalDataReader;
import mil.army.usace.hec.vortex.util.DssUtil;
import mil.army.usace.hec.vortex.util.Stopwatch;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.nio.file.Path;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BatchTimeStepResampler implements Runnable {
    private static final Logger logger = Logger.getLogger(BatchTimeStepResampler.class.getName());

    private final String pathToInput;
    private final Set<String> variables;
    private final TimeStep timeStep;
    private final Path destination;
    private final Map<String, String> writeOptions;

    private final PropertyChangeSupport support;

    private BatchTimeStepResampler(Builder builder) {
        pathToInput = builder.pathToInput;
        variables = builder.variables;
        timeStep = builder.timeStep;
        destination = builder.destination;
        writeOptions = builder.writeOptions;
        support = new PropertyChangeSupport(this);

        logger.setLevel(Level.INFO);
    }

    public static class Builder {
        private String pathToInput;
        private Set<String> variables;
        private TimeStep timeStep;
        private Path destination;
        private Map<String, String> writeOptions = new HashMap<>();

        public Builder pathToInput(String pathToInput) {
            this.pathToInput = pathToInput;
            return this;
        }

        public Builder variables(Set<String> variables) {
            this.variables = variables;
            return this;
        }

        public Builder timeStep(TimeStep timeStep) {
            this.timeStep = timeStep;
            return this;
        }

        public Builder destination(String destination) {
            this.destination = Path.of(destination);
            return this;
        }

        public Builder writeOptions(Map<String, String> writeOptions) {
            this.writeOptions = writeOptions;
            return this;
        }

        public BatchTimeStepResampler build() {
            Objects.requireNonNull(pathToInput, "pathToInput is required");
            Objects.requireNonNull(variables, "variables is required");
            Objects.requireNonNull(timeStep, "timeStep is required");
            Objects.requireNonNull(destination, "destination is required");
            return new BatchTimeStepResampler(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void run() {
        process();
    }

    public void process() {
        Stopwatch stopwatch = new Stopwatch();
        stopwatch.start();

        Set<String> condensedVariables = DssUtil.condenseVariables(pathToInput, variables);

        AtomicInteger processed = new AtomicInteger();
        int totalCount = condensedVariables.size();

        String templateBegin = MessageStore.getInstance().getMessage("time_step_resampler_begin");
        String messageBegin = String.format(templateBegin, totalCount);
        support.firePropertyChange(VortexProperty.STATUS.toString(), null, messageBegin);

        Duration interval = Duration.ofMinutes(timeStep.intervalInMinutes());

        condensedVariables.forEach(variable -> {
            try (DataReader reader = DataReader.builder()
                    .path(pathToInput)
                    .variable(variable)
                    .build()) {

                try (TemporalDataReader temporalReader = TemporalDataReader.create(reader)) {
                    Optional<ZonedDateTime> optStart = temporalReader.getStartTime();
                    Optional<ZonedDateTime> optEnd = temporalReader.getEndTime();

                    if (optStart.isEmpty() || optEnd.isEmpty()) {
                        logger.warning(() -> "Could not determine time range for variable: " + variable);
                        return;
                    }

                    ZonedDateTime startTime = optStart.get();
                    ZonedDateTime endTime = optEnd.get();
                    ZonedDateTime currentStart = startTime.toLocalDate().atStartOfDay(startTime.getZone());
                    ZonedDateTime currentEnd = currentStart.plus(interval);

                    while (!currentEnd.isAfter(endTime)) {
                        Optional<VortexGrid> optGrid = temporalReader.read(currentStart, currentEnd);
                        if (optGrid.isPresent()) {
                            List<VortexData> grids = List.of(optGrid.get());

                            DataWriter writer = DataWriter.builder()
                                    .data(grids)
                                    .destination(destination)
                                    .options(writeOptions)
                                    .build();

                            writer.write();
                        }

                        currentStart = currentEnd;
                        currentEnd = currentStart.plus(interval);
                    }
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, e, e::getMessage);
            }

            int newValue = (int) (((float) processed.incrementAndGet() / totalCount) * 100);
            support.firePropertyChange(VortexProperty.PROGRESS.toString(), null, newValue);
        });

        stopwatch.end();
        String timeMessage = "Batch time-step resampler time: " + stopwatch;
        logger.info(timeMessage);

        String templateEnd = MessageStore.getInstance().getMessage("time_step_resampler_end");
        String messageEnd = String.format(templateEnd, processed, destination);
        support.firePropertyChange(VortexProperty.COMPLETE.toString(), null, messageEnd);

        String templateTime = MessageStore.getInstance().getMessage("time_step_resampler_time");
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
