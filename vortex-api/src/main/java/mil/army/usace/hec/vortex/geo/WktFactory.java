package mil.army.usace.hec.vortex.geo;

import hec.heclib.grid.AlbersInfo;
import hec.heclib.grid.GridInfo;
import hec.heclib.grid.SpecifiedGridInfo;
import mil.army.usace.hec.vortex.GdalRegister;
import org.gdal.osr.SpatialReference;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.projection.*;
import ucar.unidata.geoloc.projection.proj4.AlbersEqualAreaEllipse;
import ucar.unidata.geoloc.projection.proj4.LambertConformalConicEllipse;
import ucar.unidata.geoloc.projection.proj4.StereographicAzimuthalProjection;
import ucar.unidata.geoloc.projection.proj4.TransverseMercatorProjection;
import ucar.unidata.util.Parameter;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.gdal.osr.osrConstants.*;

public class WktFactory {
    private static final Logger logger = Logger.getLogger(WktFactory.class.getName());

    static {
        GdalRegister.getInstance();
    }

    private static final String WGS84 = "WGS84";

    private WktFactory() {
    }

    public static String createWkt(Projection projection) {
        if (projection instanceof LatLonProjection in) {
            SpatialReference srs = new SpatialReference();
            setGcsParameters(in, srs);

            String wkt = srs.ExportToWkt();

            srs.delete();

            return wkt;

        } else if (projection instanceof AlbersEqualArea in) {
            SpatialReference srs = new SpatialReference();
            srs.SetProjCS("Albers Equal Area");
            setGcsParameters(in, srs);
            srs.SetACEA(
                    in.getParallelOne(),
                    in.getParallelTwo(),
                    in.getOriginLat(),
                    in.getOriginLon(),
                    in.getFalseEasting(),
                    in.getFalseNorthing()
            );
            srs.SetLinearUnits(SRS_UL_METER, 1.0);

            String wkt = srs.ExportToPrettyWkt();

            srs.delete();

            return wkt;

        } else if (projection instanceof AlbersEqualAreaEllipse in) {
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
            srs.SetLinearUnits(SRS_UL_METER, 1.0);

            String wkt = srs.ExportToPrettyWkt();

            srs.delete();

            return wkt;

        } else if (projection instanceof LambertConformal in) {
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
            srs.SetLinearUnits(SRS_UL_METER, 1.0);

            String wkt = srs.ExportToPrettyWkt();

            srs.delete();

            return wkt;

        } else if (projection instanceof LambertConformalConicEllipse in) {
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
            srs.SetLinearUnits(SRS_UL_METER, 1.0);

            String wkt = srs.ExportToPrettyWkt();

            srs.delete();

            return wkt;

        } else if (projection instanceof Mercator in) {
            SpatialReference srs = new SpatialReference();
            setGcsParameters(in, srs);
            srs.SetMercator(
                    in.getParallel(),
                    in.getOriginLon(),
                    1,
                    in.getFalseEasting(),
                    in.getFalseNorthing()
            );
            srs.SetLinearUnits(SRS_UL_METER, 1.0);

            String wkt = srs.ExportToPrettyWkt();

            srs.delete();

            return wkt;

        } else if (projection instanceof Orthographic in) {
            SpatialReference srs = new SpatialReference();
            setGcsParameters(in, srs);
            srs.SetOrthographic(
                    in.getOriginLat(),
                    in.getOriginLon(),
                    0,
                    0
            );
            srs.SetLinearUnits(SRS_UL_METER, 1.0);

            String wkt = srs.ExportToPrettyWkt();

            srs.delete();

            return wkt;

        } else if (projection instanceof Sinusoidal in) {
            SpatialReference srs = new SpatialReference();
            setGcsParameters(in, srs);
            srs.SetSinusoidal(
                    in.getCentMeridian(),
                    in.getFalseEasting(),
                    in.getFalseNorthing()
            );
            srs.SetLinearUnits(SRS_UL_METER, 1.0);

            String wkt = srs.ExportToPrettyWkt();

            srs.delete();

            return wkt;

        } else if (projection instanceof Stereographic in) {
            SpatialReference srs = new SpatialReference();
            setGcsParameters(in, srs);
            srs.SetStereographic(
                    in.getTangentLat(),
                    in.getTangentLon(),
                    in.getScale(),
                    in.getFalseEasting(),
                    in.getFalseNorthing()
            );
            srs.SetLinearUnits(SRS_UL_METER, 1.0);

            String wkt = srs.ExportToPrettyWkt();

            srs.delete();

            return wkt;

        } else if (projection instanceof StereographicAzimuthalProjection in) {
            SpatialReference srs = new SpatialReference();

            setGcsParameters(in, srs);

            List<Parameter> parameters = projection.getProjectionParameters();

            Map<String, Double> numericParameters = parameters.stream()
                    .filter(p -> !p.isString())
                    .collect(Collectors.toMap(Parameter::getName, Parameter::getNumericValue));

            double centerLat = numericParameters.get("latitude_of_projection_origin");
            double centerLon = numericParameters.get("longitude_of_projection_origin");
            double scale = numericParameters.get("scale_factor_at_projection_origin");
            double falseEasting = numericParameters.getOrDefault("false_easting", 0.0);
            double falseNorthing = numericParameters.getOrDefault("false_northing", 0.0);

            srs.SetPS(
                    centerLat,
                    centerLon,
                    scale,
                    falseEasting,
                    falseNorthing
            );

            srs.SetLinearUnits(SRS_UL_METER, 1.0);

            String wkt = srs.ExportToPrettyWkt();

            srs.delete();

            return wkt;

        } else if (projection instanceof RotatedPole) {
            List<Parameter> parameters = projection.getProjectionParameters();

            Map<String, Double> numericParameters = parameters.stream()
                    .filter(p -> !p.isString())
                    .collect(Collectors.toMap(Parameter::getName, Parameter::getNumericValue));

            double gridNorthPoleLon = numericParameters.get("grid_north_pole_longitude");
            double gridNorthPoleLat = numericParameters.get("grid_north_pole_latitude");

            SpatialReference srs = new SpatialReference();

            String proj4 = String.format("+proj=ob_tran +o_proj=longlat +lon_0=%.18g +o_lon_p=%.18g " +
                            "+o_lat_p=%.18g +a=%.18g +b=%.18g +to_meter=0.0174532925199 +wktext",
                    180.0 + gridNorthPoleLon, 0.0,
                    gridNorthPoleLat, srs.GetSemiMajor(),
                    srs.GetSemiMinor());

            srs.ImportFromProj4(proj4);

            String wkt = srs.ExportToPrettyWkt();

            srs.delete();

            return wkt;
        } else if (projection instanceof TransverseMercator in) {
            SpatialReference srs = new SpatialReference();
            setGcsParameters(in, srs);
            List<Parameter> parameters = projection.getProjectionParameters();

            Map<String, Double> numericParameters = parameters.stream()
                    .filter(p -> !p.isString())
                    .collect(Collectors.toMap(Parameter::getName, Parameter::getNumericValue));

            Map<String, String> stringParameters = parameters.stream()
                    .filter(Parameter::isString)
                    .filter(p -> Objects.nonNull(p.getStringValue()))
                    .collect(Collectors.toMap(Parameter::getName, Parameter::getStringValue));

            int factor = Objects.equals(stringParameters.get("units"), "km") ? 1000 : 1;

            double centerLatitude = numericParameters.get("latitude_of_projection_origin");
            double centerLongitude = numericParameters.get("longitude_of_central_meridian");
            double scaleFactor = numericParameters.get("scale_factor_at_central_meridian");
            double falseEasting = numericParameters.get("false_easting") * factor;
            double falseNorthing = numericParameters.get("false_northing") * factor;

            srs.SetTM(
                    centerLatitude,
                    centerLongitude,
                    scaleFactor,
                    falseEasting,
                    falseNorthing
            );
            srs.SetLinearUnits(SRS_UL_METER, 1.0);

            String wkt = srs.ExportToPrettyWkt();

            srs.delete();

            return wkt;
        } else if (projection instanceof TransverseMercatorProjection in) {
            SpatialReference srs = new SpatialReference();
            setGcsParameters(in, srs);
            List<Parameter> parameters = projection.getProjectionParameters();

            Map<String, Double> numericParameters = parameters.stream()
                    .filter(p -> !p.isString())
                    .collect(Collectors.toMap(Parameter::getName, Parameter::getNumericValue));

            Map<String, String> stringParameters = parameters.stream()
                    .filter(Parameter::isString)
                    .filter(p -> Objects.nonNull(p.getStringValue()))
                    .collect(Collectors.toMap(Parameter::getName, Parameter::getStringValue));

            int factor = Objects.equals(stringParameters.get("units"), "km") ? 1000 : 1;

            double centerLatitude = numericParameters.get("latitude_of_projection_origin");
            double centerLongitude = numericParameters.get("longitude_of_central_meridian");
            double scaleFactor = numericParameters.get("scale_factor_at_central_meridian");
            double falseEasting = numericParameters.get("false_easting") * factor;
            double falseNorthing = numericParameters.get("false_northing") * factor;

            srs.SetTM(
                    centerLatitude,
                    centerLongitude,
                    scaleFactor,
                    falseEasting,
                    falseNorthing
            );
            srs.SetLinearUnits(SRS_UL_METER, 1.0);

            String wkt = srs.ExportToPrettyWkt();

            srs.delete();

            return wkt;
        } else {
            logger.severe(() -> "Projection " + projection + " not supported");
            return "";
        }

    }

