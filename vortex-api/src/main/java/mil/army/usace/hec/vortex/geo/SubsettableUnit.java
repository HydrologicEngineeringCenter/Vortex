package mil.army.usace.hec.vortex.geo;

import mil.army.usace.hec.vortex.Options;
import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.io.DataReader;
import mil.army.usace.hec.vortex.io.DataWriter;
import org.locationtech.jts.geom.Envelope;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SubsettableUnit {
    private final DataReader reader;
    private final Envelope envelope;
    private final String envelopeWkt;
    private final Path destination;
    private final Options writeOptions;

    private SubsettableUnit(Builder builder) {
        reader = builder.reader;
        envelope = builder.envelope;
        envelopeWkt = builder.envelopeWkt;
        destination = builder.destination;
        writeOptions = builder.writeOptions;
    }

    public static class Builder {
        private DataReader reader;
        private Envelope envelope;
        private String envelopeWkt;
        private Path destination;
        private Options writeOptions;

        public Builder reader(DataReader reader) {
            this.reader = reader;
            return this;
        }

        public Builder setEnvelope(Envelope envelope) {
            this.envelope = envelope;
            return this;
        }

        public Builder setEnvelopeWkt(String envelopeWkt) {
            this.envelopeWkt = envelopeWkt;
            return this;
        }

        public Builder destination(Path destination) {
            this.destination = destination;
            return this;
        }

        public Builder writeOptions(Options writeOptions) {
            this.writeOptions = writeOptions;
            return this;
        }

        public SubsettableUnit build() {
            if (reader == null) {
                throw new IllegalArgumentException("DataReader must be provided");
            }
            if (envelope == null) {
                throw new IllegalArgumentException("Subsetting envelope must be provided");
            }
            if (destination == null) {
                throw new IllegalArgumentException("Destination must be provided");
            }
            return new SubsettableUnit(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public void process() {
        List<VortexGrid> grids = reader.getDtos().stream().map(grid -> (VortexGrid) grid).collect(Collectors.toList());

        grids.forEach(grid -> {
            String gridWkt = grid.wkt();
            Reprojector reprojector = Reprojector.builder()
                    .from(envelopeWkt)
                    .to(gridWkt)
                    .build();

            Envelope reprojected = reprojector.reproject(envelope);

            Subsetter subsetter = Subsetter.builder()
                    .setGrid(grid)
                    .setEnvelope(reprojected)
                    .build();

            VortexGrid subset = subsetter.subset();

            List<VortexData> data = new ArrayList<>();
            data.add(subset);

            DataWriter writer = DataWriter.builder()
                    .data(data)
                    .destination(destination)
                    .options(writeOptions)
                    .build();

            writer.write();
        });
    }
}
