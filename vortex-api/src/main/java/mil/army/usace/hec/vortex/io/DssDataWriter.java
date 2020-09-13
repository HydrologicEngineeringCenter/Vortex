package mil.army.usace.hec.vortex.io;

import hec.data.DataSetIllegalArgumentException;
import hec.data.Interval;
import hec.heclib.dss.DSSPathname;
import hec.heclib.dss.DssDataType;
import hec.heclib.dss.HecTimeSeries;
import hec.heclib.grid.*;
import hec.heclib.util.HecTime;
import hec.io.TimeSeriesContainer;
import mil.army.usace.hec.vortex.Options;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.VortexPoint;
import mil.army.usace.hec.vortex.util.MatrixUtils;
import mil.army.usace.hec.vortex.util.TimeConverter;
import org.gdal.osr.SpatialReference;

import javax.measure.Unit;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static javax.measure.MetricPrefix.*;
import static systems.uom.common.USCustomary.*;
import static tech.units.indriya.AbstractUnit.ONE;
import static tech.units.indriya.unit.Units.HOUR;
import static tech.units.indriya.unit.Units.*;

public class DssDataWriter extends DataWriter {

    private static final Logger logger = Logger.getLogger(DssDataWriter.class.getName());

    DssDataWriter(DataWriterBuilder builder) {
        super(builder);
    }

    private static final int SECONDS_PER_MINUTE = 60;
    private static final int SECONDS_PER_HOUR = 3600;
    private static final int SECONDS_PER_DAY = 86400;

