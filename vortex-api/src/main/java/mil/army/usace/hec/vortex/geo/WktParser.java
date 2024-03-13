package mil.army.usace.hec.vortex.geo;

import hec.heclib.grid.AlbersInfo;
import hec.heclib.grid.GridInfo;
import org.gdal.osr.SpatialReference;
import ucar.unidata.geoloc.Earth;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.projection.*;
import ucar.unidata.geoloc.projection.proj4.LambertConformalConicEllipse;

public class WktParser {
    private WktParser() {
        // Private empty constructor for utility class
    }

    public static Projection getProjection(String wkt) {
        Projection defaultProjection = new LatLonProjection();
        if (wkt == null || wkt.isEmpty()) return defaultProjection;
        SpatialReference srs = new SpatialReference(wkt);

        try {
            if (srs.IsGeographic() == 1) return parseLatLong(srs);
            String projectionType = srs.GetAttrValue("PROJECTION");
            if (projectionType.matches("(?i).*albers.*conic.*equal.*area.*"))
                return parseAlbersEqualArea(srs);
            if (projectionType.matches("(?i).*lambert.*conformal.*conic.*"))
                return parseLambertConformalConic(srs);
            if (projectionType.matches("(?i).*lambert.*conformal.*"))
                return parseLambertConformal(srs);
            if (projectionType.matches("(?i).*orthographic.*"))
                return parseOrthographic(srs);
            if (projectionType.matches("(?i).*sinusoidal.*"))
                return parseSinusoidal(srs);
            if (projectionType.matches("(?i).*stereographic"))
                return parseStereographic(srs);
            if (projectionType.matches("(?i).*rotated.*pole.*"))
                return parseRotatedPole(srs);
            if (projectionType.matches("(?i).*transverse.*mercator.*"))
                return parseTransverseMercator(srs);
            if (projectionType.matches("(?i).*mercator.*"))
                return parseMercator(srs);
        } finally {
            srs.delete();
        }

        return defaultProjection;
    }

    public static String getProjectionUnit(String wkt) {
        SpatialReference srs = new SpatialReference(wkt);
        String unitName = srs.GetLinearUnitsName().toLowerCase();
        switch (unitName) {
            case "meter":
            case "metre":
                return "m";
            default:
                return unitName;
        }
    }

    public static GridInfo getGridInfo(String wkt) {
        GridInfo defaultGridInfo = new GridInfo();
        if (wkt == null || wkt.isEmpty()) return defaultGridInfo;
        SpatialReference srs = new SpatialReference(wkt);

        String projectionType = srs.GetAttrValue("PROJECTION").toLowerCase();
        if (projectionType.contains("albers")) {
            AlbersInfo albersInfo = new AlbersInfo();
            albersInfo.setCentralMeridian((float) getCenterLongitude(srs));
            albersInfo.setLatitudeOfProjectionOrigin((float) getCenterLatitude(srs));
            albersInfo.setStandardParallels((float) getStandardParallel1(srs), (float) getStandardParallel2(srs));
            albersInfo.setFalseEastingAndNorthing((float) getFalseEasting(srs), (float) getFalseNorthing(srs));
            albersInfo.setProjectionDatum(GridInfo.getNad83());
            albersInfo.setProjectionUnits(getProjectionUnit(wkt));
            return albersInfo;
        }

        return defaultGridInfo;
    }

    private static Projection parseLatLong(SpatialReference srs) {
        String projString = srs.ExportToProj4();
        if (projString.contains("+o_lon_p") && projString.contains("+o_lat_p"))
            return parseRotatedPole(srs);

        Earth earth = getEarth(srs);
        return new LatLonProjection(earth);
    }

    private static Projection parseAlbersEqualArea(SpatialReference srs) {
        return new AlbersEqualArea(
                getCenterLatitude(srs),
                getCenterLongitude(srs),
                getStandardParallel1(srs),
                getStandardParallel2(srs),
                getFalseEasting(srs),
                getFalseNorthing(srs),
                getRadiusKm(srs)
        );
    }

    private static Projection parseLambertConformalConic(SpatialReference srs) {
        return new LambertConformalConicEllipse(
                getCenterLatitude(srs),
                getCenterLongitude(srs),
                getStandardParallel1(srs),
                getStandardParallel2(srs),
                getFalseEasting(srs),
                getFalseNorthing(srs),
                getEarth(srs)
        );
    }

