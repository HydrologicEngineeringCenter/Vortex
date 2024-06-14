package mil.army.usace.hec.vortex.geo;

import mil.army.usace.hec.vortex.VortexGrid;
import org.gdal.gdal.*;
import org.gdal.gdalconst.gdalconst;
import org.gdal.ogr.*;
import org.gdal.ogr.Driver;
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;
import org.gdal.osr.osr;

import java.awt.geom.Point2D;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

public class ZonalStatisticsCalculator {
    private final VortexGrid grid;
    private final Map<String, Integer[]> zoneMasks;

    private ZonalStatisticsCalculator(ZonalStatisticsCalculatorBuilder builder) {
        this.grid = builder.grid;
        this.zoneMasks = builder.zoneMasks;
    }

    public static class ZonalStatisticsCalculatorBuilder {
        private VortexGrid grid;
        private Map<String, Integer[]> zoneMasks;

        public ZonalStatisticsCalculatorBuilder grid(VortexGrid grid) {
            this.grid = grid;
            return this;
        }

        public ZonalStatisticsCalculatorBuilder zoneMasks(Map<String, Integer[]> zoneMasks) {
            this.zoneMasks = zoneMasks;
            return this;
        }

        public ZonalStatisticsCalculator build() {
            return new ZonalStatisticsCalculator(this);
        }
    }

    public static ZonalStatisticsCalculatorBuilder builder() {
        return new ZonalStatisticsCalculatorBuilder();
    }

    public List<ZonalStatistics> getZonalStatistics() {
        float[] data = grid.data();
        List<ZonalStatistics> zoneStatistics = new ArrayList<>();
        zoneMasks.forEach((id, mask) -> {
            ZonalStatistics zonalStatistics = computeZonalStatistics(id, mask, data);
            zoneStatistics.add(zonalStatistics);
        });
        return zoneStatistics;
    }

    public static Map<String, Integer[]> createZoneMasks(Path pathToZoneDataset, String field, VortexGrid grid) {
        Dataset raster = RasterUtils.getDatasetFromVortexGrid(grid);

        double[] geoTransform = raster.GetGeoTransform();
        double dx = geoTransform[1];
        double dy = geoTransform[5];
        double originX = geoTransform[0];
        double originY = geoTransform[3];
        int nx = raster.GetRasterXSize();
        int ny = raster.GetRasterYSize();

        double terminusX = originX + dx * nx;
        double terminusY = originY + dy * ny;

        double minX = Math.min(originX, terminusX);
        double maxX = Math.max(originX, terminusX);
        double minY = Math.min(originY, terminusY);
        double maxY = Math.max(originY, terminusY);

        Driver driver = ogr.GetDriverByName("Memory");
        DataSource dataSource = driver.CreateDataSource("");

        DataSource inDataSource = ogr.Open(pathToZoneDataset.toString());
        Layer inLayer = inDataSource.GetLayer(0);

        SpatialReference src = inLayer.GetSpatialRef();
        src.SetAxisMappingStrategy(osr.OAMS_TRADITIONAL_GIS_ORDER);
        SpatialReference tgt = new SpatialReference(raster.GetProjection());
        tgt.SetAxisMappingStrategy(osr.OAMS_TRADITIONAL_GIS_ORDER);
        CoordinateTransformation transformation = osr.CreateCoordinateTransformation(src, tgt);

        AtomicReference<List<GridCell>> gridCells = new AtomicReference<>();

        Map<String, List<Long>> idFidsMap = new HashMap<>();
        long count = inLayer.GetFeatureCount();
        for (int i = 0; i < count; i++) {
            Feature inFeature = inLayer.GetFeature(i);
            String id = inFeature.GetFieldAsString(field);
            long fid = inFeature.GetFID();

            idFidsMap.computeIfAbsent(id, fids -> new ArrayList<>());

            idFidsMap.get(id).add(fid);
        }

        Map<String, Integer[]> zoneMasks = new HashMap<>();

        for (Map.Entry<String, List<Long>> entry : idFidsMap.entrySet()) {

            List<Long> fids = entry.getValue();
            if (fids.isEmpty())
                continue;

            Geometry geometry = new Geometry(ogrConstants.wkbGeometryCollection);

            for (Long fid : fids) {
                Feature inFeature = inLayer.GetFeature(fid);
                geometry.AddGeometry(inFeature.GetGeometryRef());
                inFeature.delete();
            }

            geometry.Transform(transformation);

            String id = entry.getKey();
            Layer layer = dataSource.CreateLayer(id, tgt, geometry.GetGeometryType());

            Feature feature = new Feature(layer.GetLayerDefn());
            feature.SetGeometry(geometry);

            layer.CreateFeature(feature);

            ArrayList<String> options = new ArrayList<>();
            options.add("-of");
            options.add("MEM");
            options.add("-te");
            options.add(Double.toString(minX));
            options.add(Double.toString(minY));
            options.add(Double.toString(maxX));
            options.add(Double.toString(maxY));
            options.add("-tr");
            options.add(Double.toString(dx));
            options.add(Double.toString(dy));

            Dataset target = gdal.GetDriverByName("MEM").Create("", nx, ny, 1, gdalconst.GDT_Byte);
            target.SetGeoTransform(geoTransform);
            target.SetProjection(raster.GetProjection());

            int[] bands = {1};
            double[] burnValues = {1};
            gdal.RasterizeLayer(target, bands, layer, burnValues, new Vector<>(options));

            Band mask = target.GetRasterBand(1);
            int[] maskData = new int[nx * ny];
            mask.ReadRaster(0, 0, nx, ny, maskData);

            Integer[] maskArray = IntStream.of(maskData).boxed().toArray(Integer[]::new);

            int nonZeroCellCount = (int) Arrays.stream(maskArray).filter(val -> val > 0).count();
            if (nonZeroCellCount > 0) {
                zoneMasks.put(id, maskArray);
            } else {
                Set<Integer> intersectionIndices = new HashSet<>();
                if (gridCells.get() == null) {
                    gridCells.set(Grid.builder()
                            .originX(originX)
                            .originY(originY)
                            .dy(dy)
                            .dx(dx)
                            .nx(nx)
                            .ny(ny)
                            .build()
                            .getGridCells());
                }
                double[] extent = layer.GetExtent();
                double centroidX = ((extent[0] + extent[1]) / 2);
                double centroidY = ((extent[2] + extent[3]) / 2);
                Point2D point = new Point2D.Double(centroidX, centroidY);
                gridCells.get().stream()
                        .filter(cell -> cell.contains(point))
                        .findAny()
                        .ifPresent(cell -> intersectionIndices.add(cell.getIndex()));
                Integer[] intersectionMaskArray = new Integer[nx * ny];
                Arrays.fill(intersectionMaskArray, 0);
                intersectionIndices.forEach(index -> intersectionMaskArray[index] = 1);
                zoneMasks.put(id, intersectionMaskArray);
            }

            geometry.delete();
            layer.delete();
            feature.delete();
            target.delete();
            mask.delete();
        }

        raster.delete();
        driver.delete();
        dataSource.delete();
        inDataSource.delete();
        inLayer.delete();
        src.delete();
        tgt.delete();
        transformation.delete();

        return zoneMasks;
    }