    @Override
    public void write() {
        List<VortexGrid> grids = data.stream()
                .filter(vortexData -> vortexData instanceof VortexGrid)
                .map(vortexData -> (VortexGrid) vortexData)
                .collect(Collectors.toList());

        grids.forEach(grid -> {
            GridInfo gridInfo = getGridInfo(grid);

            float[] data;
            if (grid.dy() < 0){
                data = MatrixUtils.flipArray(grid.data(), grid.nx(), grid.ny());
            } else {
                data = grid.data();
            }

            Unit<?> units = getUnits(grid.units());

            if (units.equals(MILLI(METRE).divide(SECOND))
                    || units.equals(MILLI(METRE).divide(HOUR))
                    || units.equals(MILLI(METRE).divide(DAY))) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm");
                LocalDateTime startTime = LocalDateTime.parse(gridInfo.getStartTime(), formatter);
                LocalDateTime endTime = LocalDateTime.parse(gridInfo.getEndTime(), formatter);
                Duration interval = Duration.between(startTime, endTime);
                float[] convertedData = new float[data.length];
                if (units.equals(MILLI(METRE).divide(SECOND))) {
                    IntStream.range(0, data.length).forEach(i -> convertedData[i] = data[i] * interval.getSeconds());
                } else if (units.equals(MILLI(METRE).divide(HOUR))){
                    IntStream.range(0, data.length).forEach(i -> convertedData[i] = data[i] * interval.getSeconds()/SECONDS_PER_HOUR);
                } else if (units.equals(MILLI(METRE).divide(DAY))){
                    IntStream.range(0, data.length).forEach(i -> convertedData[i] = data[i] * interval.getSeconds()/SECONDS_PER_DAY);
                }

                gridInfo.setDataUnits("MM");
                gridInfo.setDataType(DssDataType.PER_CUM.value());

                DSSPathname dssPathname = new DSSPathname();
                String cPart;
                if (!getCPart(grid.shortName()).isEmpty()){
                    cPart = getCPart(grid.shortName());
                } else {
                    cPart = getCPart(grid.description());
                }
                dssPathname.setCPart(cPart);

                if (options != null) {
                    Map<String, String> parts = options.getOptions();
                    if (parts.containsKey("partF") && parts.get("partF").equals("*")) {
                        DSSPathname pathnameIn = new DSSPathname();
                        int status = pathnameIn.setPathname(grid.fullName());
                        if (status == 0) {
                            dssPathname.setFPart(pathnameIn.getFPart());
                        }
                    }
                    if (parts.containsKey("units")) {
                        String unitString = parts.get("units");
                        gridInfo.setDataUnits(unitString);
                    }
                }

                write(convertedData, gridInfo, dssPathname);

            } else if (units.equals(FAHRENHEIT) || units.equals(KELVIN) || units.equals(CELSIUS)) {
                float[] convertedData = new float[data.length];
                if (units.equals(FAHRENHEIT)) {
                    IntStream.range(0, data.length).forEach(i -> convertedData[i] = (float) ((data[i] - 32.0) * (5.0/9.0)));
                } else if (units.equals(KELVIN)){
                    IntStream.range(0, data.length).forEach(i -> convertedData[i] = (float) (data[i] - 273.15));
                } else if (units.equals(CELSIUS)) {
                    IntStream.range(0, data.length).forEach(i -> convertedData[i] = data[i]);
                }

                gridInfo.setDataType(DssDataType.INST_VAL.value());
                gridInfo.setDataUnits("DEG C");

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm");
                LocalDateTime endTime = LocalDateTime.parse(gridInfo.getStartTime(), formatter);

                HecTime endTimeOut = new HecTime();
                endTimeOut.setXML(endTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

                gridInfo.setGridTimes(endTimeOut, endTimeOut);

                DSSPathname dssPathname = new DSSPathname();
                String cPart;
                if (!getCPart(grid.shortName()).isEmpty()){
                    cPart = getCPart(grid.shortName());
                } else {
                    cPart = getCPart(grid.description());
                }
                dssPathname.setCPart(cPart);

                if (options != null) {
                    Map<String, String> parts = options.getOptions();
                    if (parts.containsKey("partF") && parts.get("partF").equals("*")) {
                        DSSPathname pathnameIn = new DSSPathname();
                        int status = pathnameIn.setPathname(grid.fullName());
                        if (status == 0) {
                            dssPathname.setFPart(pathnameIn.getFPart());
                        }
                    }
                    if (parts.containsKey("units")) {
                        String unitString = parts.get("units");
                        gridInfo.setDataUnits(unitString);
                    }
                }

                write(convertedData, gridInfo, dssPathname);
            } else if (units.equals(ONE.divide(INCH.multiply(1000)))) {
                float[] convertedData = new float[data.length];
                IntStream.range(0, data.length).forEach(i -> convertedData[i] = data[i] / 1000);

                gridInfo.setDataUnits("IN");

                DSSPathname dssPathname = new DSSPathname();
                String cPart;
                if (!getCPart(grid.shortName()).isEmpty()){
                    cPart = getCPart(grid.shortName());
                } else {
                    cPart = getCPart(grid.description());
                }
                dssPathname.setCPart(cPart);

                write(convertedData, gridInfo, dssPathname);
            } else if (units.equals(PASCAL)) {
                float[] convertedData = new float[data.length];
                IntStream.range(0, data.length).forEach(i -> convertedData[i] = data[i] / 1000);

                gridInfo.setDataUnits("KPA");

                DSSPathname dssPathname = new DSSPathname();
                String cPart;
                if (!getCPart(grid.shortName()).isEmpty()){
                    cPart = getCPart(grid.shortName());
                } else {
                    cPart = getCPart(grid.description());
                }
                dssPathname.setCPart(cPart);

                write(convertedData, gridInfo, dssPathname);
            } else {
                DSSPathname dssPathname = new DSSPathname();
                String cPart;
                if (!getCPart(grid.shortName()).isEmpty()){
                    cPart = getCPart(grid.shortName());
                } else {
                    cPart = getCPart(grid.description());
                }
                dssPathname.setCPart(cPart);

                if (options != null) {
                    Map<String, String> parts = options.getOptions();
                    if (parts.containsKey("partF") && parts.get("partF").equals("*")) {
                        DSSPathname pathnameIn = new DSSPathname();
                        int status = pathnameIn.setPathname(grid.fullName());
                        if (status == 0) {
                            dssPathname.setFPart(pathnameIn.getFPart());
                        }
                    }
                    if (parts.containsKey("units")) {
                        String unitString = parts.get("units");
                        gridInfo.setDataUnits(unitString);
                    }
                }

                write(data, gridInfo, dssPathname);
            }
        });

        List<VortexPoint> points = data.stream()
                .filter(vortexData -> vortexData instanceof VortexPoint)
                .map(vortexData -> (VortexPoint) vortexData)
                .collect(Collectors.toList());

        Set<String> ids = points.stream()
                .map(VortexPoint::id)
                .collect(Collectors.toSet());

        Set<String> descriptions = points.stream()
                .map(VortexPoint::description)
                .collect(Collectors.toSet());

        ids.forEach(id -> descriptions.forEach(description -> {
            List<VortexPoint> filtered = points.stream()
                    .filter(point -> point.id().equals(id))
                    .filter(point -> point.description().equals(description))
                    .sorted(Comparator.comparing(VortexPoint::startTime))
                    .collect(Collectors.toList());

            List<ZonedDateTime> startTimes = filtered.stream()
                    .map(VortexPoint::startTime)
                    .sorted()
                    .collect(Collectors.toList());

            List<ZonedDateTime> endTimes = filtered.stream()
                    .map(VortexPoint::endTime)
                    .sorted()
                    .collect(Collectors.toList());

            AtomicBoolean isRegular = new AtomicBoolean(true);
            Duration diff;
            if (startTimes.size() == 1){
                isRegular.set(false);
                diff = Duration.ZERO;
            } else {
                diff = Duration.between(startTimes.get(0), startTimes.get(1));
                for (int t = 1; t < startTimes.size(); t++) {
                    if (!Duration.between(startTimes.get(t - 1), startTimes.get(t)).equals(diff)) {
                        isRegular.set(false);
                        break;
                    }
                }
            }

            double[] values = filtered.stream()
                    .mapToDouble(VortexPoint::data)
                    .toArray();

            DSSPathname pathname = new DSSPathname();
            pathname.setBPart(id);
            pathname.setCPart(getCPart(description));

            String units = filtered.get(0).units();
            DssDataType type = getDssDataType(description);

            TimeSeriesContainer tsc = new TimeSeriesContainer();
            tsc.units = units;
            tsc.type = type.toString();

            if (isRegular.get()){
                Interval interval;
                try {
                    interval = new Interval((int) diff.abs().getSeconds() / SECONDS_PER_MINUTE);
                } catch (DataSetIllegalArgumentException e) {
                    logger.log(Level.SEVERE, "Uncaught exception", e);
                    return;
                }
                pathname.setEPart(interval.getInterval());
                tsc.values = values;
                tsc.numberValues = values.length;
                tsc.startTime = getHecTime(startTimes.get(0)).value();
            } else {
                logger.info(() -> "Inconsistent time-interval in record. Data will be stored as irregular interval.");
                pathname.setEPart("Ir-Day");

                int[] times = new int[endTimes.size()];
                IntStream.range(0, endTimes.size()).forEach(time ->
                        times[time] = getHecTime(endTimes.get(time)).value());
                tsc.times = times;
                tsc.values = values;
                tsc.numberValues = values.length;
            }

            tsc.fullName = updatePathname(pathname, options).getPathname();

            HecTimeSeries dssTimeSeries = new HecTimeSeries();
            dssTimeSeries.setDSSFileName(destination.toString());
            int status = dssTimeSeries.write(tsc);
            dssTimeSeries.done();
            if (status != 0) logger.severe("Dss write error");
        }));
    }

