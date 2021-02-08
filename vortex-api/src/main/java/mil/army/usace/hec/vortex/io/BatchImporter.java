package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.Options;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class BatchImporter {
    private List<String> inFiles;
    private List<String> variables;
    private Path destination;
    private Options geoOptions;
    private Options writeOptions;

    private PropertyChangeSupport support;
    private final AtomicInteger doneCount;

    private BatchImporter(BatchImporterBuilder builder){
        this.inFiles = builder.inFiles;
        this.variables = builder.variables;
        this.destination = builder.destination;
        this.geoOptions = builder.geoOptions;
        this.writeOptions = builder.writeOptions;
        this.support = new PropertyChangeSupport(this);
        this.doneCount = new AtomicInteger();
    }

    public static class BatchImporterBuilder {
        private List<String> inFiles;
        private List<String> variables;
        private Path destination;
        private Options geoOptions;
        private Options writeOptions;

        public BatchImporterBuilder inFiles(final List<String> inFiles){
            this.inFiles = inFiles;
            return this;
        }

        public BatchImporterBuilder variables(final List<String> variables){
            this.variables = variables;
            return this;
        }

        public BatchImporterBuilder destination(final String destination){
            this.destination = Paths.get(destination);
            return this;
        }

        public BatchImporterBuilder geoOptions(final Options geoOptions){
            this.geoOptions = geoOptions;
            return this;
        }

        public BatchImporterBuilder writeOptions(final Options writeOptions){
            this.writeOptions = writeOptions;
            return this;
        }

        public BatchImporter build(){
            return new BatchImporter(this);
        }
    }

    public static BatchImporterBuilder builder() {return new BatchImporterBuilder();}

    public void process() {

        List<ImportableUnit> importableUnits = new ArrayList<>();
        inFiles.forEach(file -> variables.forEach(variable -> {
            if (DataReader.getVariables(file).contains(variable)) {

                DataReader reader = DataReader.builder()
                        .path(file)
                        .variable(variable)
                        .build();

                ImportableUnit importableUnit = ImportableUnit.builder()
                        .reader(reader)
                        .geoOptions(geoOptions)
                        .destination(destination)
                        .writeOptions(writeOptions)
                        .build();

                importableUnits.add(importableUnit);
            }
        }));

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
    }

    public void addPropertyChangeListener(PropertyChangeListener pcl) {
        this.support.addPropertyChangeListener(pcl);
    }

    public void removePropertyChangeListener(PropertyChangeListener pcl) {
        this.support.removePropertyChangeListener(pcl);
    }
}



