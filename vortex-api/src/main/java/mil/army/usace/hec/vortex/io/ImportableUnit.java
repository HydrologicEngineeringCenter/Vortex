package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.Options;
import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.geo.Resampler;
import mil.army.usace.hec.vortex.geo.VectorUtils;

import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
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

    public void process(){

        Rectangle2D env;
        String envWkt;
        if (geoOptions.containsKey("pathToShp") && new File(geoOptions.get("pathToShp")).exists()) {
            env = VectorUtils.getEnvelope(Paths.get(geoOptions.get("pathToShp")));
            envWkt = VectorUtils.getWkt(Paths.get(geoOptions.get("pathToShp")));
        } else {
            env = null;
            envWkt = null;
        }

        String method = geoOptions.getOrDefault("resamplingMethod", "near");

        int count = reader.getDtoCount();

        IntStream.range(0, count).parallel().forEach(i -> {

            VortexGrid grid = (VortexGrid) reader.getDto(i);

            String destWkt;

            if (geoOptions.containsKey("targetWkt")) {
                destWkt = geoOptions.get("targetWkt");
            } else {
                destWkt = grid.wkt();
            }

            double cellSize;
            if (geoOptions.containsKey("targetCellSize")) {
                cellSize = Double.parseDouble(geoOptions.get("targetCellSize"));
            } else {
                cellSize = Double.NaN;
            }

            VortexGrid processed = Resampler.builder()
                    .grid(grid)
                    .envelope(env)
                    .envelopeWkt(envWkt)
                    .targetWkt(destWkt)
                    .cellSize(cellSize)
                    .method(method)
                    .build()
                    .resample();

            List<VortexData> data = new ArrayList<>();
            data.add(processed);

            DataWriter writer = DataWriter.builder()
                    .data(data)
                    .destination(destination)
                    .options(writeOptions)
                    .build();

            writer.write();
        });

        support.firePropertyChange("complete", null, null);
    }

    public void addPropertyChangeListener(PropertyChangeListener pcl) {
        this.support.addPropertyChangeListener(pcl);
    }

    public void removePropertyChangeListener(PropertyChangeListener pcl) {
        this.support.removePropertyChangeListener(pcl);
    }

}