    private static String getCPart(String shortName){
        String desc;
        if (shortName != null) {
            desc = shortName.toLowerCase();
        } else {
            return "";
        }

        if (desc.contains("precipitation") && desc.contains("frequency")) {
            return "PRECIPITATION-FREQUENCY";
        } else if (desc.contains("pressure") && desc.contains("surface")) {
            return "PRESSURE";
        } else if (desc.contains("precipitation")
                    || desc.contains("precip")
                    || desc.contains("precip") && desc.contains("rate")
                    || desc.contains("qpe01h")
                    || desc.contains("rainfall")
                    || desc.contains("pr")) {
            return "PRECIPITATION";
        } else if (desc.contains("temperature")
                || desc.equals("airtemp")
                || desc.equals("tasmin")
                || desc.equals("tasmax")){
            return "TEMPERATURE";
        } else if ((desc.contains("short") && desc.contains("wave") || desc.contains("solar"))
                && desc.contains("radiation")){
            return "SOLAR RADIATION";
        } else if ((desc.contains("wind")) && (desc.contains("speed"))){
            return "WINDSPEED";
        } else if (desc.contains("snow") && desc.contains("water") && desc.contains("equivalent")
                || desc.equals("swe")) {
            return "SWE";
        } else if ((desc.contains("snowfall")) && (desc.contains("accumulation"))) {
            return "SNOWFALL ACCUMLATION";
        } else if (desc.contains("albedo")) {
            return "ALBEDO";
        } else if (desc.equals("cold content")){
                return "COLD CONTENT";
        } else if (desc.equals("cold content ati")){
            return "COLD CONTENT ATI";
        } else if (desc.equals("liquid water")){
            return "LIQUID WATER";
        } else if (desc.equals("meltrate ati")){
            return "MELTRATE ATI";
        } else if (desc.equals("snow depth")){
            return "SNOW DEPTH";
        } else if (desc.equals("snow melt")){
            return "SNOW MELT";
        } else {
            return "";
        }
    }

