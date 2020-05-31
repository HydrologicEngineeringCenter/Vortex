package mil.army.usace.hec.vortex.math;

import mil.army.usace.hec.vortex.Options;
import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.io.DataReader;
import mil.army.usace.hec.vortex.io.DataWriter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SanitizableUnit {
    private final DataReader reader;
    private final double minimumThreshold;
    private final double maximumThreshold;
    private final float minimumReplacementValue;
    private final float maximumReplacementValue;
    private final Path destination;
    private final Options writeOptions;

    private SanitizableUnit(Builder builder) {
        reader = builder.reader;
        minimumThreshold = builder.minimumThreshold;
        maximumThreshold = builder.maximumThreshold;
        minimumReplacementValue = builder.minimumReplacementValue;
        maximumReplacementValue = builder.maximumReplacementValue;
        destination = builder.destination;
        writeOptions = builder.writeOptions;
    }

    public static class Builder {
        private DataReader reader;
        private double minimumThreshold = -Double.MAX_VALUE;
        private double maximumThreshold = Double.MAX_VALUE;
        private float minimumReplacementValue = Float.NaN;
        private float maximumReplacementValue = Float.NaN;
        private Path destination;
        private Options writeOptions;

        public Builder reader(DataReader reader) {
            this.reader = reader;
            return this;
        }

        public Builder minimumThreshold(double minimumThreshold) {
            this.minimumThreshold = minimumThreshold;
            return this;
        }

        public Builder maximumThreshold(double maximumThreshold) {
            this.maximumThreshold = maximumThreshold;
            return this;
        }

        public Builder minimumReplacementValue(float minimumReplacementValue) {
            this.minimumReplacementValue = minimumReplacementValue;
            return this;
        }

        public Builder maximumReplacementValue(float maximumReplacementValue) {
            this.maximumReplacementValue = maximumReplacementValue;
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

        public SanitizableUnit build() {
            if (reader == null) {
                throw new IllegalArgumentException("DataReader must be provided");
            }
            if (destination == null) {
                throw new IllegalArgumentException("Destination must be provided");
            }
            return new SanitizableUnit(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public void process() {
        List<VortexGrid> grids = reader.getDtos().stream().map(grid -> (VortexGrid) grid).collect(Collectors.toList());

        grids.forEach(grid -> {
            Sanitizer sanitizer = Sanitizer.builder()
                    .inputGrid(grid)
                    .maximumThreshold(maximumThreshold)
                    .maximumReplacementValue(maximumReplacementValue)
                    .minimumThreshold(minimumThreshold)
                    .minimumReplacementValue(minimumReplacementValue)
                    .build();

            VortexGrid sanitized = sanitizer.sanitize();

            List<VortexData> data = new ArrayList<>();
            data.add(sanitized);

            DataWriter writer = DataWriter.builder()
                    .data(data)
                    .destination(destination)
                    .options(writeOptions)
                    .build();

            writer.write();
        });
    }
}
