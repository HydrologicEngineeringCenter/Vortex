package mil.army.usace.hec.vortex.math;

import mil.army.usace.hec.vortex.MessageStore;
import mil.army.usace.hec.vortex.Options;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.VortexProperty;
import mil.army.usace.hec.vortex.geo.Resampler;
import mil.army.usace.hec.vortex.geo.ResamplingMethod;
import mil.army.usace.hec.vortex.io.DataReader;
import mil.army.usace.hec.vortex.util.Stopwatch;
import org.locationtech.jts.geom.Envelope;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BatchGridCalculator implements Runnable {
    private static final Logger logger = Logger.getLogger(BatchGridCalculator.class.getName());

    private final String pathToInput;
    private final Set<String> variables;
    private final String pathToRaster;
    private final ResamplingMethod resamplingMethod;
    private final Operation operation;
    private final Path destination;
    private final Map<String, String> writeOptions;

    private final PropertyChangeSupport support;

    private BatchGridCalculator(Builder builder){
        pathToInput = builder.pathToInput;
        variables = builder.variables;
        pathToRaster = builder.pathToRaster;
        resamplingMethod = builder.resamplingMethod;
        operation = builder.operation;
        destination = builder.destination;
        writeOptions = builder.writeOptions;
        support = new PropertyChangeSupport(this);

        logger.setLevel(Level.INFO);
    }

    public static class Builder {

        private String pathToInput;
        private Set<String> variables;
        private boolean isSelectAll;
        private String pathToRaster;
        private ResamplingMethod resamplingMethod;
        private Operation operation;
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

        public Builder setPathToRaster(String pathToRaster) {
            this.pathToRaster = pathToRaster;
            return this;
        }

        public Builder setResamplingMethod(ResamplingMethod resamplingMethod) {
            this.resamplingMethod = resamplingMethod;
            return this;
        }

        public Builder setOperation(Operation operation) {
            this.operation = operation;
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

        public BatchGridCalculator build() {
            if(isSelectAll){
                variables = new HashSet<>();
                variables.addAll(DataReader.getVariables(pathToInput));
            }
            return new BatchGridCalculator(this);
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

        Set<String> sourceVariables = DataReader.getVariables(pathToInput);
        VortexGrid raster = getRaster();

        List<GridCalculatableUnit> units = new ArrayList<>();
        variables.forEach(variable -> {
            if (sourceVariables.contains(variable)) {

                DataReader reader = DataReader.builder()
                        .path(pathToInput)
                        .variable(variable)
                        .build();

                GridCalculatableUnit unit = GridCalculatableUnit.builder()
                        .reader(reader)
                        .raster(raster)
                        .operation(operation)
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
        String timeMessage = "Batch grid calculator time: " + stopwatch;
        logger.info(timeMessage);

        String templateEnd = MessageStore.getInstance().getMessage("calculator_end");
        String messageEnd = String.format(templateEnd, processed, destination);
        support.firePropertyChange(VortexProperty.COMPLETE.toString(), null, messageEnd);

        String templateTime = MessageStore.getInstance().getMessage("calculator_time");
        String messageTime = String.format(templateTime, stopwatch);
        support.firePropertyChange(VortexProperty.STATUS.toString(), null, messageTime);
    }

    private VortexGrid getRaster() {
        if (variables.isEmpty())
            return null;

        String variable = variables.iterator().next();

        DataReader inputReader = DataReader.builder()
                .path(pathToInput)
                .variable(variable)
                .build();

        VortexGrid input = (VortexGrid) inputReader.getDto(0);
        double minX = input.originX();
        double minY = Math.min(input.originY(), input.terminusY());
        double maxX = input.getTerminusX();
        double maxY = Math.max(input.originY(), input.terminusY());

        Envelope envelope = new Envelope(minX, maxX, minY, maxY);

        DataReader rasterReader = DataReader.builder()
                .path(pathToRaster)
                .build();

        VortexGrid raster = (VortexGrid) rasterReader.getDto(0);

        return Resampler.builder()
                .grid(raster)
                .envelope(envelope)
                .envelopeWkt(input.wkt())
                .targetWkt(input.wkt())
                .method(resamplingMethod)
                .build()
                .resample();
    }

    public void addPropertyChangeListener(PropertyChangeListener pcl) {
        this.support.addPropertyChangeListener(pcl);
    }

    public void removePropertyChangeListener(PropertyChangeListener pcl) {
        this.support.removePropertyChangeListener(pcl);
    }
}
