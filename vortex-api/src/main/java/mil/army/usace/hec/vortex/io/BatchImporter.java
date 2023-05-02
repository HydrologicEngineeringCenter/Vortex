package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.Options;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public abstract class BatchImporter {
    private static final Logger logger = Logger.getLogger(BatchImporter.class.getName());
    private final List<String> inFiles;
    private final List<String> variables;
    final Path destination;
    final Map<String, String> geoOptions;
    final Map<String, String> writeOptions;

    private final PropertyChangeSupport support;
    private final AtomicInteger doneCount;

    BatchImporter(Builder builder){
        this.inFiles = builder.inFiles;
        this.variables = builder.variables;
        this.destination = builder.destination;
        this.geoOptions = builder.geoOptions;
        this.writeOptions = builder.writeOptions;
        this.support = new PropertyChangeSupport(this);
        this.doneCount = new AtomicInteger();
    }

    public static class Builder {
        private List<String> inFiles;
        private List<String> variables;
        private Path destination;
        private final Map<String, String> geoOptions = new HashMap<>();
        private final Map<String, String> writeOptions = new HashMap<>();

        public Builder inFiles(final List<String> inFiles){
            this.inFiles = inFiles;
            return this;
        }

        public Builder variables(final List<String> variables){
            this.variables = variables;
            return this;
        }

        public Builder destination(final String destination){
            this.destination = Paths.get(destination);
            return this;
        }

        /**
         * @deprecated since 0.10.16, replaced by {@link #geoOptions}
         * @param geoOptions  the geographic options
         * @return the builder
         */
        @Deprecated
        public Builder geoOptions(final Options geoOptions){
            Optional.ofNullable(geoOptions).ifPresent(o -> this.geoOptions.putAll(o.getOptions()));
            return this;
        }

        public Builder geoOptions(final Map<String, String> geoOptions){
            this.geoOptions.putAll(geoOptions);
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

        public BatchImporter build(){
            if (destination == null){
                throw new IllegalStateException("Invalid destination.");
            }

            PathMatcher dssMatcher = FileSystems.getDefault().getPathMatcher("glob:**/*.dss");
            if (dssMatcher.matches(destination)) {
                return new ConcurrentBatchImporter(this);
            }

            PathMatcher ncMatcher = FileSystems.getDefault().getPathMatcher("glob:**/*.nc");
            if (ncMatcher.matches(destination)) {
                return new SerialBatchImporter(this);
            }

            throw new IllegalStateException("Invalid destination: " + destination);
        }
    }

    public static Builder builder() {return new Builder();}

    public void process() {
        Instant start = Instant.now();

        processWrite();

        long seconds = Duration.between(start, Instant.now()).toSeconds();
        String timeMessage = String.format("Batch import time: %d:%02d:%02d%n", seconds / 3600, (seconds % 3600) / 60, (seconds % 60));
        logger.info(timeMessage);
    }

    abstract void processWrite();

    List<DataReader> getDataReaders() {
        List<DataReader> readers = new ArrayList<>();

        inFiles.forEach(file -> {
            if (DataReader.isVariableRequired(file)) {
                variables.forEach(variable -> {
                    if (DataReader.getVariables(file).contains(variable)) {
                        DataReader reader = DataReader.builder()
                                .path(file)
                                .variable(variable)
                                .build();

                        readers.add(reader);
                    }
                });
            } else {
                DataReader reader = DataReader.builder()
                        .path(file)
                        .build();

                readers.add(reader);
            }
        });

        return readers;
    }

    PropertyChangeListener writeProgressListener(int totalCount) {
        return evt -> {
            if(evt.getPropertyName().equals("complete")) {
                int newValue = (int) (((float) doneCount.incrementAndGet() / totalCount) * 100);
                support.firePropertyChange("progress", null, newValue);
            }
        };
    }

    public void addPropertyChangeListener(PropertyChangeListener pcl) {
        this.support.addPropertyChangeListener(pcl);
    }

    public void removePropertyChangeListener(PropertyChangeListener pcl) {
        this.support.removePropertyChangeListener(pcl);
    }
}



