package mil.army.usace.hec.vortex.geo;

import org.gdal.ogr.Geometry;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;

import java.util.logging.Level;
import java.util.logging.Logger;

public class GeometryConverter {

    private static final Logger logger = Logger.getLogger(GeometryConverter.class.getName());

    private GeometryConverter(){}

    public static Geometry convertToOgrGeometry(org.locationtech.jts.geom.Geometry jtsGeometry){
        Coordinate[] coordinates = jtsGeometry.getCoordinates();
        boolean is3d = false;
        for (Coordinate coordinate : coordinates){
            if (!Double.isNaN(coordinate.getZ())){
                is3d = true;
                break;
            }
        }
        int outputDimension;
        if (is3d){
            outputDimension = 3;
        } else {
            outputDimension = 2;
        }
        WKBWriter writer = new WKBWriter(outputDimension);
        return Geometry.CreateFromWkb(writer.write(jtsGeometry));
    }

    public static org.locationtech.jts.geom.Geometry convertToJtsGeometry(Geometry gdalGeometry){
        WKBReader reader = new WKBReader();
        try {
            return reader.read(gdalGeometry.ExportToWkb());
        } catch (ParseException e) {
            logger.log(Level.SEVERE, e, e::getMessage);
        }
        return null;
    }
}
