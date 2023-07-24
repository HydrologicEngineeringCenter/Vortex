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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class GridToPointConverter {

    private final String pathToGrids;
    private final Set<String> variables;
    private final Path pathToZoneDataset;
    private final String field;
    private final Path destination;
    private final Map<String, String> writeOptions;
    private final PropertyChangeSupport support;

    private GridToPointConverter(GridToPointConverterBuilder builder){
        this.pathToGrids = builder.pathToGrids;
        this.variables = builder.variables;
        this.pathToZoneDataset = builder.pathToFeatures;
        this.field = builder.field;
        this.destination = builder.destination;
        this.writeOptions = builder.writeOptions;
        this.support = new PropertyChangeSupport(this);
    }

    public static class GridToPointConverterBuilder {
        private String pathToGrids;
        private Set<String> variables;
        private Path pathToFeatures;
        private String field;
        private Path destination;
        private final Map<String, String> writeOptions = new HashMap<>();

        public GridToPointConverterBuilder pathToGrids (final String pathToGrids){
            this.pathToGrids = pathToGrids;
            return this;
        }

        public GridToPointConverterBuilder variables(final Set<String> variables){
            this.variables = variables;
            return this;
        }

        /**
         * @deprecated since 0.10.20, replaced by {@link #pathToFeatures}
         * @param pathToFeatures  the path to features file
         * @return the builder
         */
        @Deprecated
        public GridToPointConverterBuilder pathToFeatures (final Path pathToFeatures){
            this.pathToFeatures = pathToFeatures;
            return this;
        }

        public GridToPointConverterBuilder pathToFeatures (final String pathToFeatures){
            this.pathToFeatures = Paths.get(pathToFeatures);
            return this;
        }

        public GridToPointConverterBuilder field (final String field){
            this.field = field;
            return this;
        }

        public GridToPointConverterBuilder destination (final String destination){
            this.destination = Paths.get(destination);
            return this;
        }

        /**
         * @deprecated since 0.10.20, replaced by {@link #destination}
         * @param destination  the destinationDep
         * @return the builder
         */
        @Deprecated
        public GridToPointConverterBuilder destination (final Path destination){
            this.destination = destination;
            return this;
        }

        /**
         * @deprecated since 0.10.16, replaced by {@link #writeOptions}
         * @param writeOptions  the file write options
         * @return the builder
         */
        @Deprecated
        public GridToPointConverterBuilder writeOptions (final Options writeOptions){
            Optional.ofNullable(writeOptions).ifPresent(o -> this.writeOptions.putAll(o.getOptions()));
            return this;
        }

        public GridToPointConverterBuilder writeOptions (final Map<String, String> writeOptions){
            this.writeOptions.putAll(writeOptions);
            return this;
        }

        public GridToPointConverter build(){
            return new GridToPointConverter(this);
        }
    }

    public static GridToPointConverterBuilder builder() {return new GridToPointConverterBuilder();}

    public void convert() {
        VortexGrid grid0 = (VortexGrid) DataReader.builder()
                .path(pathToGrids)
                .variable(variables.iterator().next())
                .build()
                .getDto(0);

        AtomicReference<VortexGrid> reference = new AtomicReference<>();
        reference.set(grid0);

        Map<String, Integer[]> zoneMasks = ZonalStatisticsCalculator.createZoneMasks(pathToZoneDataset, field, grid0);

        List<VortexData> points = new ArrayList<>();

        int totalCount = variables.size();
        AtomicInteger doneCount = new AtomicInteger();

        variables.forEach(variable -> {
            DataReader reader = DataReader.builder()
                    .path(pathToGrids)
                    .variable(variable)
                    .build();

            List<VortexGrid> grids = reader.getDtos().stream()
                    .map(VortexGrid.class::cast)
                    .toList();

            for (VortexGrid grid : grids) {
                if (!ReferenceUtils.compareSpatiallyEquivalent(grid, reference.get())) {
                    reference.set(grid);
                    zoneMasks.clear();
                    zoneMasks.putAll(ZonalStatisticsCalculator.createZoneMasks(pathToZoneDataset, field, grid));
                }

                List<ZonalStatistics> zonalStatistics = ZonalStatisticsCalculator.builder()
                        .grid(grid)
                        .zoneMasks(zoneMasks)
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
            }
            int newValue = (int) (((float) doneCount.incrementAndGet() / totalCount) * 100);
            support.firePropertyChange("progress", null, newValue);
        });

        DataWriter writer = DataWriter.builder()
                .destination(destination)
                .data(points)
                .options(writeOptions)
                .build();

        writer.write();
    }

    public void addPropertyChangeListener(PropertyChangeListener pcl) {
        this.support.addPropertyChangeListener(pcl);
    }

    public void removePropertyChangeListener(PropertyChangeListener pcl) {
        this.support.removePropertyChangeListener(pcl);
    }
}
