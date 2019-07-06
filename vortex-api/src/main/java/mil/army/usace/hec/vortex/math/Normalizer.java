package mil.army.usace.hec.vortex.math;

import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.io.DataReader;
import mil.army.usace.hec.vortex.io.DataWriter;
import mil.army.usace.hec.vortex.Options;

import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class Normalizer {

    private Path pathToSource;
    private Path pathToNormals;
    private Set<String> sourceVariables;
    private Set<String> normalsVariables;
    private ZonedDateTime startTime;
    private ZonedDateTime endTime;
    private Duration interval;
    private Path destination;
    private Options writeOptions;

    private Normalizer (NormalizerBuilder builder){
        this.pathToSource = builder.pathToSource;
        this.pathToNormals = builder.pathToNormals;
        this.sourceVariables = builder.sourceVariables;
        this.normalsVariables = builder.normalsVariables;
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
        this.interval = builder.interval;
        this.destination = builder.destination;
        this.writeOptions = builder.writeOptions;
    }

    public static class NormalizerBuilder{
        private Path pathToSource;
        private Path pathToNormals;
        private Set<String> sourceVariables;
        private Set<String> normalsVariables;
        ZonedDateTime startTime;
        ZonedDateTime endTime;
        Duration interval;
        private Path destination;
        Options writeOptions;

        public NormalizerBuilder pathToSource (final Path pathToSource){
            this.pathToSource = pathToSource;
            return this;
        }

        public NormalizerBuilder pathToNormals (final Path pathToNormals){
            this.pathToNormals = pathToNormals;
            return this;
        }

        public NormalizerBuilder sourceVariables(final Set<String> sourceVariables){
            this.sourceVariables = sourceVariables;
            return this;
        }

        public NormalizerBuilder normalsVariables(final Set<String> normalsVariables){
            this.normalsVariables = normalsVariables;
            return this;
        }

        public NormalizerBuilder startTime (final ZonedDateTime startTime){
            this.startTime = startTime;
            return this;
        }

        public NormalizerBuilder endTime (final ZonedDateTime endTime){
            this.endTime = endTime;
            return this;
        }

        public NormalizerBuilder interval (final Duration interval){
            this.interval = interval;
            return this;
        }

        public NormalizerBuilder destination (final Path destination){
            this.destination = destination;
            return this;
        }

        public NormalizerBuilder writeOptions (final Options writeOptions){
            this.writeOptions = writeOptions;
            return this;
        }

        public Normalizer build(){
            return new Normalizer(this);
        }
    }

    public static NormalizerBuilder builder() {return new NormalizerBuilder();}

    public void normalize(){
        List<VortexData> source = new ArrayList<>();
        sourceVariables.forEach(variable -> source.addAll(
                DataReader.builder()
                        .path(pathToSource)
                        .variable(variable)
                        .build()
                        .getDTOs()));

        List<VortexData> normals = new ArrayList<>();
        normalsVariables.forEach(variable -> normals.addAll(
                DataReader.builder()
                        .path(pathToNormals)
                        .variable(variable)
                        .build()
                        .getDTOs()));

        AtomicReference<ZonedDateTime> intervalStart = new AtomicReference<>();
        intervalStart.set(startTime);

        while (intervalStart.get().isBefore(endTime)){
            ZonedDateTime start = intervalStart.get();
            ZonedDateTime end = intervalStart.get().plus(interval);

            List<VortexGrid> output = normalize(

                    source.stream().filter(grid -> ((grid.startTime().isEqual(start) || grid.startTime().isAfter(start))
                            && (grid.endTime().isEqual(end) || grid.endTime().isBefore(end))))
                            .map(grid -> (VortexGrid)grid)
                            .collect(Collectors.toList()),

                    normals.stream().filter(grid -> ((grid.startTime().equals(start) || grid.startTime().isAfter(start))
                            && (grid.endTime().isEqual(end) || grid.endTime().isBefore(end))))
                            .map(grid -> (VortexGrid)grid)
                            .collect(Collectors.toList()));

            output.forEach(grid -> {
                List<VortexData> data = new ArrayList<>();
                data.add(grid);

                DataWriter writer = DataWriter.builder()
                        .data(data)
                        .destination(destination)
                        .options(writeOptions)
                        .build();

                writer.write();
            });

            intervalStart.set(end);
        }
    }

    static List<VortexGrid> normalize(List<VortexGrid> source, List<VortexGrid> normals) {
        boolean valid = validate(source, normals);
        if(!valid){
            return Collections.emptyList();
        }

        int length = source.get(0).data().length;

        float[] sourceTotals = new float[length];
        source.forEach(dto -> {
            float[] data = dto.data();
            for (int i = 0; i < length; i++) {
                if (!Float.isNaN(data[i])) {
                    sourceTotals[i] += data[i];
                } else {
                    sourceTotals[i] += 0;
                }
            }
        });

        float[] normalTotals = new float[length];
        normals.forEach(dto -> {
            float[] data = dto.data();
            for (int i = 0; i < length; i++) {
                normalTotals[i] += data[i];
            }
        });

        List<VortexGrid> output = new ArrayList<>();
        source.forEach(dto -> {
            float[] inData = dto.data();
            float[] outData = new float[length];
            for (int i = 0; i < length; i++) {
                float value;
                if (sourceTotals[i] == 0) {
                    value = 0;
                } else if (Float.isNaN(normalTotals[i])) {
                    value = inData[i];
                } else if (Float.isNaN(inData[i])){
                    value = Float.NaN;
                } else {
                    value = normalTotals[i] / sourceTotals[i] * inData[i];
                }
                outData[i] = value;
            }
            output.add(VortexGrid.builder()
                    .dx(dto.dx()).dy(dto.dy()).nx(dto.nx()).ny(dto.ny())
                    .originX(dto.originX()).originY(dto.originY())
                    .wkt(dto.wkt()).data(outData).units(dto.units())
                    .fileName(dto.fileName()).shortName(dto.shortName())
                    .fullName(dto.shortName()).description(dto.description())
                    .startTime(dto.startTime()).endTime(dto.endTime())
                    .interval(dto.interval()).build()
            );
        });

        return output;
    }

    private static boolean validate(List<VortexGrid> source, List<VortexGrid> normals){
        VortexGrid reference = source.get(0);
        int nx = reference.nx();
        int ny = reference.ny();
        double dx = reference.dx();
        double dy = reference.dy();

        AtomicBoolean valid = new AtomicBoolean();
        valid.set(true);

        for (VortexGrid grid : source) {
            boolean gridValid = true;
            if(grid.nx() != nx) {
                gridValid = false;
            }
            if(grid.ny() != ny) {
                gridValid = false;
            }
            if(Double.compare(grid.dx(), dx) != 0) {
                gridValid = false;
            }
            if(Double.compare(grid.dy(), dy) != 0) {
                gridValid = false;
            }
            if(!gridValid){
                logValidationFailure("Source", reference, grid);
                valid.set(false);
            }
        }

        for (VortexGrid grid : normals) {
            boolean gridValid = true;
            if(grid.nx() != nx) {
                gridValid = false;
            }
            if(grid.ny() != ny) {
                gridValid = false;
            }
            if(Double.compare(grid.dx(), dx) != 0) {
                gridValid = false;
            }
            if(Double.compare(grid.dy(), dy) != 0) {
                gridValid = false;
            }
            if(!gridValid){
                logValidationFailure("Normals", reference, grid);
                valid.set(false);
            }
        }
        return valid.get();
    }

    private static void logValidationFailure(String origin, VortexGrid reference, VortexGrid invalid){
        String pattern = "yyyy-MM-dd HH:mm";
        SimpleDateFormat formatter = new SimpleDateFormat(pattern);
        System.out.println(origin + " grid starting: " + formatter.format(invalid.startTime()) + " and ending: "
                + formatter.format(invalid.endTime()) + " has nx: " + invalid.nx() + " ny: " + invalid.ny()
                + " dx: " + invalid.dx() + " dy: " + invalid.dy() + " compared to \n"
                + " reference grid starting " + formatter.format(reference.startTime()) + " and ending: "
                + formatter.format(reference.endTime()) + " has nx: " + reference.nx() + " ny: " + reference.ny()
                + " dx: " + reference.dx() + " dy: " + reference.dy() + "\n"
                + "Review file: " + invalid.fileName() + ", variable: " + invalid.fullName()
        );
    }
}
