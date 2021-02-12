package mil.army.usace.hec.vortex.math;

import mil.army.usace.hec.vortex.Options;
import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.io.DataReader;
import mil.army.usace.hec.vortex.io.DataWriter;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;

public class Shifter {

    private final String pathToFile;
    private final Set<String> grids;
    private final Duration shift;
    private final Path destination;
    private final Map<String, String> options;

    private Shifter (Builder builder){
        this.pathToFile = builder.pathToFile;
        this.grids = builder.grids;
        this.shift = builder.shift;
        this.destination = builder.destination;
        this.options = builder.options;
    }

    public static class Builder {
        private String pathToFile;
        private Set<String> grids;
        Duration shift;
        private Path destination;
        private Map<String, String> options = new HashMap<>();

        public Builder pathToFile (final String pathToFile){
            this.pathToFile = pathToFile;
            return this;
        }

        public Builder grids (final Set<String> grids){
            this.grids = grids;
            return this;
        }

        public Builder shift (final Duration shift){
            this.shift = shift;
            return this;
        }

        public Builder destination (final String destination){
            this.destination = Paths.get(destination);
            return this;
        }

        /**
         * @deprecated since 0.10.16, replaced by {@link #writeOptions}
         * @param writeOptions  the file write options
         * @return the builder
         */
        @Deprecated
        public Builder writeOptions(final Options writeOptions){
            Optional.ofNullable(writeOptions).ifPresent(o -> this.options.putAll(o.getOptions()));
            return this;
        }

        public Builder writeOptions(final Map<String, String> options){
            this.options = options;
            return this;
        }

        public Shifter build(){
            return new Shifter(this);
        }
    }

    public static Builder builder(){return new Builder();}

    public void shift(){
        List<VortexData> targets = new ArrayList<>();
        grids.forEach(grid -> targets.addAll(
                DataReader.builder()
                        .path(pathToFile)
                        .variable(grid)
                        .build()
                        .getDtos()));

        List<VortexGrid> output = new ArrayList<>();
        targets.forEach(grid -> output.add(shift((VortexGrid) grid, shift)));

        output.parallelStream().forEach(dto ->{
            List<VortexData> data = new ArrayList<>();
            data.add(dto);

            DataWriter writer = DataWriter.builder()
                    .data(data)
                    .destination(destination)
                    .options(options)
                    .build();

            writer.write();
        });
    }

    public static VortexGrid shift(VortexGrid dto, Duration shift) {
        ZonedDateTime shiftedStart;
        if (dto.startTime() != null) {
            ZonedDateTime start = dto.startTime();
            shiftedStart = start.plus(shift);
        } else {
            shiftedStart = null;
        }

        ZonedDateTime shiftedEnd;
        if (dto.endTime() != null) {
            ZonedDateTime end = dto.endTime();
            shiftedEnd = end.plus(shift);
        } else {
            shiftedEnd = null;
        }

        return VortexGrid.builder()
                .dx(dto.dx()).dy(dto.dy()).nx(dto.nx()).ny(dto.ny())
                .originX(dto.originX()).originY(dto.originY())
                .wkt(dto.wkt()).data(dto.data()).units(dto.units())
                .fileName(dto.fileName()).shortName(dto.shortName())
                .fullName(dto.shortName()).description(dto.description())
                .startTime(shiftedStart).endTime(shiftedEnd)
                .interval(dto.interval()).build();
    }
}
