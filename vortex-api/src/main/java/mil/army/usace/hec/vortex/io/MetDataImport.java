package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.Options;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class MetDataImport {
    private List<Path> inFiles;
    private List<String> variables;
    private Path destination;
    private Options geoOptions;
    private Options writeOptions;

    private MetDataImport(MetDataImportBuilder builder){
        this.inFiles = builder.inFiles;
        this.variables = builder.variables;
        this.destination = builder.destination;
        this.geoOptions = builder.geoOptions;
        this.writeOptions = builder.writeOptions;
    }

    public static class MetDataImportBuilder{
        private List<Path> inFiles;
        private List<String> variables;
        private Path destination;
        private Options geoOptions;
        private Options writeOptions;

        public MetDataImportBuilder inFiles(final List<Path> inFiles){
            this.inFiles = inFiles;
            return this;
        }

        public MetDataImportBuilder variables(final List<String> variables){
            this.variables = variables;
            return this;
        }

        public MetDataImportBuilder destination(final Path destination){
            this.destination = destination;
            return this;
        }

        public MetDataImportBuilder geoOptions(final Options geoOptions){
            this.geoOptions = geoOptions;
            return this;
        }

        public MetDataImportBuilder writeOptions(final Options writeOptions){
            this.writeOptions = writeOptions;
            return this;
        }

        public MetDataImport build(){
            return new MetDataImport(this);
        }
    }

    public static MetDataImportBuilder builder() {return new MetDataImportBuilder();}

    public void process() {

        List<ProcessableUnit> processableUnits = new ArrayList<>();
        inFiles.forEach(file -> variables.forEach(variable -> {
            if (DataReader.getVariables(file).contains(variable)) {

                DataReader reader = DataReader.builder()
                        .path(file)
                        .variable(variable)
                        .build();

                ProcessableUnit processableUnit = ProcessableUnit.builder()
                        .reader(reader)
                        .geoOptions(geoOptions)
                        .destination(destination)
                        .writeOptions(writeOptions)
                        .build();

                processableUnits.add(processableUnit);
            }
        }));
        processableUnits.parallelStream().forEach(ProcessableUnit::process);
    }
}



