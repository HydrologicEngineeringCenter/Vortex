package mil.army.usace.hec.vortex.geo;

import mil.army.usace.hec.vortex.Options;
import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.io.DataReader;
import mil.army.usace.hec.vortex.io.DataWriter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

class TransposableUnit {
    private DataReader reader;
    private double angle;
    private Double stormCenterX;
    private Double stormCenterY;
    private Path destination;
    private Options writeOptions;

    private TransposableUnit(TransposableUnitBuilder builder){
        this.reader = builder.reader;
        this.angle = builder.angle;
        this.stormCenterX = builder.stormCenterX;
        this.stormCenterY = builder.stormCenterY;
        this.destination = builder.destination;
        this.writeOptions = builder.writeOptions;
    }

    public static class TransposableUnitBuilder {
        private DataReader reader;
        private double angle;
        private Double stormCenterX;
        private Double stormCenterY;
        private Path destination;
        private Options writeOptions;

        public TransposableUnitBuilder reader(DataReader reader){
            this.reader = reader;
            return this;
        }

        public TransposableUnitBuilder angle (double angle){
            this.angle = angle;
            return this;
        }

        public TransposableUnitBuilder stormCenterX (Double stormCenterX){
            this.stormCenterX = stormCenterX;
            return this;
        }

        public TransposableUnitBuilder stormCenterY (Double stormCenterY){
            this.stormCenterY = stormCenterY;
            return this;
        }

        public TransposableUnitBuilder destination(Path destination){
            this.destination = destination;
            return this;
        }

        public TransposableUnitBuilder writeOptions(Options writeOptions){
            this.writeOptions = writeOptions;
            return this;
        }

        public TransposableUnit build(){
            if (reader == null){
                throw new IllegalArgumentException("TransposableUnit requires DataReader");
            }
            if (destination == null){
                throw new IllegalArgumentException("TransposableUnit requires destination");
            }
            return new TransposableUnit(this);
        }
    }

    public static TransposableUnitBuilder builder() {return new TransposableUnitBuilder();}

    public void process(){
        List<VortexGrid> grids = reader.getDtos().stream().map(grid -> (VortexGrid)grid).collect(Collectors.toList());

        grids.forEach(grid -> {
            VortexGrid transposed = Transposer.builder()
                    .grid(grid)
                    .angle(angle)
                    .stormCenterX(stormCenterX)
                    .stormCenterY(stormCenterY)
                    .build()
                    .transpose();

            List<VortexData> data = new ArrayList<>();
            data.add(transposed);

            DataWriter writer = DataWriter.builder()
                    .data(data)
                    .destination(destination)
                    .options(writeOptions)
                    .build();

            writer.write();
        });
    }
}
