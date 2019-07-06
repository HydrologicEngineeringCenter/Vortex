package mil.army.usace.hec.vortex.geo;

import mil.army.usace.hec.vortex.GdalRegister;
import org.gdal.osr.SpatialReference;
import org.gdal.osr.osr;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.projection.AlbersEqualArea;
import ucar.unidata.geoloc.projection.LambertConformal;
import ucar.unidata.geoloc.projection.LatLonProjection;
import ucar.unidata.geoloc.projection.Mercator;
import ucar.unidata.geoloc.projection.Orthographic;
import ucar.unidata.geoloc.projection.Sinusoidal;
import ucar.unidata.util.Parameter;

import java.util.List;
import java.util.Optional;

public class WktFactory {

    static {
        GdalRegister.getInstance();
    }

    private static final String WGS84 = "WGS84";

    private WktFactory(){}

    public static String createWkt(ProjectionImpl projection) {
        if (projection instanceof LatLonProjection) {
            LatLonProjection in = (LatLonProjection) projection;
            SpatialReference srs = new SpatialReference();
            setGcsParameters(in, srs);
            return srs.ExportToWkt();

        } else if (projection instanceof AlbersEqualArea) {
            AlbersEqualArea in = (AlbersEqualArea) projection;
            SpatialReference srs = new SpatialReference();
            srs.SetProjCS("Albers Equal Area Conic");
            setGcsParameters(in, srs);
            srs.SetACEA(
                    in.getParallelOne(),
                    in.getParallelTwo(),
                    in.getOriginLat(),
                    in.getOriginLon(),
                    in.getFalseEasting(),
                    in.getFalseNorthing()
            );
            srs.SetLinearUnits(osr.SRS_UL_METER, 1.0);
            return srs.ExportToWkt();

        } else if (projection instanceof LambertConformal) {
            LambertConformal in = (LambertConformal) projection;
            SpatialReference srs = new SpatialReference();
            srs.SetProjCS("Lambert Conformal Conic 2SP");
            setGcsParameters(in, srs);
            srs.SetLCC(
                    in.getParallelOne(),
                    in.getParallelTwo(),
                    in.getOriginLat(),
                    in.getOriginLon(),
                    in.getFalseEasting(),
                    in.getFalseNorthing()
            );
            srs.SetLinearUnits(osr.SRS_UL_METER, 1.0);
            return srs.ExportToPrettyWkt();

        } else if (projection instanceof Mercator) {
            Mercator in = (Mercator) projection;
            SpatialReference srs = new SpatialReference();
            setGcsParameters(in, srs);
            srs.SetMercator(
                    in.getParallel(),
                    in.getOriginLon(),
                    1,
                    in.getFalseEasting(),
                    in.getFalseNorthing()
            );
            srs.SetLinearUnits(osr.SRS_UL_METER, 1.0);
            return srs.ExportToPrettyWkt();

        } else if (projection instanceof Orthographic) {
            Orthographic in = (Orthographic) projection;
            SpatialReference srs = new SpatialReference();
            setGcsParameters(in, srs);
            srs.SetOrthographic(
                    in.getOriginLat(),
                    in.getOriginLon(),
                    0,
                    0
            );
            srs.SetLinearUnits(osr.SRS_UL_METER, 1.0);
            return srs.ExportToPrettyWkt();

        } else if (projection instanceof Sinusoidal) {
            Sinusoidal in = (Sinusoidal) projection;
            SpatialReference srs = new SpatialReference();
            setGcsParameters(in, srs);
            srs.SetSinusoidal(
                    in.getCentMeridian(),
                    in.getFalseEasting(),
                    in.getFalseNorthing()
            );
            srs.SetLinearUnits(osr.SRS_UL_METER, 1.0);
            return srs.ExportToPrettyWkt();

        } else {
            return "";
        }

    }

    private static void setGcsParameters(ProjectionImpl projection, SpatialReference srs) {
        List<Parameter> parameters = projection.getProjectionParameters();
        Optional<Parameter> semiMajor = parameters.stream()
                .filter(parameter -> parameter.getName().equalsIgnoreCase("semi_major_axis"))
                .findAny();
        Optional<Parameter> semiMinor = parameters.stream()
                .filter(parameter -> parameter.getName().equalsIgnoreCase("semi_minor_axis"))
                .findAny();
        Optional<Parameter> radius = parameters.stream()
                .filter(parameter -> parameter.getName().equalsIgnoreCase("earth_radius"))
                .findAny();
        if (semiMajor.isPresent() && semiMinor.isPresent()){
            double invFlattening = 1.0 / (1.0 - (semiMinor.get().getNumericValue()/semiMajor.get().getNumericValue()));
            srs.SetGeogCS(
                    "unknown",
                    "unknown",
                    "spheroid",
                    semiMajor.get().getNumericValue(),
                    invFlattening);
        } else if (radius.isPresent()){
            srs.SetGeogCS(
                    "unknown",
                    "unknown",
                    "spheroid",
                    radius.get().getNumericValue(),
                    0);
        } else {
            srs.SetWellKnownGeogCS( WGS84 );
        }
    }

    public static String create(String name) {
        if (name.contains("SHG")) {
            SpatialReference srs = new SpatialReference();
            srs.SetProjCS("USA_Contiguous_Albers_Equal_Area_Conic_USGS_version");
            srs.SetWellKnownGeogCS("NAD83");
            srs.SetACEA(29.5, 45.5, 23.0, -96.0, 0, 0);
            srs.SetLinearUnits(osr.SRS_UL_METER, 1.0);
            return srs.ExportToPrettyWkt();
        }
        if (name.startsWith("UTM")){
            int zone = Integer.parseInt(name.substring(3, name.length()-1));
            int hemisphere;
            if (name.substring(name.length() - 1).equals("N")){
                hemisphere = 1;
            } else {
                hemisphere = 0;
            }
            SpatialReference srs = new SpatialReference();
            srs.SetProjCS("WGS 84 / UTM zone " + zone + name.substring(name.length() - 1));
            srs.SetWellKnownGeogCS(WGS84);
            srs.SetUTM(zone, hemisphere);
            srs.SetLinearUnits(osr.SRS_UL_METER, 1.0);
            return srs.ExportToPrettyWkt();
        }
        return "";
    }

    public static String shg(){
        return  "PROJCS[\"USA_Contiguous_Albers_Equal_Area_Conic_USGS_version\"," +
                "        GEOGCS[\"GCS_North_American_1983\"," +
                "        DATUM[\"D_North_American_1983\"," +
                "        SPHEROID[\"GRS_1980\",6378137.0,298.257222101]]," +
                "PRIMEM[\"Greenwich\",0.0]," +
                "UNIT[\"Degree\",0.0174532925199433]]," +
                "PROJECTION[\"Albers\"]," +
                "        PARAMETER[\"False_Easting\",0.0]," +
                "PARAMETER[\"False_Northing\",0.0]," +
                "PARAMETER[\"Central_Meridian\",-96.0]," +
                "PARAMETER[\"Standard_Parallel_1\",29.5]," +
                "PARAMETER[\"Standard_Parallel_2\",45.5]," +
                "PARAMETER[\"Latitude_Of_Origin\",23.0]," +
                "UNIT[\"Meter\",1.0]]";
    }
}