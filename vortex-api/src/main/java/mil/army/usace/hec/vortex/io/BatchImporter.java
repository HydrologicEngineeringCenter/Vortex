package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.Options;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class BatchImporter {
    private List<Path> inFiles;
    private List<String> variables;
    private Path destination;
    private Options geoOptions;
    private Options writeOptions;

    private BatchImporter(BatchImporterBuilder builder){
        this.inFiles = builder.inFiles;
        this.variables = builder.variables;
        this.destination = builder.destination;
        this.geoOptions = builder.geoOptions;
        this.writeOptions = builder.writeOptions;
    }

    public static class BatchImporterBuilder {
        private List<Path> inFiles;
        private List<String> variables;
        private Path destination;
        private Options geoOptions;
        private Options writeOptions;

        public BatchImporterBuilder inFiles(final List<Path> inFiles){
            this.inFiles = inFiles;
            return this;
        }

        public BatchImporterBuilder variables(final List<String> variables){
            this.variables = variables;
            return this;
        }

        public BatchImporterBuilder destination(final Path destination){
            this.destination = destination;
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
        importableUnits.parallelStream().forEach(ImportableUnit::process);
    }
}