    private static DssDataType getDssDataType(String description) {
        String desc = description.toLowerCase();
        if (desc.contains("precipitation") && desc.contains("frequency")) {
            return DssDataType.INST_VAL;
        } else if (desc.contains("precipitation")
                || desc.contains("precip") && desc.contains("rate")
                || desc.contains("qpe01h")) {
            return DssDataType.PER_CUM;
        } else if (desc.contains("temperature")
                || desc.equals("temp-air")) {
            return DssDataType.INST_VAL;
        } else if ((desc.contains("short") && desc.contains("wave") || desc.contains("solar"))
                && desc.contains("radiation")) {
            return DssDataType.PER_AVER;
        } else if ((desc.contains("wind")) && (desc.contains("speed"))) {
            return DssDataType.INST_VAL;
        } else if ((desc.contains("snow")) && (desc.contains("water")) && (desc.contains("equivalent"))) {
            return DssDataType.INST_VAL;
        } else if (desc.contains("albedo")) {
            return DssDataType.INST_VAL;
        } else {
            return DssDataType.INVAL;
        }
    }

    private static DSSPathname updatePathname(DSSPathname pathnameIn, Options options) {
        DSSPathname pathnameOut = new DSSPathname(pathnameIn.getPathname());

        if (options != null) {
            Map<String, String> parts = options.getOptions();
            if (parts.containsKey("partA") && !parts.get("partA").equals("*")) {
                pathnameOut.setAPart(parts.get("partA"));
            }
            if (parts.containsKey("partB") && !parts.get("partB").equals("*")) {
                pathnameOut.setBPart(parts.get("partB"));
            }
            if (parts.containsKey("partC") && !parts.get("partC").equals("*")) {
                pathnameOut.setCPart(parts.get("partC"));
            }
            if (parts.containsKey("partD") && !parts.get("partD").equals("*")) {
                pathnameOut.setDPart(parts.get("partD"));
            }
            if (parts.containsKey("partE") && !parts.get("partE").equals("*")) {
                pathnameOut.setEPart(parts.get("partE"));
            }
            if (parts.containsKey("partF") && !parts.get("partF").equals("*")) {
                pathnameOut.setFPart(parts.get("partF"));
            }
        }

        return pathnameOut;
    }

