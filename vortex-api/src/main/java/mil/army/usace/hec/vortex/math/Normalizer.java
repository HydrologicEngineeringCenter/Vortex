package mil.army.usace.hec.vortex.math;

import mil.army.usace.hec.vortex.Options;
import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.io.DataReader;
import mil.army.usace.hec.vortex.io.DataWriter;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class Normalizer {

    private static Logger logger = Logger.getLogger(Normalizer.class.getName());
    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm").withZone(ZoneId.of("UTC"));

    private String pathToSource;
    private String pathToNormals;
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

        logger.setLevel(Level.INFO);
        builder.handlers.forEach(handler -> logger.addHandler(handler));
    }

    public static class NormalizerBuilder{
        private String pathToSource;
        private String pathToNormals;
        private Set<String> sourceVariables;
        private Set<String> normalsVariables;
        ZonedDateTime startTime;
        ZonedDateTime endTime;
        Duration interval;
        private Path destination;
        Options writeOptions;
        List<Handler> handlers = new ArrayList<>();

        public NormalizerBuilder pathToSource (final String pathToSource){
            this.pathToSource = pathToSource;
            return this;
        }

        public NormalizerBuilder pathToNormals (final String pathToNormals){
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

        public NormalizerBuilder destination (final String destination){
            this.destination = Paths.get(destination);
            return this;
        }

        public NormalizerBuilder writeOptions (final Options writeOptions){
            this.writeOptions = writeOptions;
            return this;
        }

        public NormalizerBuilder handlers (final List<Handler> handlers){
            this.handlers.addAll(handlers);
            return this;
        }

        public Normalizer build(){
            return new Normalizer(this);
        }
    }

    public static NormalizerBuilder builder() {return new NormalizerBuilder();}

    public void normalize(){

        logger.info(() -> "Normalization started...");

        List<VortexData> source = new ArrayList<>();
        sourceVariables.forEach(variable -> source.addAll(
                DataReader.builder()
                        .path(pathToSource)
                        .variable(variable)
                        .build()
                        .getDtos()));

        List<VortexData> normals = new ArrayList<>();
        normalsVariables.forEach(variable -> normals.addAll(
                DataReader.builder()
                        .path(pathToNormals)
                        .variable(variable)
                        .build()
                        .getDtos()));

        AtomicReference<ZonedDateTime> intervalStart = new AtomicReference<>();
        intervalStart.set(startTime);

        AtomicInteger count = new AtomicInteger();
        while (intervalStart.get().isBefore(endTime)){
            ZonedDateTime start = intervalStart.get();
            ZonedDateTime end = intervalStart.get().plus(interval);

            List<VortexGrid> sourceFiltered = source.stream().filter(grid -> ((grid.startTime().isEqual(start) || grid.startTime().isAfter(start))
                    && (grid.endTime().isEqual(end) || grid.endTime().isBefore(end))))
                    .map(grid -> (VortexGrid)grid)
                    .collect(Collectors.toList());

            if (sourceFiltered.isEmpty()) {
                logger.warning(() -> format("No source grids for period %s to %s", formatter.format(start), formatter.format(end)));
            }

            List<VortexGrid> normalsFiltered = normals.stream().filter(grid -> ((grid.startTime().equals(start) || grid.startTime().isAfter(start))
                    && (grid.endTime().isEqual(end) || grid.endTime().isBefore(end))))
                    .map(grid -> (VortexGrid)grid)
                    .collect(Collectors.toList());

            if (normalsFiltered.isEmpty()){
                logger.warning(() -> format("No normals grids for period %s to %s", formatter.format(start), formatter.format(end)));
            }

            List<VortexGrid> output = normalize(sourceFiltered, normalsFiltered);

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
            count.addAndGet(output.size());
        }
        logger.info(() -> format("Normalization complete: %s grids normalized.", count.get()));
    }

    static List<VortexGrid> normalize(List<VortexGrid> source, List<VortexGrid> normals) {
        if(source.isEmpty() || normals.isEmpty()){
            return Collections.emptyList();
        }

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
                    .fullName(dto.fullName()).description(dto.description())
                    .startTime(dto.startTime()).endTime(dto.endTime())
                    .interval(dto.interval()).build()
            );
        });

        return output;
    }

    private static boolean validate(List<VortexGrid> source, List<VortexGrid> normals){
        if(source.isEmpty()){
            throw new IllegalStateException("Must provide more than one source grid.");
        }

        if(normals.isEmpty()){
            throw new IllegalStateException("Must provide more then one normals grid.");
        }

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
        logger.severe(() -> origin + " grid starting: " + formatter.format(invalid.startTime()) + " and ending: "
                + formatter.format(invalid.endTime()) + " has nx: " + invalid.nx() + " ny: " + invalid.ny()
                + " dx: " + invalid.dx() + " dy: " + invalid.dy() + "."
                + " Reference grid starting " + formatter.format(reference.startTime()) + " and ending "
                + formatter.format(reference.endTime()) + " has nx: " + reference.nx() + " ny: " + reference.ny()
                + " dx: " + reference.dx() + " dy: " + reference.dy() + "."
                + " Review file: " + invalid.fileName() + ", variable: " + invalid.fullName()
        );
    }
}
