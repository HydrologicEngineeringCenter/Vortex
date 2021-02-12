package mil.army.usace.hec.vortex.geo;

import mil.army.usace.hec.vortex.Options;
import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.io.DataReader;
import mil.army.usace.hec.vortex.io.DataWriter;
import mil.army.usace.hec.vortex.math.Scaler;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

class TransposableUnit {
    private final DataReader reader;
    private final double angle;
    private final Double stormCenterX;
    private final Double stormCenterY;
    private final double scaleFactor;
    private final Path destination;
    private final Map<String, String> writeOptions;

    private TransposableUnit(Builder builder){
        this.reader = builder.reader;
        this.angle = builder.angle;
        this.stormCenterX = builder.stormCenterX;
        this.stormCenterY = builder.stormCenterY;
        this.scaleFactor = builder.scaleFactor;
        this.destination = builder.destination;
        this.writeOptions = builder.writeOptions;
    }

    public static class Builder {
        private DataReader reader;
        private Double angle;
        private Double stormCenterX;
        private Double stormCenterY;
        private Double scaleFactor;
        private Path destination;
        private final Map<String, String> writeOptions = new HashMap<>();

        public Builder reader(DataReader reader){
            this.reader = reader;
            return this;
        }

        public Builder angle (double angle){
            this.angle = angle;
            return this;
        }

        public Builder stormCenterX (Double stormCenterX){
            this.stormCenterX = stormCenterX;
            return this;
        }

        public Builder stormCenterY (Double stormCenterY){
            this.stormCenterY = stormCenterY;
            return this;
        }

        public Builder scaleFactor (Double scaleFactor){
            this.scaleFactor = scaleFactor;
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
        public Builder writeOptions(final Options writeOptions){
            Optional.ofNullable(writeOptions).ifPresent(o -> this.writeOptions.putAll(o.getOptions()));
            return this;
        }

        public Builder writeOptions(Map<String, String> writeOptions){
            this.writeOptions.putAll(writeOptions);
            return this;
        }

        public TransposableUnit build(){
            if (reader == null){
                throw new IllegalArgumentException("TransposableUnit requires DataReader");
            }
            if (destination == null){
                throw new IllegalArgumentException("TransposableUnit requires destination");
            }
            if (angle == null || angle.isNaN()){
                angle = 0.0;
            }
            if (scaleFactor == null || scaleFactor.isNaN()){
                scaleFactor = 1.0;
            }
            return new TransposableUnit(this);
        }
    }

    public static Builder builder() {return new Builder();}

    public void process(){
        List<VortexGrid> grids = reader.getDtos().stream().map(grid -> (VortexGrid)grid).collect(Collectors.toList());

        grids.forEach(grid -> {
            VortexGrid transposed;
            if (angle == 0 && (stormCenterX == null || stormCenterX.isNaN())
                    && (stormCenterY == null || stormCenterY.isNaN())){
                transposed = grid;
            } else {
                transposed = Transposer.builder()
                        .grid(grid)
                        .angle(angle)
                        .stormCenterX(stormCenterX)
                        .stormCenterY(stormCenterY)
                        .build()
                        .transpose();
            }

            VortexGrid scaled;
            if (scaleFactor == 1.0) {
                scaled = transposed;
            } else {
                scaled = Scaler.builder()
                        .grid(transposed)
                        .scaleFactor(scaleFactor)
                        .build()
                        .scale();
            }

            List<VortexData> data = new ArrayList<>();
            data.add(scaled);

            DataWriter writer = DataWriter.builder()
                    .data(data)
                    .destination(destination)
                    .options(writeOptions)
                    .build();

            writer.write();
        });
    }
}
