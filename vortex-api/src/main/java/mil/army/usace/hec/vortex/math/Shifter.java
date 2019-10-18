package mil.army.usace.hec.vortex.math;

import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.io.DataReader;
import mil.army.usace.hec.vortex.io.DataWriter;
import mil.army.usace.hec.vortex.Options;

import java.nio.file.Path;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class Shifter {

    private String pathToFile;
    private Set<String> grids;
    private Duration shift;
    private Path destination;
    private Options options;

    private Shifter (ShifterBuilder builder){
        this.pathToFile = builder.pathToFile;
        this.grids = builder.grids;
        this.shift = builder.shift;
        this.destination = builder.destination;
        this.options = builder.options;
    }

    public static class ShifterBuilder{
        private String pathToFile;
        private Set<String> grids;
        Duration shift;
        private Path destination;
        private Options options;

        public ShifterBuilder pathToFile (final String pathToFile){
            this.pathToFile = pathToFile;
            return this;
        }

        public ShifterBuilder grids (final Set<String> grids){
            this.grids = grids;
            return this;
        }

        public ShifterBuilder shift (final Duration shift){
            this.shift = shift;
            return this;
        }

        public ShifterBuilder destination (final Path destination){
            this.destination = destination;
            return this;
        }

        public ShifterBuilder writeOptions(final Options options){
            this.options = options;
            return this;
        }

        public Shifter build(){
            return new Shifter(this);
        }
    }

    public static ShifterBuilder builder(){return new ShifterBuilder();}

    public void shift(){
        List<VortexGrid> targets = new ArrayList<>();
        grids.forEach(grid -> targets.addAll(
                DataReader.builder()
                        .path(pathToFile)
                        .variable(grid)
                        .build()
                        .getDtos()
                        .stream()
                        .map(data -> (VortexGrid) data)
                        .collect(Collectors.toList())));

        List<VortexGrid> output = new ArrayList<>();
        targets.parallelStream().forEach(dto -> output.add(shift(dto, shift)));

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
