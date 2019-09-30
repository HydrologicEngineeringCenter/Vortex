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
import org.gdal.osr.SpatialReference;

import javax.measure.IncommensurableException;
import javax.measure.Unit;
import javax.measure.UnitConverter;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static systems.uom.common.USCustomary.*;
import static tech.units.indriya.AbstractUnit.ONE;
import static tech.units.indriya.unit.Units.*;
import static tec.units.indriya.unit.MetricPrefix.*;
import static tech.units.indriya.unit.Units.HOUR;

public class DssDataWriter extends DataWriter {

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
                dssPathname.setCPart(getCPart(grid.shortName()));

                write(convertedData, gridInfo, dssPathname);

            } else if (units.equals(FAHRENHEIT) || units.equals(KELVIN)) {
                UnitConverter converter;
                try {
                    converter = units.getConverterToAny(CELSIUS);
                } catch (IncommensurableException e) {
                    e.printStackTrace();
                    return;
                }
                float[] convertedData = new float[data.length];
                IntStream.range(0, data.length).forEach(i -> convertedData[i] =
                        Double.valueOf(converter.convert(Float.valueOf(data[i]).doubleValue())).floatValue());

                gridInfo.setDataType(DssDataType.INST_VAL.value());
                gridInfo.setDataUnits("DEG C");

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm");
                LocalDateTime endTime = LocalDateTime.parse(gridInfo.getStartTime(), formatter);

                HecTime endTimeOut = new HecTime();
                endTimeOut.setXML(endTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

                gridInfo.setGridTimes(endTimeOut, endTimeOut);

                DSSPathname dssPathname = new DSSPathname();
                dssPathname.setCPart(getCPart(grid.shortName()));

                write(convertedData, gridInfo, dssPathname);
            } else {
                DSSPathname dssPathname = new DSSPathname();
                dssPathname.setCPart(getCPart(grid.shortName()));
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
                    .sorted(Comparator.comparing(VortexPoint::endTime))
                    .collect(Collectors.toList());

            List<ZonedDateTime> dates = filtered.stream()
                    .map(VortexPoint::endTime)
                    .collect(Collectors.toList());

            AtomicBoolean isRegular = new AtomicBoolean(false);
            Duration diff = Duration.between(dates.get(0), dates.get(1));
            IntStream.range(1 , dates.size()).forEach(date -> {
                if(Duration.between(dates.get(date - 1), dates.get(date)).equals(diff)){
                    isRegular.set(true);
                } else {
                    isRegular.set(false);
                }
            });

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
            tsc.dataType = type.value();

            if (isRegular.get()){
                Interval interval;
                try {
                    interval = new Interval((int) diff.abs().getSeconds() / SECONDS_PER_MINUTE);
                } catch (DataSetIllegalArgumentException e) {
                    e.printStackTrace();
                    return;
                }
                pathname.setEPart(interval.getInterval());
                tsc.values = values;
                tsc.startTime = getHecTime(dates.get(0)).value();
            } else {
                pathname.setEPart("Ir-Month");

                int[] times = new int[dates.size()];
                IntStream.range(0, dates.size()).forEach(date ->
                        times[date] = getHecTime(dates.get(date)).value());
                tsc.times = times;
                tsc.values = values;
            }

            tsc.fullName = updatePathname(pathname, options).getPathname();

            HecTimeSeries dssTimeSeries = new HecTimeSeries();
            dssTimeSeries.setDSSFileName(destination.toString());
            int status = dssTimeSeries.write(tsc);
            dssTimeSeries.done();
            if (status != 0) System.out.println("Dss write error");
        }));
    }

    private static String getCPart(String shortName){
        String desc;
        if (shortName != null) {
            desc = shortName.toLowerCase();
        } else {
            return "";
        }

        if (desc.contains("precipitation")
                || desc.contains("precip")
                || desc.contains("precip") && desc.contains("rate")
                || desc.contains("qpe01h")
                || desc.contains("rainfall")) {
            return "PRECIPITATION";
        } else if (desc.contains("temperature")){
            return "TEMPERATURE";
        } else if ((desc.contains("short") && desc.contains("wave") || desc.contains("solar"))
                && desc.contains("radiation")){
            return "SOLAR RADIATION";
        } else if ((desc.contains("wind")) && (desc.contains("speed"))){
            return "WINDSPEED";
        } else if ((desc.contains("snow")) && (desc.contains("water")) && (desc.contains("equivalent"))){
            return "SWE";
        } else if (desc.contains("albedo")){
            return "ALBEDO";
        } else {
            return "";
        }
    }

    private static DssDataType getDssDataType(String description){
        String desc = description.toLowerCase();
        if (desc.contains("precipitation")
                || desc.contains("precip") && desc.contains("rate")
                || desc.contains("qpe01h")) {
            return DssDataType.PER_CUM;
        } else if (desc.contains("temperature")){
            return DssDataType.INST_VAL;
        } else if ((desc.contains("short") && desc.contains("wave") || desc.contains("solar"))
                && desc.contains("radiation")){
            return DssDataType.PER_AVER;
        } else if ((desc.contains("wind")) && (desc.contains("speed"))){
            return DssDataType.INST_VAL;
        } else if ((desc.contains("snow")) && (desc.contains("water")) && (desc.contains("equivalent"))){
            return DssDataType.INST_VAL;
        } else if (desc.contains("albedo")){
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
            if (parts.containsKey("partD") && !parts.get("partC").equals("*")) {
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
                return MILLI(METRE).divide(DAY);
            case "kg.m-2":
            case "kg/m^2":
            case "mm":
                return MILLI(METRE);
            case "in":
            case "inch":
            case "inches":
                return INCH;
            case "celsius":
            case "degrees c":
            case "deg c":
            case "c":
                return CELSIUS;
            case "fahrenheit":
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
            case "hPa":
                return HECTO(PASCAL);
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
        LocalDateTime date = LocalDateTime.parse(gridInfo.getStartTime(), formatter);
        HecTime time = new HecTime();
        time.setXML(date.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return time;
    }
    private static HecTime getEndTime(GridInfo gridInfo){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm");
        LocalDateTime date = LocalDateTime.parse(gridInfo.getEndTime(), formatter);
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
        griddedData.setPathname(updatePathname(pathname, options).getPathname());
        HecTime startTime = getStartTime(gridData.getGridInfo());
        HecTime endTime = getEndTime(gridData.getGridInfo());

        if (gridData.getGridInfo().getDataType() == DssDataType.INST_VAL.value()){
            griddedData.setGridTime(endTime);
        } else {
            griddedData.setGriddedTimeWindow(startTime, endTime);
        }

        int status = griddedData.storeGriddedData(info, gridData);
        if (status != 0) {
            System.out.println("DSS write error");
        }
        griddedData.done();
    }

}

