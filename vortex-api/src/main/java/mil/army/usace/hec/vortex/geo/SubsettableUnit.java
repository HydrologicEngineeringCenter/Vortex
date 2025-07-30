package mil.army.usace.hec.vortex.geo;

import mil.army.usace.hec.vortex.Options;
import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.io.DataReader;
import mil.army.usace.hec.vortex.io.DataWriter;
import org.locationtech.jts.geom.Envelope;

import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SubsettableUnit {
    private static final Logger logger = Logger.getLogger(SubsettableUnit.class.getName());

    private final DataReader reader;
    private final Envelope envelope;
    private final String envelopeWkt;
    private final Path destination;
    private final Map<String, String> writeOptions;

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
        private final Map<String, String> writeOptions = new HashMap<>();

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
        List<VortexGrid> grids = reader.getDtos().stream().map(grid -> (VortexGrid) grid).toList();

        for (VortexGrid grid : grids) {
            try {
                String gridWkt = grid.wkt();

                if (ReferenceUtils.isGeographic(gridWkt)) {
                    logger.log(Level.SEVERE, String.format("Grid \"%s\" uses geographic coordinate system. Can not clip.", grid.fullName()));
                    return;
                }

                ResamplingMethod resamplingMethod = ResamplingMethod.NEAREST_NEIGHBOR;

                Resampler resampler = Resampler.builder()
                        .grid(grid)
                        .envelope(envelope)
                        .envelopeWkt(envelopeWkt)
                        .method(resamplingMethod)
                        .build();

                VortexGrid resampled = resampler.resample();

                List<VortexData> data = new ArrayList<>();
                data.add(resampled);

                DataWriter writer = DataWriter.builder()
                        .data(data)
                        .destination(destination)
                        .options(writeOptions)
                        .build();

                writer.write();
            } catch (Exception e) {
                logger.log(Level.SEVERE, e, e::getMessage);
            }
        }
    }
}