    private static void setGcsParameters(Projection projection, SpatialReference srs) {
        List<Parameter> parameters = projection.getProjectionParameters();
        Optional<Parameter> semiMajor = parameters.stream()
                .filter(parameter -> parameter.getName().equalsIgnoreCase("semi_major_axis"))
                .findAny();
        Optional<Parameter> semiMinor = parameters.stream()
                .filter(parameter -> parameter.getName().equalsIgnoreCase("semi_minor_axis"))
                .findAny();
        Optional<Parameter> inverseFlattening = parameters.stream()
                .filter(parameter -> parameter.getName().equalsIgnoreCase("inverse_flattening"))
                .findAny();
        Optional<Parameter> radius = parameters.stream()
                .filter(parameter -> parameter.getName().equalsIgnoreCase("earth_radius"))
                .findAny();
        if (semiMajor.isPresent() && semiMinor.isPresent()) {
            double invFlattening = 1.0 / (1.0 - (semiMinor.get().getNumericValue() / semiMajor.get().getNumericValue()));
            srs.SetGeogCS(
                    "unknown",
                    "unknown",
                    "spheroid",
                    semiMajor.get().getNumericValue(),
                    invFlattening);
        } else if (semiMajor.isPresent() && inverseFlattening.isPresent()) {
            double invFlatteningValue = inverseFlattening.get().getNumericValue();
            double invFlatteningValueNonInf = Double.isInfinite(invFlatteningValue) ? 0 : invFlatteningValue;
            srs.SetGeogCS(
                    "unknown",
                    "unknown",
                    "spheroid",
                    semiMajor.get().getNumericValue(),
                    invFlatteningValueNonInf);
        } else if (radius.isPresent()) {
            srs.SetGeogCS(
                    "unknown",
                    "unknown",
                    "sphere",
                    radius.get().getNumericValue(),
                    0);
        } else {
            srs.SetWellKnownGeogCS(WGS84);
        }
    }

