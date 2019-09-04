package mil.army.usace.hec.vortex.geo;

import hec.heclib.grid.AlbersInfo;
import hec.heclib.grid.GridInfo;
import mil.army.usace.hec.vortex.GdalRegister;
import mil.army.usace.hec.vortex.VortexGrid;
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;

import java.util.concurrent.atomic.AtomicBoolean;

public class ReferenceUtils {
    static {
        GdalRegister.getInstance();
    }

    private ReferenceUtils(){}

    public static String enhanceWkt(String wkt){
        SpatialReference srs = new SpatialReference(wkt);
        srs.MorphFromESRI();
        return srs.ExportToPrettyWkt();
    }

    public static String getMapUnits(String wkt){
        SpatialReference srs = new SpatialReference(wkt);
        srs.MorphFromESRI();
        return srs.GetLinearUnitsName();
    }

    public static int getUlyDirection(String wkt, double llx, double lly){
        SpatialReference source = new SpatialReference(wkt);
        source.MorphFromESRI();
        if (source.IsProjected() == 1){
            double latitudeOfOrigin = source.GetProjParm("latitude_of_origin");
            SpatialReference dest = new SpatialReference();
            dest.SetWellKnownGeogCS( "WGS84" );
            CoordinateTransformation transform = CoordinateTransformation.CreateCoordinateTransformation(source, dest);
            double latitudeOfLly = transform.TransformPoint(llx, lly)[1];
            if (latitudeOfLly < latitudeOfOrigin && Math.signum(lly) > 0){
                return -1;
            }
            return 1;
        }
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
        SpatialReference sr1 = new SpatialReference(grid1.wkt());
        SpatialReference sr2 = new SpatialReference(grid1.wkt());

        if (sr1.IsSame(sr2) != 1){
            valid.set(false);
        }

        return valid.get();
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
}
