package mil.army.usace.hec.vortex.convert;

import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.VortexVariable;
import mil.army.usace.hec.vortex.geo.RasterUtils;
import mil.army.usace.hec.vortex.util.UnitUtil;

import javax.measure.Unit;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static javax.measure.MetricPrefix.MILLI;
import static systems.uom.common.USCustomary.INCH;
import static tech.units.indriya.AbstractUnit.ONE;
import static tech.units.indriya.unit.Units.*;

/**
 * Performs common data conversions for use in HEC applications.
 */

public class DataConverter {

    private static final int SECONDS_PER_HOUR = 3600;
    private static final int SECONDS_PER_DAY = 86400;
    private static final float KELVIN_TO_CELSIUS = -273.15f;

    private DataConverter() {
    }

    public static List<VortexGrid> convert(List<VortexGrid> vortexGrids) {
        List<VortexGrid> convertedGrids = new ArrayList<>();
        for (VortexGrid vortexGrid : vortexGrids) {
            VortexGrid convertedGrid = convert(vortexGrid);
            convertedGrids.add(convertedGrid);
        }
        return convertedGrids;
    }

    public static VortexGrid convert(VortexGrid vortexGrid) {
        VortexVariable variable = VortexVariable.fromGrid(vortexGrid);
        Unit<?> units = UnitUtil.parse(vortexGrid.units());

        if (units == null)
            return vortexGrid;

        float[] data = vortexGrid.data();
        float noDataValue = (float) vortexGrid.noDataValue();

        ZonedDateTime startTime = vortexGrid.startTime();
        ZonedDateTime endTime = vortexGrid.endTime();
        Duration interval = vortexGrid.interval();

        String convertedUnits;
        float[] convertedData;

        if (variable == VortexVariable.PRECIPITATION
                && startTime != null && endTime != null && !interval.isZero()
                && (units.equals(MILLI(METRE).divide(SECOND))
                || units.equals(MILLI(METRE).divide(HOUR))
                || units.equals(MILLI(METRE).divide(DAY)))) {
            float conversion = getPrecipRateConversionFactor(interval, units);
            convertedData = RasterUtils.convert(data, conversion, noDataValue);
            convertedUnits = "mm";
        } else if (variable == VortexVariable.PRECIPITATION && units.equals(METRE)) {
            convertedData = RasterUtils.convert(data, 1000, noDataValue);
            convertedUnits = "mm";
        } else if (variable == VortexVariable.HUMIDITY && units.equals(ONE)) {
            convertedData = RasterUtils.convert(data, 100, noDataValue);
            convertedUnits = "%";
        } else if (units.equals(KELVIN)) {
            convertedData = new float[data.length];
            for (int i = 0; i < data.length; i++) {
                float value = data[i];
                convertedData[i] = Float.compare(noDataValue, value) == 0 ? noDataValue : data[i] + KELVIN_TO_CELSIUS;
            }
            convertedUnits = "celsius";
        } else if (units.equals(ONE.divide(INCH.multiply(1000)))) {
            convertedData = RasterUtils.convert(data, 1E-3f, noDataValue);
            convertedUnits = "in";
        } else if (units.equals(PASCAL)) {
            convertedData = RasterUtils.convert(data, 1E-3f, noDataValue);
            convertedUnits = "kpa";
        } else {
            return vortexGrid;
        }

        return VortexGrid.toBuilder(vortexGrid)
                .data(convertedData)
                .units(convertedUnits)
                .build();
    }

    private static float getPrecipRateConversionFactor(Duration interval, Unit<?> units) {
        if (units.equals(MILLI(METRE).divide(SECOND)))
            return interval.getSeconds();

        if (units.equals(MILLI(METRE).divide(HOUR)))
            return (float) interval.getSeconds() / SECONDS_PER_HOUR;

        if (units.equals(MILLI(METRE).divide(DAY)))
            return (float) interval.getSeconds() / SECONDS_PER_DAY;

        return 1;
    }
}
