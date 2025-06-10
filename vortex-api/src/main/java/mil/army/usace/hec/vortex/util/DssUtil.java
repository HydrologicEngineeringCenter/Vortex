package mil.army.usace.hec.vortex.util;

import hec.heclib.dss.DSSPathname;
import hec.heclib.dss.DssDataType;
import hec.heclib.grid.AlbersInfo;
import hec.heclib.grid.GridInfo;
import hec.heclib.grid.SpecifiedGridInfo;
import hec.heclib.util.HecTime;
import mil.army.usace.hec.vortex.VortexGrid;
import org.gdal.osr.SpatialReference;
import si.uom.NonSI;
import tech.units.indriya.unit.Units;

import javax.measure.Unit;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static javax.measure.MetricPrefix.KILO;
import static javax.measure.MetricPrefix.MILLI;
import static systems.uom.common.USCustomary.*;
import static tech.units.indriya.AbstractUnit.ONE;
import static tech.units.indriya.unit.Units.HOUR;
import static tech.units.indriya.unit.Units.MINUTE;
import static tech.units.indriya.unit.Units.*;

public class DssUtil {
    private static final int SECONDS_PER_MINUTE = 60;

    // Map Unit<?> object to string (to be written to DSS)
    private static final Map<Unit<?>, String> UNITS_TO_STRING = Map.ofEntries(
            Map.entry(INCH, "IN"),
            Map.entry(MILLI(METRE), "MM"),
            Map.entry(CUBIC_FOOT.divide(SECOND), "CFS"),
            Map.entry(CUBIC_METRE.divide(SECOND), "M3/S"),
            Map.entry(METRE, "M"),
            Map.entry(FOOT, "FT"),
            Map.entry(FAHRENHEIT, "DEG F"),
            Map.entry(CELSIUS, "DEG C"),
            Map.entry(CELSIUS.multiply(DAY), "DEGC-D"),
            Map.entry(FAHRENHEIT.multiply(DAY), "DEGF-D"),
            Map.entry(MILLI(METRE).divide(CELSIUS.multiply(DAY)), "MM/DEG-D"),
            Map.entry(INCH.divide(FAHRENHEIT.multiply(DAY)), "IN/DEG-D"),
            Map.entry(JOULE.divide(SQUARE_METRE).multiply(41840).divide(MINUTE), "LANG/MIN"),
            Map.entry(JOULE.divide(SQUARE_METRE), "J/M2"),
            Map.entry(UnitUtil.BTU_PER_FT2, "BTU/FT2"),
            Map.entry(WATT.divide(SQUARE_METRE), "WATT/M2"),
            Map.entry(ONE, "UNSPECIF"),
            Map.entry(MILLI(GRAM).divide(LITRE), "MG/L"),
            Map.entry(Units.PERCENT, "%"),
            Map.entry(HOUR, "HR"),
            Map.entry(MINUTE, "MINUTES"),
            Map.entry(KILO(METRE).divide(HOUR), "KPH"),
            Map.entry(MILE.divide(HOUR), "MPH"),
            Map.entry(FOOT_PER_SECOND, "FT/S"),
            Map.entry(METRE_PER_SECOND, "M/S"),
            Map.entry(INCH.divide(HOUR), "IN/HR"),
            Map.entry(INCH.divide(DAY), "IN/DAY"),
            Map.entry(MILLI(METRE).divide(SECOND), "MM/S"),
            Map.entry(MILLI(METRE).divide(HOUR), "MM/HR"),
            Map.entry(MILLI(METRE).divide(DAY), "MM/DAY"),
            Map.entry(TON, "TONS"),
            Map.entry(KILOGRAM.multiply(1000), "TONNES"),
            Map.entry(MILE, "MILE"),
            Map.entry(KILO(METRE), "KM"),
            Map.entry(KILO(PASCAL), "KPA"),
            Map.entry(PASCAL, "PA"),
            Map.entry(NonSI.INCH_OF_MERCURY, "IN HG"),
            Map.entry(SQUARE_METRE, "M2"),
            Map.entry(SQUARE_FOOT, "SQFT"),
            Map.entry(ACRE, "ACRE"),
            Map.entry(SQUARE_METRE.multiply(1000), "THOU M2"),
            Map.entry(CUBIC_METRE, "M3"),
            Map.entry(CUBIC_METRE.multiply(1000), "THOU M3"),
            Map.entry(ACRE_FOOT, "AC-FT")
    );

    private DssUtil(){}

    public static Map<String, Set<String>> getPathnameParts(List<String> pathnames){
        Set<String> aParts = new HashSet<>();
        Set<String> bParts = new HashSet<>();
        Set<String> cParts = new HashSet<>();
        Set<String> fParts = new HashSet<>();
        pathnames.stream().map(DSSPathname::new).forEach(pathname ->{
            aParts.add(pathname.getAPart());
            bParts.add(pathname.getBPart());
            cParts.add(pathname.getCPart());
            fParts.add(pathname.getFPart());
        });
        Map<String, Set<String>> parts = new HashMap<>();
        parts.put("aParts", aParts);
        parts.put("bParts", bParts);
        parts.put("cParts", cParts);
        parts.put("fParts", fParts);
        return parts;
    }