    private static Projection parseLambertConformal(SpatialReference srs) {
        return new LambertConformal(
                getCenterLatitude(srs),
                getCenterLongitude(srs),
                getStandardParallel1(srs),
                getStandardParallel2(srs),
                getFalseEasting(srs),
                getFalseNorthing(srs),
                getRadiusKm(srs)
        );
    }

    private static Projection parseOrthographic(SpatialReference srs) {
        return new Orthographic(
                getCenterLatitude(srs),
                getCenterLongitude(srs),
                getRadiusKm(srs)
        );
    }

    private static Projection parseSinusoidal(SpatialReference srs) {
        return new Sinusoidal(
                getCenterLongitude(srs),
                getFalseEasting(srs),
                getFalseNorthing(srs),
                getRadiusKm(srs)
        );
    }
    private static Projection parseStereographic(SpatialReference srs) {
        return new Stereographic(
                getCenterLatitude(srs),
                getCenterLongitude(srs),
                getScaleFactor(srs), // Double-check
                getFalseEasting(srs),
                getFalseNorthing(srs)
        );
    }

    private static Projection parseRotatedPole(SpatialReference srs) {
        double[] northLatLon = getNorthLatLon(srs);
        return new RotatedPole(
                northLatLon[0],
                northLatLon[1]
        );
    }

    private static Projection parseTransverseMercator(SpatialReference srs) {
        return new TransverseMercator(
                getCenterLatitude(srs),
                getCenterLongitude(srs), // Double-check
                getScaleFactor(srs), // Double-check
                getFalseEasting(srs),
                getFalseNorthing(srs),
                getRadiusKm(srs)
        );
    }

    private static Projection parseMercator(SpatialReference srs) {
        return new Mercator(
                getCenterLongitude(srs),
                getStandardParallel(srs), // Double-check
                getFalseEasting(srs),
                getFalseNorthing(srs)
        );
    }

    /* Extract from SpatialReference */
    private static double getCenterLatitude(SpatialReference srs) {
        double ogrValue = srs.GetProjParm("latitude_of_center");
        double esriValue = srs.GetProjParm("latitude_of_origin");
        return getValidValue(ogrValue, esriValue);
    }

    private static double getCenterLongitude(SpatialReference srs) {
        double ogrValue = srs.GetProjParm("longitude_of_center");
        double esriValue = srs.GetProjParm("central_meridian");
        return getValidValue(ogrValue, esriValue);
    }

    private static double getFalseEasting(SpatialReference srs) {
        return srs.GetProjParm("false_easting");
    }

    private static double getFalseNorthing(SpatialReference srs) {
        return srs.GetProjParm("false_northing");
    }

    private static double getStandardParallel(SpatialReference srs) {
        return srs.GetProjParm("standard_parallel");
    }

    private static double getStandardParallel1(SpatialReference srs) {
        return srs.GetProjParm("standard_parallel_1");
    }

    private static double getStandardParallel2(SpatialReference srs) {
        return srs.GetProjParm("standard_parallel_2");
    }

    private static double getRadiusKm(SpatialReference srs) {
        return srs.GetSemiMajor() / 1000;
    }

    private static double getScaleFactor(SpatialReference srs) {
        return srs.GetProjParm("scale_factor");
    }

    private static double[] getNorthLatLon(SpatialReference srs) {
        String projString = srs.ExportToProj4();
        String originLatKey = "+o_lat_p=";
        String originLonKey = "+lon_0=";

        double[] latLon = new double[2];
        for (String param : projString.split("\\s+")) {
            if (param.startsWith(originLatKey)) {
                double oLatP = Double.parseDouble(param.substring(originLatKey.length()));
                latLon[0] = oLatP;
            }
            if (param.startsWith(originLonKey)) {
                double oLonP = Double.parseDouble(param.substring(originLonKey.length()));
                latLon[1] = oLonP - 180;
            }
        }

        return latLon;
    }

    private static Earth getEarth(SpatialReference srs) {
        double semiMajor = srs.GetSemiMajor();
        double semiMinor = srs.GetSemiMinor();
        double inverseFlattening = srs.GetInvFlattening();
        return new Earth(semiMajor, semiMinor, inverseFlattening);
    }

    private static double getValidValue(double... values) {
        for (double value : values)
            if (value != 0)
                return value;
        return Double.NaN;
    }
}
