package mil.army.usace.hec.vortex.convert;

import mil.army.usace.hec.vortex.Options;
import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.VortexPoint;
import mil.army.usace.hec.vortex.geo.ReferenceUtils;
import mil.army.usace.hec.vortex.geo.ZonalStatistics;
import mil.army.usace.hec.vortex.geo.ZonalStatisticsCalculator;
import mil.army.usace.hec.vortex.io.DataReader;
import mil.army.usace.hec.vortex.io.DataWriter;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class GridToPointConverter {

    private String pathToGrids;
    private Set<String> variables;
    private Path pathToZoneDataset;
    private String field;
    private Path destination;
    private Options writeOptions;

    private GridToPointConverter(GridToPointConverterBuilder builder){
        this.pathToGrids = builder.pathToGrids;
        this.variables = builder.variables;
        this.pathToZoneDataset = builder.pathToFeatures;
        this.field = builder.field;
        this.destination = builder.destination;
        this.writeOptions = builder.writeOptions;
    }

    public static class GridToPointConverterBuilder {
        private String pathToGrids;
        private Set<String> variables;
        private Path pathToFeatures;
        private String field;
        private Path destination;
        private Options writeOptions;

        public GridToPointConverterBuilder pathToGrids (final String pathToGrids){
            this.pathToGrids = pathToGrids;
            return this;
        }

        public GridToPointConverterBuilder variables(final Set<String> variables){
            this.variables = variables;
            return this;
        }

        public GridToPointConverterBuilder pathToFeatures (final Path pathToFeatures){
            this.pathToFeatures = pathToFeatures;
            return this;
        }

        public GridToPointConverterBuilder field (final String field){
            this.field = field;
            return this;
        }

        public GridToPointConverterBuilder destination (final Path destination){
            this.destination = destination;
            return this;
        }

        public GridToPointConverterBuilder writeOptions (final Options writeOptions){
            this.writeOptions = writeOptions;
            return this;
        }

        public GridToPointConverter build(){
            return new GridToPointConverter(this);
        }
    }

    public static GridToPointConverterBuilder builder() {return new GridToPointConverterBuilder();}

    public void convert(){
        List<VortexGrid> grids = new ArrayList<>();
        variables.forEach(variable -> grids.addAll(
                DataReader.builder()
                        .path(pathToGrids)
                        .variable(variable)
                        .build()
                        .getDtos()
                        .stream()
                        .map(grid -> (VortexGrid)grid)
                        .collect(Collectors.toList())));

        AtomicReference<VortexGrid> maskedGrid = new AtomicReference<>();
        maskedGrid.set(grids.get(0));

        AtomicReference<Map<String, Integer[]>> zoneMasks = new AtomicReference<>();
        zoneMasks.set(ZonalStatisticsCalculator.createZoneMasks(pathToZoneDataset, field, maskedGrid.get()));

        List<VortexData> points = new ArrayList<>();

        grids.forEach(grid -> {

            if (!ReferenceUtils.compareSpatiallyEquivalent(grid, maskedGrid.get())){
                maskedGrid.set(grid);
                zoneMasks.set(ZonalStatisticsCalculator.createZoneMasks(pathToZoneDataset, field, grid));
            }

            List<ZonalStatistics> zonalStatistics = ZonalStatisticsCalculator.builder()
                    .grid(grid)
                    .zoneMasks(zoneMasks.get())
                    .build()
                    .getZonalStatistics();

            zonalStatistics.forEach(zone -> {
                VortexPoint point = VortexPoint.builder()
                        .id(zone.getId())
                        .units(grid.units())
                        .data((float) zone.getAverage())
                        .description(grid.description())
                        .startTime(grid.startTime())
                        .endTime(grid.endTime())
                        .interval(grid.interval())
                        .build();

                points.add(point);
            });
        });

        DataWriter writer = DataWriter.builder()
                .destination(destination)
                .data(points)
                .options(writeOptions)
                .build();

        writer.write();
    }
}