    private static Unit<?> getUnits(String units){
        switch (units.toLowerCase()){
            case "kg.m-2.s-1":
            case "kg/m2s":
            case "mm/s":
                return MILLI(METRE).divide(SECOND);
            case "mm hr^-1":
            case "mm/hr":
                return MILLI(METRE).divide(HOUR);
            case "mm/day":
            case "mm/d":
                return MILLI(METRE).divide(DAY);
            case "kg.m-2":
            case "kg/m^2":
            case "kg m^-2":
            case "mm":
            case "millimeters h20":
                return MILLI(METRE);
            case "in":
            case "inch":
            case "inches":
                return INCH;
            case "1/1000 in":
                return ONE.divide(INCH.multiply(1000));
            case "celsius":
            case "degrees c":
            case "deg c":
            case "c":
                return CELSIUS;
            case "degc-d":
                return CELSIUS.multiply(DAY);
            case "fahrenheit":
            case "degf":
            case "deg f":
            case "f":
                return FAHRENHEIT;
            case "kelvin":
            case "k":
                return KELVIN;
            case "watt/m2":
                return WATT.divide(SQUARE_METRE);
            case "J m**-2":
                return JOULE.divide(SQUARE_METRE);
            case "kph":
                return KILO(METRE).divide(HOUR);
            case "%":
                return PERCENT;
            case "hpa":
                return HECTO(PASCAL);
            case "pa":
                return PASCAL;
            case "m":
                return METRE;
            default:
                return ONE;
        }
    }

    private static String getUnitsString(Unit<?> unit){
        if (unit.equals(MILLI(METRE))){
            return "MM";
        }
        if (unit.equals(INCH)){
            return "IN";
        }
        if (unit.equals(MILLI(METRE).divide(SECOND))){
            return "MM/S";
        }
        if (unit.equals(CUBIC_METRE.divide(SECOND))){
            return "M3/S";
        }
        if (unit.equals(CUBIC_FOOT.divide(SECOND))){
            return "CFS";
        }
        if (unit.equals(METRE)){
            return "M";
        }
        if (unit.equals(FOOT)){
            return "FT";
        }
        if (unit.equals(CELSIUS)){
            return "DEG C";
        }
        if (unit.equals(FAHRENHEIT)){
            return "DEG F";
        }
        if (unit.equals(WATT.divide(SQUARE_METRE))){
            return "WATT/M2";
        }
        if (unit.equals(KILOMETRE_PER_HOUR)){
            return "KPH";
        }
        if (unit.equals(METRE_PER_SECOND)){
            return "M/S";
        }
        if (unit.equals(MILE_PER_HOUR)){
            return "MPH";
        }
        if (unit.equals(FOOT_PER_SECOND)){
            return "FT/S";
        }
        if (unit.equals(KILO(PASCAL))){
            return "KPA";
        }
        if (unit.equals(PASCAL)) {
            return "PA";
        }
        if (unit.equals(PERCENT)){
            return "%";
        }
        if (unit.equals(KILO(METRE))){
            return "KM";
        }
        if (unit.equals(MILE)){
            return "MILE";
        }
        if (unit.equals(ONE)){
            return "UNSPECIF";
        }
        if (unit.equals(TON)){
            return "TONS";
        }
        if (unit.equals(MILLI(GRAM).divide(LITRE))){
            return "MG/L";
        }
        if (unit.equals(CELSIUS.multiply(DAY))){
            return "DEGC-D";
        }
        return "";
    }

