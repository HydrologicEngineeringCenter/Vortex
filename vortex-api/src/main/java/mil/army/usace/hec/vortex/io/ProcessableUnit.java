package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.Options;
import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.geo.Resampler;
import mil.army.usace.hec.vortex.geo.VectorUtils;

import java.awt.geom.Rectangle2D;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ProcessableUnit {

    private DataReader reader;
    private Map<String,String> geoOptions;
    private Path destination;
    private Options writeOptions;

    private ProcessableUnit (ProcessableUnitBuilder builder){
        this.reader = builder.reader;
        this.geoOptions = builder.geoOptions;
        this.destination = builder.destination;
        this.writeOptions = builder.writeOptions;
    }

    public static class ProcessableUnitBuilder{
        private DataReader reader;
        private Map<String,String> geoOptions;
        private Path destination;
        private Options writeOptions;

        public ProcessableUnitBuilder reader(DataReader reader){
            this.reader = reader;
            return this;
        }

        public ProcessableUnitBuilder geoOptions(Options geoOptions){
            if (geoOptions == null) {
                this.geoOptions = Options.create().getOptions();
            } else {
                this.geoOptions = geoOptions.getOptions();
            }
            return this;
        }

        public ProcessableUnitBuilder destination(Path destination){
            this.destination = destination;
            return this;
        }

        public ProcessableUnitBuilder writeOptions(Options writeOptions){
            this.writeOptions = writeOptions;
            return this;
        }

        public ProcessableUnit build(){
            if (reader == null){
                System.out.println("ProcessableUnit requires DataReader");
            }
            if (destination == null){
                System.out.println("ProcessableUnit requires destination");
            }
            return new ProcessableUnit(this);
        }
    }

    public static ProcessableUnitBuilder builder() {return new ProcessableUnitBuilder();}

    public void process(){

        Rectangle2D env = null;
        String envWkt = null;
        if (geoOptions.containsKey("pathToShp")){
            Path pathToShp = Paths.get(geoOptions.get("pathToShp"));
            if(Files.exists(pathToShp)) {
                env = VectorUtils.getEnvelope(pathToShp);
                envWkt = VectorUtils.getWkt(pathToShp);
            }
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
