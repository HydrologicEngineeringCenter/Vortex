package mil.army.usace.hec.vortex.util;

import hec.heclib.dss.DSSPathname;
import hec.heclib.dss.DssDataType;
import hec.heclib.grid.AlbersInfo;
import hec.heclib.grid.GridInfo;
import hec.heclib.grid.SpecifiedGridInfo;
import hec.heclib.util.HecTime;
import mil.army.usace.hec.vortex.VortexGrid;
import org.gdal.osr.SpatialReference;

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

        Unit<?> units = UnitUtil.getUnits(grid.units());
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

    private static String getUnitsString(Unit<?> unit) {
        if (unit.equals(MILLI(METRE))) {
            return "MM";
        } else if (unit.equals(INCH)) {
            return "IN";
        } else if (unit.equals(INCH.divide(HOUR))) {
            return "IN/HR";
        } else if (unit.equals(MILLI(METRE).divide(SECOND))) {
            return "MM/S";
        } else if (unit.equals(MILLI(METRE).divide(HOUR))) {
            return "MM/HR";
        } else if (unit.equals(MILLI(METRE).divide(DAY))) {
            return "MM/DAY";
        } else if (unit.equals(CUBIC_METRE.divide(SECOND))) {
            return "M3/S";
        } else if (unit.equals(CUBIC_FOOT.divide(SECOND))) {
            return "CFS";
        } else if (unit.equals(METRE)) {
            return "M";
        } else if (unit.equals(FOOT)) {
            return "FT";
        } else if (unit.equals(CELSIUS)) {
            return "DEG C";
        } else if (unit.equals(FAHRENHEIT)) {
            return "DEG F";
        } else if (unit.equals(WATT.divide(SQUARE_METRE))) {
            return "WATT/M2";
        } else if (unit.equals(KILOMETRE_PER_HOUR)) {
            return "KPH";
        } else if (unit.equals(METRE_PER_SECOND)) {
            return "M/S";
        } else if (unit.equals(MILE_PER_HOUR)) {
            return "MPH";
        } else if (unit.equals(FOOT_PER_SECOND)) {
            return "FT/S";
        } else if (unit.equals(KILO(PASCAL))) {
            return "KPA";
        } else if (unit.equals(PASCAL)) {
            return "PA";
        } else if (unit.equals(PERCENT)) {
            return "%";
        } else if (unit.equals(KILO(METRE))) {
            return "KM";
        } else if (unit.equals(MILE)) {
            return "MILE";
        } else if (unit.equals(ONE)) {
            return "UNSPECIF";
        } else if (unit.equals(TON)) {
            return "TONS";
        } else if (unit.equals(MILLI(GRAM).divide(LITRE))) {
            return "MG/L";
        } else if (unit.equals(CELSIUS.multiply(DAY))) {
            return "DEGC-D";
        } else if (unit.equals(MINUTE)) {
            return "MINUTES";
        } else {
            return unit.toString();
        }
    }

    private static HecTime getHecTime(ZonedDateTime zonedDateTime) {
        return new HecTime(zonedDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }
}
