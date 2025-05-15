package mil.army.usace.hec.vortex.io;

import hec.heclib.dss.DSSPathname;
import hec.heclib.dss.DssDataType;
import hec.heclib.dss.HecTimeSeries;
import hec.heclib.grid.GridData;
import hec.heclib.grid.GridInfo;
import hec.heclib.grid.GriddedData;
import hec.heclib.util.HecTime;
import hec.heclib.util.HecTimeArray;
import hec.hecmath.HecMath;
import hec.io.DataContainer;
import hec.io.TimeSeriesContainer;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.VortexPoint;
import mil.army.usace.hec.vortex.VortexVariable;
import mil.army.usace.hec.vortex.convert.DataConverter;
import mil.army.usace.hec.vortex.geo.RasterUtils;
import mil.army.usace.hec.vortex.geo.ZonalStatistics;
import mil.army.usace.hec.vortex.util.DssUtil;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static hec.heclib.util.Heclib.UNDEFINED_FLOAT;
import static mil.army.usace.hec.vortex.VortexVariable.*;

class DssDataWriter extends DataWriter {

    private static final Logger logger = Logger.getLogger(DssDataWriter.class.getName());

    DssDataWriter(Builder builder) {
        super(builder);
    }

    @Override
    public void write() {
        List<VortexGrid> grids = data.stream()
                .filter(VortexGrid.class::isInstance)
                .map(VortexGrid.class::cast)
                .map(DataConverter::convert)
                .toList();

        for (VortexGrid grid : grids) {
            GridInfo gridInfo = DssUtil.getGridInfo(grid);

            float[] data;
            if (grid.dy() < 0) {
                data = RasterUtils.flipVertically(grid.data(), grid.nx());
            } else {
                data = grid.data();
            }

            double noDataValue = grid.noDataValue();
            for (int i = 0; i < data.length; i++) {
                float value = data[i];
                if (Double.compare(value, noDataValue) == 0 || Double.isNaN(value) || Double.isInfinite(value)) {
                    data[i] = UNDEFINED_FLOAT;
                }
            }

            DSSPathname dssPathname = new DSSPathname();

            String cPart = getCPart(grid);

            dssPathname.setCPart(cPart);

            if (cPart.equals("PRECIPITATION-FREQUENCY")
                    && options.getOrDefault("partF", "").isEmpty()) {
                options.put("partF", grid.description());
            }

            if (!cPart.equals("PRECIPITATION") && options.get("dataType") == null) {
                if (!Duration.ZERO.equals(grid.interval()))
                    options.put("dataType", "PER-AVER");

                if (grid.interval() == null || Duration.ZERO.equals(grid.interval()))
                    options.put("dataType", "INST-VAL");
            }

            if (options.containsKey("partF") && options.get("partF").equals("*")) {
                DSSPathname pathnameIn = new DSSPathname();
                int status = pathnameIn.setPathname(grid.fullName());
                if (status == 0) {
                    dssPathname.setFPart(pathnameIn.getFPart());
                }
            }

            write(data, gridInfo, dssPathname);

        }

        List<VortexPoint> points = data.stream()
                .filter(VortexPoint.class::isInstance)
                .map(VortexPoint.class::cast)
                .toList();

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
                    .toList();

            VortexPoints vortexPoints = new VortexPoints(filtered);

            List<TimeSeriesContainer> tscs = vortexPoints.getTscs();
            writeDssTimeSeries(tscs);
        }));
    }

    private void writeDssTimeSeries(List<TimeSeriesContainer> tscs){
        HecTimeSeries dssTimeSeries = new HecTimeSeries();
        dssTimeSeries.setDSSFileName(destination.toString());
        for (TimeSeriesContainer tsc : tscs) {
            int status = dssTimeSeries.write(tsc);
            if (status != 0) logger.severe("Dss write error");
        }

        dssTimeSeries.done();
    }

    private TimeSeriesContainer computePrecipAccum(TimeSeriesContainer tsc, List<ZonedDateTime> endTimes) {
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

            tscAccum.setFullName(dssPathname.toString());

            return tscAccum;
        } catch (Exception e) {
            logger.log(Level.SEVERE, e, e::getMessage);
        }

        return new TimeSeriesContainer();
    }

    private static String getCPart(VortexGrid vortexGrid) {
        String cPartFromShortName = getCPartForGrid(vortexGrid.shortName());
        if (!cPartFromShortName.isBlank() && isStandardCPart(cPartFromShortName))
            return cPartFromShortName;

        String cPartFromDescription = getCPartForGrid(vortexGrid.description());
        if (!cPartFromDescription.isBlank() && isStandardCPart(cPartFromDescription))
            return cPartFromDescription;

        String cPartFromFileName = getCPartForGrid(vortexGrid.fileName());
        if (!cPartFromFileName.isBlank() && isStandardCPart(cPartFromFileName))
            return cPartFromFileName;

        return cPartFromShortName;
    }

    private static String getCPartForGrid(String description) {
        if (description == null)
            return "";

        VortexVariable vortexVariable = VortexVariable.fromName(description);
        if (vortexVariable != VortexVariable.UNDEFINED)
            return vortexVariable.getDssCPart();

        if (!description.isEmpty())
            return description.toUpperCase();

        return "";
    }

    private static boolean isStandardCPart(String cPart) {
        return Arrays.stream(VortexVariable.values())
                .map(VortexVariable::getDssCPart)
                .anyMatch(s -> s.equals(cPart));
    }

    private static String getCPartForTimeSeries(String description, DssDataType type) {
        if (description == null)
            return "";

        VortexVariable vortexVariable = VortexVariable.fromName(description);

        if (vortexVariable == PRECIPITATION) {
            if (type.equals(DssDataType.INST_CUM)) {
                return "PRECIP-CUM";
            } else {
                return "PRECIP-INC";
            }
        }

        return getCPartForGrid(description);
    }

    private static DssDataType getDssDataType(String description, Duration interval) {
        VortexVariable variable = VortexVariable.fromName(description);
        if (variable == PRECIPITATION)
            return DssDataType.PER_CUM;
        if (variable == TEMPERATURE)
            return DssDataType.INST_VAL;
        if (variable == SHORTWAVE_RADIATION)
            return DssDataType.PER_AVER;
        if (variable == WINDSPEED)
            return DssDataType.INST_VAL;
        if (variable == SNOW_WATER_EQUIVALENT) {
            if (interval.equals(Duration.ZERO)) {
                return DssDataType.INST_VAL;
            } else {
                return DssDataType.PER_AVER;
            }
        }
        if (variable == PRECIPITATION_FREQUENCY) {
            return DssDataType.INST_VAL;
        } else if (variable == ALBEDO) {
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

    private static HecTime getHecTime(ZonedDateTime zonedDateTime) {
        return new HecTime(zonedDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }

    private void write(float[] data, GridInfo gridInfo, DSSPathname pathname) {
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

    private class VortexPoints {
        private final List<VortexPoint> vortexPoints = new ArrayList<>();

        private final String id;
        private final String units;
        private final String description;

        private VortexPoints(List<VortexPoint> points) {
            vortexPoints.addAll(points);

            VortexPoint point0 = points.get(0);
            id = point0.id();
            units = point0.units();
            description = point0.description();
        }

        private List<TimeSeriesContainer> getTscs() {

            List<ZonedDateTime> startTimes = vortexPoints.stream()
                    .map(VortexPoint::startTime)
                    .sorted()
                    .toList();

            List<ZonedDateTime> endTimes = vortexPoints.stream()
                    .map(VortexPoint::endTime)
                    .sorted()
                    .toList();

            List<ZonalStatistics> zonalStatistics = vortexPoints.stream()
                    .map(VortexPoint::getZonalStatistics)
                    .toList();

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

            DssDataType type = getDssDataType(description, diff);

            List<TimeSeriesContainer> tscs = new ArrayList<>();

            if (options.getOrDefault("Average", "").equalsIgnoreCase("true")) {
                double[] averages = zonalStatistics.stream()
                        .mapToDouble(ZonalStatistics::getAverage)
                        .toArray();

                TimeSeriesContainer tscAvg = initTsc("", averages, type);

                tscs.add(tscAvg);
            }

            if (options.getOrDefault("Min", "").equalsIgnoreCase("true")) {
                double[] mins = zonalStatistics.stream()
                        .mapToDouble(ZonalStatistics::getMin)
                        .toArray();

                TimeSeriesContainer tscMin = initTsc("-MIN", mins, type);

                tscs.add(tscMin);
            }

            if (options.getOrDefault("Max", "").equalsIgnoreCase("true")) {
                double[] maxes = zonalStatistics.stream()
                        .mapToDouble(ZonalStatistics::getMax)
                        .toArray();

                TimeSeriesContainer tscMax = initTsc("-MAX", maxes, type);

                tscs.add(tscMax);
            }

            if (options.getOrDefault("Median", "").equalsIgnoreCase("true")) {
                double[] medians = zonalStatistics.stream()
                        .mapToDouble(ZonalStatistics::getMedian)
                        .toArray();

                TimeSeriesContainer tscMedian = initTsc("-MEDIAN", medians, type);

                tscs.add(tscMedian);
            }

            if (options.getOrDefault("1Q", "").equalsIgnoreCase("true")) {
                double[] firstQuartiles = zonalStatistics.stream()
                        .mapToDouble(ZonalStatistics::getFirstQuartile)
                        .toArray();

                TimeSeriesContainer tsc1Q = initTsc("-Q1", firstQuartiles, type);

                tscs.add(tsc1Q);
            }

            if (options.getOrDefault("3Q", "").equalsIgnoreCase("true")) {
                double[] thirdQuartiles = zonalStatistics.stream()
                        .mapToDouble(ZonalStatistics::getThirdQuartile)
                        .toArray();

                TimeSeriesContainer tsc3Q = initTsc("-Q3", thirdQuartiles, type);

                tscs.add(tsc3Q);
            }

            if (options.getOrDefault("Pct>0", "").equalsIgnoreCase("true")) {
                double[] pctCellsGreaterThanZero = zonalStatistics.stream()
                        .mapToDouble(ZonalStatistics::getPctCellsGreaterThanZero)
                        .toArray();

                TimeSeriesContainer tscGreaterThanZero = initTsc("-PCT>0", pctCellsGreaterThanZero, type);
                tscGreaterThanZero.setUnits("%");

                tscs.add(tscGreaterThanZero);
            }

            if (options.getOrDefault("Pct>1Q", "").equalsIgnoreCase("true")) {
                double[] pctCellsGreaterThan1Q = zonalStatistics.stream()
                        .mapToDouble(ZonalStatistics::getPctCellsGreaterThanFirstQuartile)
                        .toArray();

                TimeSeriesContainer tscGreaterThan1Q = initTsc("-PCT>Q1", pctCellsGreaterThan1Q, type);
                tscGreaterThan1Q.setUnits("%");

                tscs.add(tscGreaterThan1Q);
            }

            // Set times for each TimeSeriesContainer
            for (TimeSeriesContainer tsc : tscs) {
                if (isRegular) {
                    setRegularTscTimes(tsc, diff, startTimes.get(0), type);
                } else {
                    setIrregularTscTimes(tsc, endTimes);
                }
            }

            // Add an accumulation cumulative precipitation TimeSeriesContainer
            if (options.getOrDefault("isAccumulate", "false").equalsIgnoreCase("true")) {
                tscs.stream()
                        .filter(tsc -> tsc.getFullName().split("/", 8)[3].equalsIgnoreCase("PRECIP-INC"))
                        .findAny()
                        .ifPresent(tsc -> tscs.add(computePrecipAccum(tsc, endTimes)));
            }

            // Set pathname for each TimeSeriesContainer
            for (TimeSeriesContainer tsc : tscs) {
                DSSPathname dssPathname = new DSSPathname(tsc.getFullName());
                dssPathname.setBPart(id);
                DSSPathname updated = updatePathname(dssPathname, options);
                tsc.setName(updated.toString());
            }

            return tscs;
        }

        private TimeSeriesContainer initTsc(String cPartSuffix, double[] values, DssDataType type) {
            TimeSeriesContainer tsc = new TimeSeriesContainer();
            tsc.setUnits(units);
            tsc.setType(type.toString());
            tsc.setValues(values);

            String cPart = getCPartForTimeSeries(description, type) + cPartSuffix;
            DSSPathname dssPathname = new DSSPathname(tsc.getFullName());
            dssPathname.setCPart(cPart);

            tsc.setName(dssPathname.toString());

            return tsc;
        }

        private void setRegularTscTimes(TimeSeriesContainer tsc, Duration diff, ZonedDateTime zdtStartTime, DssDataType type) {
            int seconds = (int) diff.abs().getSeconds();
            String ePart = DssUtil.getEPart(seconds);
            DSSPathname dssPathname = new DSSPathname(tsc.getFullName());
            dssPathname.setEPart(ePart);

            tsc.setName(dssPathname.toString());

            HecTime startTime = getHecTime(zdtStartTime);
            if (type.equals(DssDataType.PER_CUM) || type.equals(DssDataType.PER_AVER)) {
                startTime.addSeconds(seconds);
            }

            tsc.setStartTime(startTime);
        }

        private void setIrregularTscTimes(TimeSeriesContainer tsc, List<ZonedDateTime> endTimes) {
            logger.info(() -> "Inconsistent time-interval in record. Data will be stored as irregular interval.");

            DSSPathname dssPathname = new DSSPathname(tsc.getFullName());
            dssPathname.setEPart("Ir-Day");
            tsc.setName(dssPathname.toString());

            int[] times = new int[endTimes.size()];
            for (int i = 0; i < endTimes.size(); i++) {
                ZonedDateTime zdtEndTime = endTimes.get(i);
                HecTime endTime = getHecTime(zdtEndTime);
                times[i] = endTime.value();
            }

            HecTimeArray hecTimeArray = new HecTimeArray(times);
            tsc.setTimes(hecTimeArray);
        }
    }
}

