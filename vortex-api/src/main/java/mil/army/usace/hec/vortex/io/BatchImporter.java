package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.Options;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class BatchImporter {
    private static final Logger logger = Logger.getLogger(BatchImporter.class.getName());
    private final List<String> inFiles;
    private final List<String> variables;
    private final Path destination;
    private final Map<String, String> geoOptions;
    private final Map<String, String> writeOptions;

    private final PropertyChangeSupport support;
    private final AtomicInteger doneCount;

    private BatchImporter(Builder builder){
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
            return new BatchImporter(this);
        }
    }

    public static Builder builder() {return new Builder();}

    public void process() {
        Instant start = Instant.now();
        List<ImportableUnit> importableUnits = new ArrayList<>();

        inFiles.forEach(file -> {
            List<DataReader> readers = new ArrayList<>();
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

            readers.forEach(reader -> {
                ImportableUnit importableUnit = ImportableUnit.builder()
                        .reader(reader)
                        .geoOptions(geoOptions)
                        .destination(destination)
                        .writeOptions(writeOptions)
                        .build();

                importableUnits.add(importableUnit);
            });
        });

        int totalCount = importableUnits.size();

        importableUnits.parallelStream().forEach(importableUnit -> {
            importableUnit.addPropertyChangeListener(evt -> {
                if(evt.getPropertyName().equals("complete")) {
                    int newValue = (int) (((float) doneCount.incrementAndGet() / totalCount) * 100);
                    support.firePropertyChange("progress", null, newValue);
                }
            });

            importableUnit.process();
        });
        Duration duration = Duration.between(start, Instant.now());
        long seconds = duration.toSeconds();

        logger.info(String.format("Batch import time: %d:%02d:%02d%n", seconds / 3600, (seconds % 3600) / 60, (seconds % 60)));
    }

    public void addPropertyChangeListener(PropertyChangeListener pcl) {
        this.support.addPropertyChangeListener(pcl);
    }

    public void removePropertyChangeListener(PropertyChangeListener pcl) {
        this.support.removePropertyChangeListener(pcl);
    }
}



