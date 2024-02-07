package mil.army.usace.hec.vortex.math;

import mil.army.usace.hec.vortex.Options;
import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.io.DataReader;
import mil.army.usace.hec.vortex.io.DataWriter;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Shifter {
    private static final Logger logger = Logger.getLogger(Shifter.class.getName());

    private final String pathToFile;
    private final Set<String> variables;
    private final Set<TimeShiftMethod> methods;
    private final Duration shift;
    private final Path destination;
    private final Map<String, String> options;
    private final PropertyChangeSupport support;

    private Shifter(Builder builder) {
        this.pathToFile = builder.pathToFile;
        this.variables = builder.grids;
        this.methods = builder.methods;
        this.shift = builder.shift;
        this.destination = builder.destination;
        this.options = builder.options;
        this.support = new PropertyChangeSupport(this);

        logger.setLevel(Level.INFO);
        builder.handlers.forEach(logger::addHandler);
    }

    public static class Builder {
        private String pathToFile;
        private Set<String> grids;
        private final Set<TimeShiftMethod> methods = new HashSet<>(List.of(TimeShiftMethod.START, TimeShiftMethod.END));
        private Duration shift;
        private Path destination;
        private Map<String, String> options = new HashMap<>();
        private final List<Handler> handlers = new ArrayList<>();

        public Builder pathToFile(final String pathToFile) {
            this.pathToFile = pathToFile;
            return this;
        }

        public Builder grids(final Set<String> grids) {
            this.grids = grids;
            return this;
        }

        public Builder methods(final Set<TimeShiftMethod> methods) {
            this.methods.clear();
            this.methods.addAll(methods);
            return this;
        }

        public Builder shift(final Duration shift) {
            this.shift = shift;
            return this;
        }

        public Builder destination(final String destination) {
            this.destination = Paths.get(destination);
            return this;
        }

        /**
         * @param writeOptions the file write options
         * @return the builder
         * @deprecated since 0.10.16, replaced by {@link #writeOptions}
         */
        @Deprecated
        public Builder writeOptions(final Options writeOptions) {
            Optional.ofNullable(writeOptions).ifPresent(o -> this.options.putAll(o.getOptions()));
            return this;
        }

        public Builder writeOptions(final Map<String, String> options) {
            this.options = options;
            return this;
        }

        public Builder handlers(final List<Handler> handlers) {
            this.handlers.addAll(handlers);
            return this;
        }

        public Shifter build() {
            if (methods.isEmpty())
                throw new IllegalStateException("Methods must not be empty");

            return new Shifter(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public void shift() {

        logger.info(() -> "Time-shift started...");

        AtomicInteger processed = new AtomicInteger();
        int total = variables.size();

        variables.forEach(variable -> {
            DataReader reader = DataReader.builder()
                    .path(pathToFile)
                    .variable(variable)
                    .build();

            int count = reader.getDtoCount();

            for (int i = 0; i < count; i++) {
                VortexGrid grid = (VortexGrid) reader.getDto(i);

                List<VortexData> shiftedGrids = new ArrayList<>();
                shiftedGrids.add(shift(grid, methods, shift));

                DataWriter writer = DataWriter.builder()
                        .data(shiftedGrids)
                        .destination(destination)
                        .options(options)
                        .build();

                writer.write();
            }

            int newValue = (int) (((float) processed.incrementAndGet() / total) * 100);
            support.firePropertyChange("progress", null, newValue);
        });
    }

    public static VortexGrid shift(VortexGrid dto, Set<TimeShiftMethod> methods, Duration shift) {
        ZonedDateTime shiftedStart;
        if (dto.startTime() != null) {
            ZonedDateTime start = dto.startTime();
            if (methods.contains(TimeShiftMethod.START)) {
                shiftedStart = start.plus(shift);
            } else {
                shiftedStart = start;
            }
        } else {
            shiftedStart = null;
        }

        ZonedDateTime shiftedEnd;
        if (dto.endTime() != null) {
            ZonedDateTime end = dto.endTime();
            if (methods.contains(TimeShiftMethod.END)) {
                shiftedEnd = end.plus(shift);
            } else {
                shiftedEnd = end;
            }
        } else {
            shiftedEnd = null;
        }

        Duration interval;
        if (shiftedStart != null && shiftedEnd != null) {
            interval = Duration.between(shiftedStart, shiftedEnd);
        } else {
            interval = dto.interval();
        }

        return VortexGrid.toBuilder(dto)
                .startTime(shiftedStart)
                .endTime(shiftedEnd)
                .interval(interval)
                .build();
    }

    public void addPropertyChangeListener(PropertyChangeListener pcl) {
        this.support.addPropertyChangeListener(pcl);
    }

    public void removePropertyChangeListener(PropertyChangeListener pcl) {
        this.support.removePropertyChangeListener(pcl);
    }
}
