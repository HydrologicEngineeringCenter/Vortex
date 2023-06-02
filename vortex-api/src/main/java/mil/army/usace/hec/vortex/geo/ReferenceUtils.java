package mil.army.usace.hec.vortex.geo;

import hec.heclib.grid.AlbersInfo;
import hec.heclib.grid.GridInfo;
import mil.army.usace.hec.vortex.GdalRegister;
import mil.army.usace.hec.vortex.VortexGrid;
import org.gdal.osr.SpatialReference;

import java.util.concurrent.atomic.AtomicBoolean;

public class ReferenceUtils {
    static {
        GdalRegister.getInstance();
    }

    private ReferenceUtils(){}

    public static String getMapUnits(String crs) {
        SpatialReference srs = new SpatialReference();

        srs.ImportFromWkt(crs);
        srs.MorphFromESRI();

        String linearUnitsName = srs.GetLinearUnitsName();

        srs.delete();

        return linearUnitsName;
    }

    public static int getUlyDirection(String wkt, double llx, double lly){
        //Return 1 until there is a use case the necessitates returning -1. This function is only called by DssDataReader.
//        SpatialReference source = new SpatialReference(wkt);
//        source.MorphFromESRI();
//        if (source.IsProjected() == 1){
//            double latitudeOfOrigin = source.GetProjParm("latitude_of_origin");
//            SpatialReference dest = new SpatialReference();
//            dest.SetWellKnownGeogCS( "WGS84" );
//            CoordinateTransformation transform = CoordinateTransformation.CreateCoordinateTransformation(source, dest);
//            double latitudeOfLly = transform.TransformPoint(llx, lly)[1];
//            double falseNorthing = source.GetProjParm("false_northing");
//            double llyLessFalseNorthing = lly - falseNorthing;
//            if (latitudeOfLly < latitudeOfOrigin && Math.signum(llyLessFalseNorthing) > 0){
//                return -1;
//            }
//            return 1;
//        }
        return 1;
    }

    public static boolean compareSpatiallyEquivalent(VortexGrid grid1, VortexGrid grid2){
        AtomicBoolean valid = new AtomicBoolean();
        valid.set(true);

        if (grid1.nx() != grid2.nx()){
            valid.set(false);
        }
        if (grid1.ny() != grid2.ny()){
            valid.set(false);
        }
        if (grid1.dx() != grid2.dx()){
            valid.set(false);
        }
        if (grid1.dy() != grid2.dy()){
            valid.set(false);
        }

        if (!equals(grid1.wkt(), grid2.wkt())) {
            valid.set(false);
        }

        return valid.get();
    }

    public static boolean equals(String wkt1, String wkt2) {
        SpatialReference sr1 = new SpatialReference(wkt1);
        SpatialReference sr2 = new SpatialReference(wkt2);

        boolean isSame = sr1.IsSame(sr2) != 1;

        sr1.delete();
        sr2.delete();

        return isSame;
    }

    public static boolean isShg(GridInfo info){
        if(info.getGridType() != 420){
            return false;
        }
        AlbersInfo albersInfo = (AlbersInfo) info;
        if(albersInfo.getFirstStandardParallel() != 29.5){
            return false;
        }
        if(albersInfo.getSecondStandardParallel() != 45.5){
            return false;
        }
        if(albersInfo.getLatitudeOfProjectionOrigin() != 23.0){
            return false;
        }
        if(albersInfo.getCentralMeridian() != -96.0){
            return false;
        }
        if(albersInfo.getFalseEasting() != 0){
            return false;
        }
        if(albersInfo.getFalseNorthing() != 0){
            return false;
        }
        if(albersInfo.getProjectionDatum() != GridInfo.getNad83()){
            return false;
        }
        String units = albersInfo.getProjectionUnits().toLowerCase();
        return units.equals("m") || units.equals("meter") || units.equals("meters") || units.equals("metre");
    }

    public static boolean isGeographic(String wkt) {
        SpatialReference srs = new SpatialReference(wkt);
        boolean isGeographic = srs.IsGeographic() == 1;
        srs.delete();
        return isGeographic;
    }
}
