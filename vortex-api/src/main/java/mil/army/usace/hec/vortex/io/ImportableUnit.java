package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.Options;
import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.geo.Resampler;
import mil.army.usace.hec.vortex.geo.VectorUtils;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ImportableUnit {

    private DataReader reader;
    private Map<String,String> geoOptions;
    private Path destination;
    private Options writeOptions;

    private ImportableUnit(ImportableUnitBuilder builder){
        this.reader = builder.reader;
        this.geoOptions = builder.geoOptions;
        this.destination = builder.destination;
        this.writeOptions = builder.writeOptions;
    }

    public static class ImportableUnitBuilder {
        private DataReader reader;
        private Map<String,String> geoOptions;
        private Path destination;
        private Options writeOptions;

        public ImportableUnitBuilder reader(DataReader reader){
            this.reader = reader;
            return this;
        }

        public ImportableUnitBuilder geoOptions(Options geoOptions){
            if (geoOptions == null) {
                this.geoOptions = Options.create().getOptions();
            } else {
                this.geoOptions = geoOptions.getOptions();
            }
            return this;
        }

        public ImportableUnitBuilder destination(Path destination){
            this.destination = destination;
            return this;
        }

        public ImportableUnitBuilder writeOptions(Options writeOptions){
            this.writeOptions = writeOptions;
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

    public static ImportableUnitBuilder builder() {return new ImportableUnitBuilder();}

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

        List<VortexGrid> grids = reader.getDTOs().stream().map(grid -> (VortexGrid)grid).collect(Collectors.toList());

        for (VortexGrid grid : grids){

            String destWkt;
            double cellSize;

            if (geoOptions.containsKey("targetWkt")) {
                destWkt = geoOptions.get("targetWkt");
            } else {
                destWkt = grid.wkt();
            }

            if (geoOptions.containsKey("targetCellSize")) {
                cellSize = Double.parseDouble(geoOptions.get("targetCellSize"));
            } else {
                cellSize = ((Math.abs(grid.dx()) + Math.abs(grid.dy())) / 2.0);
            }

            VortexGrid processed = Resampler.builder()
                    .grid(grid)
                    .envelope(env)
                    .envelopeWkt(envWkt)
                    .targetWkt(destWkt)
                    .cellSize(cellSize)
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
        }
    }

}