    private ZonalStatistics computeZonalStatistics(String id, Integer[] mask, float[] data) {

        double noDataValue = grid.noDataValue();

        List<Double> validValues = new ArrayList<>();

        // filter for masked and non no data values
        for (int i = 0; i < data.length; i++) {
            double value = data[i];
            if (mask[i] == 1 && Double.compare(value, noDataValue) != 0) {
                validValues.add(value);
            }
        }

        Collections.sort(validValues);

        double sum = validValues.stream().mapToDouble(f -> f).sum();
        int count = validValues.size();
        double average = Double.NaN;
        double min = Double.NaN;
        double max = Double.NaN;
        double median = Double.NaN;
        double firstQuartile = Double.NaN;
        double thirdQuartile = Double.NaN;
        float pctCellsGreaterThanZero = Float.NaN;
        float pctCellsGreaterThanFirstQuartile = Float.NaN;
        int numCellsGreaterThanZero = 0;
        int numCellsGreaterThanFirstQuartile = 0;

        if (count > 0) {

            average = sum / count;
            min = validValues.get(0);
            max = validValues.get(count - 1);

            for (double value : validValues) {
                if (value > 0) {
                    numCellsGreaterThanZero++;
                }
            }

            pctCellsGreaterThanZero = (float) (100 * numCellsGreaterThanZero) / count;

        }

        // can't compute median with less than 2 values
        if (count >= 2) {
            median = computeMedian(validValues);
        }

        // if there are less than 4 valid values, don't compute additional statistics
        if (count >= 4) {

            firstQuartile = validValues.get((count + 1) / 4);
            thirdQuartile = validValues.get(3 * (count + 1) / 4);

            for (double value : validValues) {
                if (value > firstQuartile) {
                    numCellsGreaterThanFirstQuartile++;
                }
            }
            pctCellsGreaterThanFirstQuartile = (float) (100 * numCellsGreaterThanFirstQuartile) / count;

        }

        return ZonalStatistics.builder()
                .id(id)
                .average(average)
                .min(min)
                .max(max)
                .median(median)
                .firstQuartile(firstQuartile)
                .thirdQuartile(thirdQuartile)
                .pctCellsGreaterThanZero(pctCellsGreaterThanZero)
                .pctCellsGreaterThanFirstQuartile(pctCellsGreaterThanFirstQuartile)
                .build();
    }

    static double computeMedian(List<Double> values) {
        int size = values.size();
        if (values.size() % 2 == 0)
            return (values.get(size / 2) + (values.get(size / 2 - 1))) / 2;
        else
            return values.get(size / 2);
    }

}



