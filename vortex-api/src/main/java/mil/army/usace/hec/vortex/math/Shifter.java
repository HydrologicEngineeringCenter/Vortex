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
    private final Duration shiftStart;
    private final Duration shiftEnd;
    private final Path destination;
    private final Map<String, String> options;
    private final PropertyChangeSupport support;

    private Shifter(Builder builder) {
        this.pathToFile = builder.pathToFile;
        this.variables = builder.grids;
        this.shiftStart = builder.shiftStart;
        this.shiftEnd = builder.shiftEnd;
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
        private Duration shift = Duration.ZERO;
        private Duration shiftStart = Duration.ZERO;
        private Duration shiftEnd = Duration.ZERO;
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

        /**
         * @param methods the time shift methods
         * @return the builder
         * @deprecated since 0.11.16, replaced by {@link #shiftStart} and {@link #shiftEnd}
         */
        @Deprecated
        public Builder methods(final Set<TimeShiftMethod> methods) {
            this.methods.clear();
            this.methods.addAll(methods);
            return this;
        }

        /**
         * @param shift the shift duration
         * @return the builder
         * @deprecated since 0.11.16, replaced by {@link #shiftStart} and {@link #shiftEnd}
         */
        @Deprecated
        public Builder shift(final Duration shift) {
            logger.info(() -> "shift has been deprecated as of v0.11.16, use shiftStart and shiftEnd");
            this.shift = shift;
            return this;
        }

        public Builder shiftStart(final Duration shiftStart) {
            this.shiftStart = shiftStart;
            return this;
        }

        public Builder shiftEnd(final Duration shiftEnd) {
            this.shiftEnd = shiftEnd;
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

            if (!shift.equals(Duration.ZERO) && !shiftStart.equals(Duration.ZERO)) {
                logger.info(() -> "Specified shift of " + shift + " will override shiftStart of " + shiftStart);
            }

            if (!shift.equals(Duration.ZERO) && !shiftEnd.equals(Duration.ZERO)) {
                logger.info(() -> "Specified shift of " + shift + " will override shiftEnd of " + shiftEnd);
            }

            if (!shift.equals(Duration.ZERO) && methods.contains(TimeShiftMethod.START)) {
                shiftStart = shift;
            }

            if (!shift.equals(Duration.ZERO) && methods.contains(TimeShiftMethod.END)) {
                shiftEnd = shift;
            }

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
            try (DataReader reader = DataReader.builder()
                    .path(pathToFile)
                    .variable(variable)
                    .build()) {

                int count = reader.getDtoCount();

                for (int i = 0; i < count; i++) {
                    VortexGrid grid = (VortexGrid) reader.getDto(i);

                    List<VortexData> shiftedGrids = new ArrayList<>();
                    shiftedGrids.add(shift(grid, shiftStart, shiftEnd));

                    DataWriter writer = DataWriter.builder()
                            .data(shiftedGrids)
                            .destination(destination)
                            .options(options)
                            .build();

                    writer.write();
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, e, e::getMessage);
            }

            int newValue = (int) (((float) processed.incrementAndGet() / total) * 100);
            support.firePropertyChange("progress", null, newValue);
        });
    }

    static VortexGrid shift(VortexGrid dto, Duration shiftStart, Duration shiftEnd) {
        ZonedDateTime shiftedStart;
        if (dto.startTime() != null) {
            ZonedDateTime start = dto.startTime();
            shiftedStart = start.plus(shiftStart);
        } else {
            shiftedStart = null;
        }

        ZonedDateTime shiftedEnd;
        if (dto.endTime() != null) {
            ZonedDateTime end = dto.endTime();
            shiftedEnd = end.plus(shiftEnd);
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
