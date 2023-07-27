package mil.army.usace.hec.vortex.io;

import hec.heclib.dss.DSSPathname;
import hec.heclib.dss.DssDataType;
import hec.heclib.dss.HecTimeSeries;
import hec.heclib.grid.*;
import hec.heclib.util.HecTime;
import hec.heclib.util.HecTimeArray;
import hec.heclib.util.Heclib;
import hec.hecmath.HecMath;
import hec.io.DataContainer;
import hec.io.TimeSeriesContainer;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.VortexPoint;
import mil.army.usace.hec.vortex.util.MatrixUtils;
import mil.army.usace.hec.vortex.util.UnitUtil;
import org.gdal.osr.SpatialReference;

import javax.measure.Unit;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static javax.measure.MetricPrefix.KILO;
import static javax.measure.MetricPrefix.MILLI;
import static systems.uom.common.USCustomary.*;
import static tech.units.indriya.AbstractUnit.ONE;
import static tech.units.indriya.unit.Units.HOUR;
import static tech.units.indriya.unit.Units.*;

public class DssDataWriter extends DataWriter {

    private static final Logger logger = Logger.getLogger(DssDataWriter.class.getName());

    DssDataWriter(Builder builder) {
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

            double noDataValue = grid.noDataValue();
            for (int i = 0; i < data.length; i++) {
                if (Double.compare(data[i], noDataValue) == 0 || Double.isNaN(data[i])) {
                    data[i] = Heclib.UNDEFINED_FLOAT;
                }
            }

            Unit<?> units = UnitUtil.getUnits(grid.units());

            DSSPathname dssPathname = new DSSPathname();
            String cPart = getCPartForGrid(grid.shortName());
            if (cPart.isEmpty()) {
                cPart = getCPartForGrid(grid.description());
            }
            if (cPart.isEmpty()) {
                cPart = getCPartForGrid(grid.fileName());
            }

            dssPathname.setCPart(cPart);

            if (cPart.equals("PRECIPITATION-FREQUENCY")
                    && options.getOrDefault("partF", "").isEmpty()) {
                options.put("partF", grid.description());
            }

            if (cPart.matches("(SWE|SNOW DEPTH)")
                    && options.getOrDefault("dataType", "").isEmpty()
                    && !grid.interval().equals(Duration.ZERO)) {
                options.put("dataType", "PER-AVER");
            }

            if (cPart.equals("PRECIPITATION") && !grid.interval().isZero()
                    && (units.equals(MILLI(METRE).divide(SECOND))
                    || units.equals(MILLI(METRE).divide(HOUR))
                    || units.equals(MILLI(METRE).divide(DAY)))) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm");
                LocalDateTime startTime = LocalDateTime.parse(gridInfo.getStartTime(), formatter);
                LocalDateTime endTime = LocalDateTime.parse(gridInfo.getEndTime(), formatter);
                Duration interval = Duration.between(startTime, endTime);

                float conversion;
                if (units.equals(MILLI(METRE).divide(SECOND))) {
                    conversion = interval.getSeconds();
                } else if (units.equals(MILLI(METRE).divide(HOUR))) {
                    conversion = (float) interval.getSeconds() / SECONDS_PER_HOUR;
                } else if (units.equals(MILLI(METRE).divide(DAY))) {
                    conversion = (float) interval.getSeconds() / SECONDS_PER_DAY;
                } else {
                    conversion = 1;
                }

                for (int i = 0; i < data.length; i++) {
                    if (data[i] == Heclib.UNDEFINED_FLOAT)
                        continue;

                    data[i] *= conversion;
                }

                gridInfo.setDataUnits("MM");
                gridInfo.setDataType(DssDataType.PER_CUM.value());
                
                if (options.containsKey("partF") && options.get("partF").equals("*")) {
                    DSSPathname pathnameIn = new DSSPathname();
                    int status = pathnameIn.setPathname(grid.fullName());
                    if (status == 0) {
                        dssPathname.setFPart(pathnameIn.getFPart());
                    }
                }

                write(data, gridInfo, dssPathname);

            } else if (units.equals(FAHRENHEIT) || units.equals(KELVIN) || units.equals(CELSIUS)) {
                float[] convertedData = new float[data.length];
                if (units.equals(FAHRENHEIT)) {
                    IntStream.range(0, data.length).forEach(i -> convertedData[i] = data[i]);
                    gridInfo.setDataUnits("DEG F");
                } else if (units.equals(KELVIN)){
                    IntStream.range(0, data.length).forEach(i -> convertedData[i] = (float) (data[i] - 273.15));
                    gridInfo.setDataUnits("DEG C");
                } else if (units.equals(CELSIUS)) {
                    IntStream.range(0, data.length).forEach(i -> convertedData[i] = data[i]);
                    gridInfo.setDataUnits("DEG C");
                }

                if (options.containsKey("partF") && options.get("partF").equals("*")) {
                    DSSPathname pathnameIn = new DSSPathname();
                    int status = pathnameIn.setPathname(grid.fullName());
                    if (status == 0) {
                        dssPathname.setFPart(pathnameIn.getFPart());
                    }
                }

                write(convertedData, gridInfo, dssPathname);
            } else if (units.equals(ONE.divide(INCH.multiply(1000)))) {
                float[] convertedData = new float[data.length];
                IntStream.range(0, data.length).forEach(i -> convertedData[i] = data[i] / 1000);

                gridInfo.setDataUnits("IN");

                write(convertedData, gridInfo, dssPathname);
            } else if (units.equals(PASCAL)) {
                float[] convertedData = new float[data.length];
                IntStream.range(0, data.length).forEach(i -> convertedData[i] = data[i] / 1000);

                gridInfo.setDataUnits("KPA");

                write(convertedData, gridInfo, dssPathname);
            } else {
                if (options.containsKey("partF") && options.get("partF").equals("*")) {
                    DSSPathname pathnameIn = new DSSPathname();
                    int status = pathnameIn.setPathname(grid.fullName());
                    if (status == 0) {
                        dssPathname.setFPart(pathnameIn.getFPart());
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

            boolean isRegular = true;
            Duration diff;
            if (startTimes.size() == 1) {
                isRegular = false;
                diff = Duration.ZERO;
            } else {
                diff = Duration.between(startTimes.get(0), startTimes.get(1));
                for (int t = 1; t < startTimes.size(); t++) {
                    if (!Duration.between(startTimes.get(t - 1), startTimes.get(t)).equals(diff)) {
                        isRegular = false;
                        break;
                    }
                }
            }

            double[] values = filtered.stream()
                    .mapToDouble(VortexPoint::data)
                    .toArray();

            String units = filtered.get(0).units();
            DssDataType type = getDssDataType(description, diff);

            DSSPathname pathname = new DSSPathname();
            pathname.setBPart(id);
            String cPart = getCPartForTimeSeries(description, type);
            pathname.setCPart(cPart);

            TimeSeriesContainer tsc = new TimeSeriesContainer();
            tsc.setUnits(units);
            tsc.setType(type.toString());
            tsc.setValues(values);

            if (isRegular) {
                int seconds = (int) diff.abs().getSeconds();
                String ePart = getEPart(seconds);
                pathname.setEPart(ePart);

                HecTime startTime = getHecTime(startTimes.get(0));
                if (type.equals(DssDataType.PER_CUM) || type.equals(DssDataType.PER_AVER)) {
                    startTime.addSeconds(seconds);
                }

                tsc.setStartTime(startTime);
            } else {
                logger.info(() -> "Inconsistent time-interval in record. Data will be stored as irregular interval.");
                pathname.setEPart("Ir-Day");

                int[] times = new int[endTimes.size()];
                for (int i = 0; i < endTimes.size(); i++) {
                    ZonedDateTime zdtEndTime = endTimes.get(i);
                    HecTime endTime = getHecTime(zdtEndTime);
                    times[i] = endTime.value();
                }

                HecTimeArray hecTimeArray = new HecTimeArray(times);
                tsc.setTimes(hecTimeArray);
            }

            tsc.setName(updatePathname(pathname, options).toString());

            HecTimeSeries dssTimeSeries = new HecTimeSeries();
            dssTimeSeries.setDSSFileName(destination.toString());

            int status = dssTimeSeries.write(tsc);

            if (options.getOrDefault("isAccumulate", "false").equalsIgnoreCase("true")
                    && cPart.toLowerCase().contains("precip")) {
                try {
                    if (tsc.getTimes() == null) {
                        int[] times = new int[endTimes.size()];
                        for (int i = 0; i < endTimes.size(); i++) {
                            ZonedDateTime zdtEndTime = endTimes.get(i);
                            HecTime endTime = getHecTime(zdtEndTime);
                            times[i] = endTime.value();
                        }

                        HecTimeArray hecTimeArray = new HecTimeArray(times);
                        tsc.setTimes(hecTimeArray);
                    }
                    HecMath math = HecMath.createInstance(tsc).accumulation();
                    DataContainer container = math.getData();
                    TimeSeriesContainer tscAccum = (TimeSeriesContainer) container;
                    DSSPathname dssPathname = new DSSPathname(tscAccum.getFullName());

                    dssPathname.setCPart("PRECIP-CUM");
                    tscAccum.setType(DssDataType.INST_CUM.toString());

                    tscAccum.setFullName(dssPathname.getPathname());
                    if (dssTimeSeries.write(tscAccum) != 0) {
                        logger.severe("Dss write error");
                    }
                } catch (Exception e) {
                    logger.log(Level.SEVERE, e, e::getMessage);
                }
            }

            dssTimeSeries.done();
            if (status != 0) logger.severe("Dss write error");
        }));
    }

    private static String getCPartForGrid(String shortName){
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
                || desc.contains("var209-6")
                || desc.contains("rainfall")
                || desc.equals("pr")) {
            return "PRECIPITATION";
        } else if (desc.contains("temperature")
                || desc.equals("airtemp")
                || desc.equals("tasmin")
                || desc.equals("tasmax")
                || desc.equals("temp-air")) {
            return "TEMPERATURE";
        } else if ((desc.contains("short") && desc.contains("wave") || desc.contains("solar"))
                && desc.contains("radiation")){
            return "SOLAR RADIATION";
        } else if ((desc.contains("wind")) && (desc.contains("speed"))) {
            return "WINDSPEED";
        } else if (desc.contains("snow") && desc.contains("water") && desc.contains("equivalent")
                || desc.equals("swe")) {
            return "SWE";
        } else if ((desc.contains("snowfall")) && (desc.contains("accumulation"))) {
            return "SNOWFALL ACCUMULATION";
        } else if (desc.contains("albedo")) {
            return "ALBEDO";
        } else if (desc.contains("snow") && desc.contains("depth")) {
            return "SNOW DEPTH";
        } else if (desc.contains("snow") && desc.contains("melt") && desc.contains("runoff")) {
            return "LIQUID WATER";
        } else if (desc.contains("snow") && desc.contains("sublimation")) {
            return "SNOW SUBLIMATION";
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
        } else if (desc.matches("moisture\\s?deficit")){
            return "MOISTURE DEFICIT";
        } else if (desc.matches("impervious\\s?area")){
            return "IMPERVIOUS AREA";
        } else if (desc.equals("percolation") || desc.matches("percolation\\s?rate")){
            return "PERCOLATION";
        } else if (desc.matches("curve\\s?number")){
            return "CURVE NUMBER";
        } else {
            return "";
        }
    }

    private static String getCPartForTimeSeries(String description, DssDataType type){
        String desc;
        if (description != null) {
            desc = description.toLowerCase();
        } else {
            return "";
        }

        if (desc.contains("precipitation")
                || desc.contains("precip")
                || desc.contains("precip") && desc.contains("rate")
                || desc.contains("qpe01h")
                || desc.contains("var209-6")
                || desc.contains("rainfall")
                || desc.contains("pr")) {
            if (type.equals(DssDataType.INST_CUM)) {
                return "PRECIP-CUM";
            } else {
                return "PRECIP-INC";
            }
        } else {
            return getCPartForGrid(description);
        }
    }

    private static DssDataType getDssDataType(String description, Duration interval) {
        String desc = description.toLowerCase();
        if (desc.contains("precipitation") && desc.contains("frequency")) {
            return DssDataType.INST_VAL;
        } else if (desc.contains("precipitation")
                || desc.contains("precip")
                || desc.contains("qpe01h")
                || desc.contains("var209-6")) {
            return DssDataType.PER_CUM;
        } else if (desc.contains("temperature")
                || desc.equals("temp-air")) {
            return DssDataType.INST_VAL;
        } else if ((desc.contains("short") && desc.contains("wave") || desc.contains("solar"))
                && desc.contains("radiation")) {
            return DssDataType.PER_AVER;
        } else if ((desc.contains("wind")) && (desc.contains("speed"))) {
            return DssDataType.INST_VAL;
        } else if (desc.matches("(snow.*water.*equivalent|swe|snow.*depth)")) {
            if (interval.equals(Duration.ZERO)) {
                return DssDataType.INST_VAL;
            } else {
                return DssDataType.PER_AVER;
            }
        } else if (desc.contains("albedo")) {
            return DssDataType.INST_VAL;
        } else {
            return DssDataType.INVAL;
        }
    }

    private static DSSPathname updatePathname(DSSPathname pathnameIn, Map<String, String> options) {
        DSSPathname pathnameOut = new DSSPathname(pathnameIn.getPathname());

        if (options.containsKey("partA") && !options.get("partA").equals("*")) {
            pathnameOut.setAPart(options.get("partA"));
        }
        if (options.containsKey("partB") && !options.get("partB").equals("*")) {
            pathnameOut.setBPart(options.get("partB"));
        }
        if (options.containsKey("partC") && !options.get("partC").equals("*")) {
            pathnameOut.setCPart(options.get("partC"));
        }
        if (options.containsKey("partD") && !options.get("partD").equals("*")) {
            pathnameOut.setDPart(options.get("partD"));
        }
        if (options.containsKey("partE") && !options.get("partE").equals("*")) {
            pathnameOut.setEPart(options.get("partE"));
        }
        if (options.containsKey("partF") && !options.get("partF").equals("*")) {
            pathnameOut.setFPart(options.get("partF"));
        }

        return pathnameOut;
    }

    private static String getUnitsString(Unit<?> unit){
        if (unit.equals(MILLI(METRE))){
            return "MM";
        }
        if (unit.equals(INCH)){
            return "IN";
        }
        if (unit.equals(INCH.divide(HOUR))){
            return "IN/HR";
        }
        if (unit.equals(MILLI(METRE).divide(SECOND))){
            return "MM/S";
        }
        if (unit.equals(MILLI(METRE).divide(HOUR))){
            return "MM/HR";
        }
        if (unit.equals(MILLI(METRE).divide(DAY))){
            return "MM/DAY";
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
        return unit.toString();
    }

    private static String getEPart(int seconds) {
        int minutes = seconds / SECONDS_PER_MINUTE;
        switch (minutes) {
            case 1:
                return "1Minute";
            case 2:
                return "2Minute";
            case 3:
                return "3Minute";
            case 4:
                return "4Minute";
            case 5:
                return "5Minute";
            case 6:
                return "6Minute";
            case 8:
                return "8Minute";
            case 10:
                return "10Minute";
            case 12:
                return "12Minute";
            case 15:
                return "15Minute";
            case 20:
                return "20Minute";
            case 30:
                return "30Minutes";
            case 60:
                return "1Hour";
            case 120:
                return "2Hours";
            case 180:
                return "3Hours";
            case 240:
                return "4Hours";
            case 360:
                return "6Hours";
            case 480:
                return "8Hours";
            case 720:
                return "12Hours";
            case 1440:
                return "1Day";
            case 2880:
                return "2Days";
            case 5760:
                return "3Days";
            case 7200:
                return "4Days";
            case 8640:
                return "6Days";
            case 10080:
                return "1Week";
            case 43200:
                return "1Month";
            case 525600:
                return "1Year";
            default:
                return "0";
        }
    }

    private static GridInfo getGridInfo(VortexGrid grid){
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

    private static HecTime getHecTime(ZonedDateTime zonedDateTime){
        return new HecTime(zonedDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }

    private void write(float[] data, GridInfo gridInfo, DSSPathname pathname){
        if (options.containsKey("units")) {
            String unitString = options.get("units");
            gridInfo.setDataUnits(unitString);
        }

        if (options.containsKey("dataType")) {
            String dataType = options.get("dataType");
            if (dataType.equals("INST-VAL"))
                gridInfo.setDataType(DssDataType.INST_VAL.value());
            if (dataType.equals("PER-AVER"))
                gridInfo.setDataType(DssDataType.PER_AVER.value());
            if (dataType.equals("PER-CUM"))
                gridInfo.setDataType(DssDataType.PER_CUM.value());
            if (dataType.equals("INST-CUM"))
                gridInfo.setDataType(DssDataType.INST_CUM.value());
        }

        GridData gridData = new GridData(data, gridInfo);

        GriddedData griddedData = new GriddedData();
        griddedData.setDSSFileName(destination.toString());

        griddedData.setPathname(updatePathname(pathname, options).toString());

        HecTime startTime = new HecTime(gridInfo.getStartTime());
        HecTime endTime = new HecTime(gridInfo.getEndTime());

        if (endTime.isDefined()) {
            if (gridData.getGridInfo().getDataType() == DssDataType.INST_VAL.value()) {
                griddedData.setGridTime(endTime);
            } else {
                griddedData.setGriddedTimeWindow(startTime, endTime);
            }
        }

        int status = griddedData.storeGriddedData(gridInfo, gridData);
        if (status != 0) {
            System.out.println("DSS write error");
        }
        griddedData.done();
    }

}

