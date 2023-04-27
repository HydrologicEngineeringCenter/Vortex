package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.Options;
import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.geo.GeographicProcessor;

import javax.swing.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ImportableUnit {

    private final DataReader reader;
    private final Map<String,String> geoOptions;
    private final Path destination;
    private final Map<String, String> writeOptions;

    private final PropertyChangeSupport support;

    private ImportableUnit(Builder builder) {
        this.reader = builder.reader;
        this.geoOptions = builder.geoOptions;
        this.destination = builder.destination;
        this.writeOptions = builder.writeOptions;
        this.support = new PropertyChangeSupport(this);
    }

    public static class Builder {
        private DataReader reader;
        private final Map<String,String> geoOptions = new HashMap<>();
        private Path destination;
        private final Map<String, String> writeOptions = new HashMap<>();

        public Builder reader(DataReader reader){
            this.reader = reader;
            return this;
        }

        /**
         * @deprecated since 0.10.16, replaced by {@link #geoOptions}
         * @param geoOptions  the geographic options
         * @return the builder
         */
        @Deprecated
        public Builder geoOptions(Options geoOptions){
            Optional.ofNullable(geoOptions).ifPresent(o -> this.geoOptions.putAll(o.getOptions()));
            return this;
        }

        public Builder geoOptions(Map<String, String> geoOptions){
            this.geoOptions.putAll(geoOptions);
            return this;
        }

        public Builder destination(Path destination){
            this.destination = destination;
            return this;
        }

        /**
         * @deprecated since 0.10.16, replaced by {@link #writeOptions}
         * @param writeOptions  the file write options
         * @return the builder
         */
        @Deprecated
        public Builder writeOptions(Options writeOptions){
            Optional.ofNullable(writeOptions).ifPresent(o -> this.writeOptions.putAll(o.getOptions()));
            return this;
        }

        public Builder writeOptions(Map<String, String> writeOptions){
            this.writeOptions.putAll(writeOptions);
            return this;
        }

        public ImportableUnit build(){
            if (reader == null){
                System.out.println("ProcessableUnit requires DataReader");
            }
            if (destination == null){
                System.out.println("ProcessableUnit requires destination");
            }
            return new ImportableUnit(this);
        }
    }

    public static Builder builder() {return new Builder();}

    public void process() {
        String fileName = destination.getFileName().toString();
        if (fileName.endsWith(".dss"))
            processDssWrite();
        if (fileName.endsWith(".nc"))
            processNetcdfWrite();
        support.firePropertyChange("complete", null, null);
    }

    private void processDssWrite() {
        GeographicProcessor geoProcessor = new GeographicProcessor(geoOptions);
        int count = reader.getDtoCount();
        IntStream.range(0, count).parallel().forEach(i -> {
            VortexGrid grid = (VortexGrid) reader.getDto(i);
            VortexGrid processed = geoProcessor.process(grid);

            List<VortexData> data = new ArrayList<>();
            data.add(processed);

            DataWriter writer = DataWriter.builder()
                    .data(data)
                    .destination(destination)
                    .options(writeOptions)
                    .build();

            writer.write();
        });
    }

    private void processNetcdfWrite() {
        GeographicProcessor geoProcessor = new GeographicProcessor(geoOptions);
        List<VortexData> processedGrids = reader.getDtos().stream().parallel()
                .filter(VortexGrid.class::isInstance)
                .map(grid -> geoProcessor.process((VortexGrid) grid))
                .collect(Collectors.toList());

        DataWriter writer = DataWriter.builder()
                .data(processedGrids)
                .destination(destination)
                .options(writeOptions)
                .build();

        writer.write();
    }

    public void addPropertyChangeListener(PropertyChangeListener pcl) {
        this.support.addPropertyChangeListener(pcl);
    }

    public void removePropertyChangeListener(PropertyChangeListener pcl) {
        this.support.removePropertyChangeListener(pcl);
    }

}