    private static GridInfo getGridInfo(VortexGrid grid){
        GridInfo gridInfo;

        SpatialReference srs = new SpatialReference(grid.wkt());
        String crsName;
        if (srs.IsProjected() == 1){
            crsName = srs.GetAttrValue("projcs");
        } else {
            crsName = srs.GetAttrValue("geogcs");
        }

        if (crsName.toLowerCase().contains("albers")){

            AlbersInfo albersInfo = new AlbersInfo();
            albersInfo.setCoordOfGridCellZero(0, 0);

            String datum = srs.GetAttrValue("geogcs");
            if (datum.contains("83")){
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

            albersInfo.setProjectionUnits("Meters");

            gridInfo = albersInfo;
        } else {
            SpecifiedGridInfo specifiedGridInfo = new SpecifiedGridInfo();
            specifiedGridInfo.setSpatialReference(crsName, grid.wkt(), 0, 0);
            gridInfo = specifiedGridInfo;
        }

        double llx = grid.originX();
        double lly;
        if (grid.dy() < 0){
            lly = grid.originY() + grid.dy() * grid.ny();
        } else {
            lly = grid.originY();
        }
        double dx = grid.dx();
        double dy = grid.dy();
        float cellSize = (float) ((Math.abs(dx) + Math.abs(dy)) / 2.0);
        int minX = (int) Math.round(llx/cellSize);
        int minY = (int) Math.round(lly/cellSize);

        gridInfo.setCellInfo(minX, minY, grid.nx(), grid.ny(), cellSize);

        Unit<?> units = getUnits(grid.units());
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

        gridInfo.setGridTimes(getHecTime(startTime), getHecTime(endTime));

        return gridInfo;
    }

    private static HecTime getStartTime(GridInfo gridInfo){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm");
        LocalDateTime date;
        try {
            date = LocalDateTime.parse(gridInfo.getStartTime(), formatter);
        } catch (DateTimeParseException e) {
            return null;
        }
        HecTime time = new HecTime();
        time.setXML(date.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return time;
    }
    private static HecTime getEndTime(GridInfo gridInfo){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm");
        LocalDateTime date;
        try {
            date = LocalDateTime.parse(gridInfo.getEndTime(), formatter);
        } catch (DateTimeParseException e) {
            return null;
        }
        HecTime time = new HecTime();
        time.setXML(date.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return time;
    }

    private static HecTime getHecTime(ZonedDateTime zonedDateTime){
        HecTime time = new HecTime();
        time.setXML(zonedDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return time;
    }

    private void write(float[] data, GridInfo info, DSSPathname pathname){
        GridData gridData = new GridData(data, info);

        GriddedData griddedData = new GriddedData();
        griddedData.setDSSFileName(destination.toString());

        DSSPathname updatedPathname = updatePathname(pathname, options);

        HecTime time0 = getStartTime(gridData.getGridInfo());
        HecTime time1 = getEndTime(gridData.getGridInfo());
        if (time0 != null && time1 != null) {
            ZonedDateTime startTime = TimeConverter.toZonedDateTime(time0);

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("ddMMMyyyy");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HHmm");

            if (gridData.getGridInfo().getDataType() != DssDataType.INST_VAL.value()) {
                String dPart = String.format("%s:%s", dateFormatter.format(startTime), timeFormatter.format(startTime));
                updatedPathname.setDPart(dPart);

                ZonedDateTime endTime = TimeConverter.toZonedDateTime(time1);
                String ePart;
                if (endTime.getHour() == 0 && endTime.getMinute() == 0) {
                    ZonedDateTime previous = endTime.minusDays(1);
                    ePart = String.format("%s:%04d", dateFormatter.format(previous), 2400);
                } else {
                    ePart = String.format("%s:%s", dateFormatter.format(endTime), timeFormatter.format(endTime));
                }

                updatedPathname.setEPart(ePart);
            } else {
                String dPart;
                if (startTime.getHour() == 0 && startTime.getMinute() == 0) {
                    ZonedDateTime previous = startTime.minusDays(1);
                    dPart = String.format("%s:%04d", dateFormatter.format(previous), 2400);
                } else {
                    dPart = String.format("%s:%s", dateFormatter.format(startTime), timeFormatter.format(startTime));
                }

                updatedPathname.setDPart(dPart);
            }
        }

        griddedData.setPathname(updatedPathname.getPathname());

        int status = griddedData.storeGriddedData(info, gridData);
        if (status != 0) {
            logger.log(Level.SEVERE, () -> "DSS write error");
        }
        griddedData.done();
    }

}