    public static GridInfo getGridInfo(VortexGrid grid) {
        GridInfo gridInfo;

        SpatialReference srs = new SpatialReference(grid.wkt());
        String crsName;
        if (srs.IsProjected() == 1) {
            crsName = srs.GetAttrValue("projcs");
        } else if (srs.IsGeographic() == 1) {
            crsName = srs.GetAttrValue("geogcs");
        } else {
            crsName = "";
        }

        if (crsName.toLowerCase().contains("albers")) {

            AlbersInfo albersInfo = new AlbersInfo();
            albersInfo.setCoordOfGridCellZero(0, 0);

            String datum = srs.GetAttrValue("geogcs");
            if (datum.contains("83")) {
                albersInfo.setProjectionDatum(GridInfo.getNad83());
            }

            String units = srs.GetLinearUnitsName();
            albersInfo.setProjectionUnits(units);

            double centralMeridian = srs.GetProjParm("central_meridian");
            albersInfo.setCentralMeridian((float) centralMeridian);

            double falseEasting = srs.GetProjParm("false_easting");
            double falseNorthing = srs.GetProjParm("false_northing");
            albersInfo.setFalseEastingAndNorthing((float) falseEasting, (float) falseNorthing);

            double latitudeOfOrigin = srs.GetProjParm("latitude_of_origin");
            albersInfo.setLatitudeOfProjectionOrigin((float) latitudeOfOrigin);

            double standardParallel1 = srs.GetProjParm("standard_parallel_1");
            double standardParallel2 = srs.GetProjParm("standard_parallel_2");
            albersInfo.setStandardParallels((float) standardParallel1, (float) standardParallel2);

            gridInfo = albersInfo;
        } else {
            SpecifiedGridInfo specifiedGridInfo = new SpecifiedGridInfo();
            specifiedGridInfo.setSpatialReference(crsName, grid.wkt(), 0, 0);
            gridInfo = specifiedGridInfo;
        }

        double llx = grid.originX();
        double lly;
        if (grid.dy() < 0) {
            lly = grid.originY() + grid.dy() * grid.ny();
        } else {
            lly = grid.originY();
        }
        double dx = grid.dx();
        double dy = grid.dy();
        float cellSize = (float) ((Math.abs(dx) + Math.abs(dy)) / 2.0);
        int minX = (int) Math.round(llx / cellSize);
        int minY = (int) Math.round(lly / cellSize);

        gridInfo.setCellInfo(minX, minY, grid.nx(), grid.ny(), cellSize);

        Unit<?> units = UnitUtil.parse(grid.units());
        String unitsString = getUnitsString(units);
        gridInfo.setDataUnits(unitsString);

        ZonedDateTime startTime = grid.startTime();

        if (startTime == null)
            return gridInfo;

        ZonedDateTime endTime = grid.endTime();
        if (!startTime.equals(endTime) && units.isCompatible(CELSIUS)) {
            gridInfo.setDataType(DssDataType.PER_AVER.value());
        } else if (startTime.equals(endTime)) {
            gridInfo.setDataType(DssDataType.INST_VAL.value());
        } else {
            gridInfo.setDataType(DssDataType.PER_CUM.value());
        }

        HecTime hecTimeStart = getHecTime(startTime);
        hecTimeStart.showTimeAsBeginningOfDay(true);
        HecTime hecTimeEnd = getHecTime(endTime);
        hecTimeEnd.showTimeAsBeginningOfDay(false);

        gridInfo.setGridTimes(hecTimeStart, hecTimeEnd);

        return gridInfo;
    }

    public static String getUnitsString(Unit<?> unit) {
        if (unit == null) {
            return "";
        }

        return UNITS_TO_STRING.getOrDefault(unit, unit.toString());
    }

    public static String getEPart(int seconds) {
        int minutes = seconds / SECONDS_PER_MINUTE;
        return switch (minutes) {
            case 1 -> "1Minute";
            case 2 -> "2Minute";
            case 3 -> "3Minute";
            case 4 -> "4Minute";
            case 5 -> "5Minute";
            case 6 -> "6Minute";
            case 8 -> "8Minute";
            case 10 -> "10Minute";
            case 12 -> "12Minute";
            case 15 -> "15Minute";
            case 20 -> "20Minute";
            case 30 -> "30Minutes";
            case 60 -> "1Hour";
            case 120 -> "2Hours";
            case 180 -> "3Hours";
            case 240 -> "4Hours";
            case 360 -> "6Hours";
            case 480 -> "8Hours";
            case 720 -> "12Hours";
            case 1440 -> "1Day";
            case 2880 -> "2Days";
            case 5760 -> "3Days";
            case 7200 -> "4Days";
            case 8640 -> "6Days";
            case 10080 -> "1Week";
            case 43200 -> "1Month";
            case 525600 -> "1Year";
            default -> "0";
        };
    }

    private static HecTime getHecTime(ZonedDateTime zonedDateTime) {
        return new HecTime(zonedDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }
}
