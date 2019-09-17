package mil.army.usace.hec.vortex.geo;

import mil.army.usace.hec.vortex.GdalRegister;
import org.gdal.ogr.*;
import org.gdal.osr.SpatialReference;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.gdal.ogr.ogrConstants.wkbLinearRing;
import static org.gdal.ogr.ogrConstants.wkbPolygon;

public class TravelLengthGridCellsWriter {

    private List<TravelLengthGridCell> gridCells;
    private Path pathToDestination;
    private double cellSize;
    private String wkt;

    private TravelLengthGridCellsWriter(TravelLengthGridCellsWriterBuilder builder){
        this.gridCells = builder.gridCells;
        this.pathToDestination = builder.pathToDestination;
        this.cellSize = builder.cellSize;
        this.wkt = builder.wkt;
    }

    public static class TravelLengthGridCellsWriterBuilder {
        private List<TravelLengthGridCell> gridCells;
        private Path pathToDestination;
        private double cellSize;
        private String wkt;

        public TravelLengthGridCellsWriterBuilder travelLengthGridCells(List<TravelLengthGridCell> gridCells){
            this.gridCells = gridCells;
            return this;
        }

        public TravelLengthGridCellsWriterBuilder pathToDestination(Path pathToDestination){
            this.pathToDestination = pathToDestination;
            return this;
        }

        public TravelLengthGridCellsWriterBuilder cellSize(double cellSize){
            this.cellSize = cellSize;
            return this;
        }

        public TravelLengthGridCellsWriterBuilder projection (String wkt){
            this.wkt = wkt;
            return this;
        }

        public TravelLengthGridCellsWriter build(){
            if (gridCells == null){
                throw new IllegalArgumentException("List of travel length grid cells is required");
            }
            if (pathToDestination == null){
                throw new IllegalArgumentException("Path to destination is required");
            }
            String extension = getExtension(pathToDestination);
            if (extension!= null && extension.equals("shp")){
                if(cellSize <= 0){
                    throw new IllegalArgumentException("Cell size must be greater than 0");
                }
                if(wkt == null || wkt.isEmpty()){
                    throw new IllegalArgumentException("Projection WKT must be provided");
                }
            }
            return new TravelLengthGridCellsWriter(this);
        }
    }

    public static TravelLengthGridCellsWriterBuilder builder() {
        return new TravelLengthGridCellsWriterBuilder();
    }

    public void write(){

        String extension = getExtension(pathToDestination);
        if (extension == null){
            return;
        }

        if (extension.equals("mod")) {
            writeMod();
        } else if (extension.equals("shp")){
            writeShp();
        }
    }

    private void writeShp(){
        GdalRegister.getInstance();

        Driver driver = ogr.GetDriverByName("ESRI Shapefile");
        DataSource dsOut = driver.CreateDataSource(pathToDestination.toString());
        Layer lyrOut = dsOut.CreateLayer("Layer", new SpatialReference(wkt), wkbPolygon);

        FeatureDefn defn = lyrOut.GetLayerDefn();
        gridCells.forEach(gridCell -> {
            double x1 = gridCell.getIndexI() * cellSize;
            double x2 = x1 + cellSize;
            double y1 = gridCell.getIndexJ() * cellSize;
            double y2 = y1 + cellSize;
            Geometry ring = new Geometry(wkbLinearRing);
            ring.AddPoint(x1, y1);
            ring.AddPoint(x1, y2);
            ring.AddPoint(x2, y2);
            ring.AddPoint(x2, y1);
            ring.AddPoint(x1, y1);

            Geometry poly = new Geometry(wkbPolygon);
            poly.AddGeometry(ring);

            Feature feature = new Feature(defn);
            feature.SetGeometry(poly);
            lyrOut.CreateFeature(feature);
        });

        driver.delete();
        dsOut.delete();
        lyrOut.delete();
    }

    private void writeMod(){
        Set<String> subbasinIds = gridCells.stream()
                .map(TravelLengthGridCell::getSubbasin)
                .collect(Collectors.toSet());

        try (PrintWriter writer = new PrintWriter(pathToDestination.toString(), "UTF-8")) {
            writer.println("Parameter Order: Xcoord Ycoord TravelLength Area");
            writer.println("End:");
            writer.println();

            subbasinIds.forEach(subbasinId -> {
                writer.println("Subbasin: " + subbasinId);
                gridCells.stream()
                        .filter(gridCell -> gridCell.getSubbasin().equals(subbasinId))
                        .sorted(Comparator.comparing(TravelLengthGridCell::getIndexJ))
                        .sorted(Comparator.comparing(TravelLengthGridCell::getIndexI))
                        .forEach(gridCell -> writer.println(
                                "     GridCell: "
                                        + gridCell.getIndexI()
                                        + " "
                                        + gridCell.getIndexJ()
                                        + " "
                                        + gridCell.getTravelLength()
                                        + " "
                                        + gridCell.getArea()));
                writer.println("End:");
                writer.println();
            });
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private static String getExtension(Path path){
        String fileName = path.toString();
        int i = fileName.lastIndexOf('.');
        if (i >= 0) {
            return fileName.substring(i+1).toLowerCase();
        }
        return null;
    }
}