    public static String create(String name) {
        if (name.contains("SHG")) {
            return getShg();
        }
        if (name.startsWith("UTM")) {
            int zone = Integer.parseInt(name.substring(3, name.length() - 1));
            int hemisphere;
            if (name.endsWith("N")) {
                hemisphere = 1;
            } else {
                hemisphere = 0;
            }
            SpatialReference srs = new SpatialReference();
            srs.SetProjCS("WGS 84 / UTM zone " + zone + name.substring(name.length() - 1));
            srs.SetWellKnownGeogCS(WGS84);
            srs.SetUTM(zone, hemisphere);
            srs.SetLinearUnits(SRS_UL_METER, 1.0);

            String wkt = srs.ExportToPrettyWkt();

            srs.delete();

            return wkt;
        }
        return null;
    }

    /**
     * @deprecated since v0.10.28, replaced by {@link #getShg}
     */
    @Deprecated
    public static String shg() {
        return getShg();
    }

    public static String getShg() {
        return "PROJCS[\"USA_Contiguous_Albers_Equal_Area_Conic_USGS_version\"," +
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

    public static String fromEpsg(int epsg) {
        SpatialReference srs = new SpatialReference();
        srs.ImportFromEPSG(epsg);
        String wkt = srs.ExportToPrettyWkt();
        srs.delete();
        return wkt;
    }

    public static String fromGridInfo(GridInfo gridInfo) {
        if (ReferenceUtils.isShg(gridInfo)) {
            return getShg();
        }
        if (gridInfo instanceof AlbersInfo albersInfo) {
            SpatialReference srs = getSpatialReference(albersInfo);

            String units = albersInfo.getProjectionUnits().toLowerCase();
            if (units.contains("foot") || units.contains("us")) {
                srs.SetLinearUnits(SRS_UL_US_FOOT, Double.parseDouble(SRS_UL_FOOT_CONV));
            } else {
                srs.SetLinearUnits(SRS_UL_METER, 1.0);
            }

            String wkt = srs.ExportToWkt();
            srs.delete();
            return wkt;
        }
        if (gridInfo instanceof SpecifiedGridInfo specifiedGridInfo) {
            SpatialReference srs = new SpatialReference(specifiedGridInfo.getSpatialReferenceSystem());
            String wkt = srs.ExportToWkt();
            srs.delete();
            return wkt;
        }
        SpatialReference srs = new SpatialReference(gridInfo.getSpatialReferenceSystem());
        String wkt = srs.ExportToWkt();
        srs.delete();
        return wkt;
    }

    private static SpatialReference getSpatialReference(AlbersInfo albersInfo) {
        SpatialReference srs = new SpatialReference();

        int datum = albersInfo.getProjectionDatum();
        if (datum == GridInfo.getNad83()) {
            srs.SetWellKnownGeogCS("NAD83");
        } else if (datum == GridInfo.getNad27()) {
            srs.SetWellKnownGeogCS("NAD27");
        }

        double parallel1 = albersInfo.getFirstStandardParallel();
        double parallel2 = albersInfo.getSecondStandardParallel();
        double originLat = albersInfo.getLatitudeOfProjectionOrigin();
        double originLon = albersInfo.getCentralMeridian();
        double falseEasting = albersInfo.getFalseEasting();
        double falseNorthing = albersInfo.getFalseNorthing();
        srs.SetACEA(parallel1, parallel2, originLat, originLon, falseEasting, falseNorthing);
        return srs;
    }
}