package mil.army.usace.hec.vortex.util;

import systems.uom.common.USCustomary;
import tech.units.indriya.format.SimpleUnitFormat;

import javax.measure.Unit;
import javax.measure.format.MeasurementParseException;
import javax.measure.format.UnitFormat;
import java.util.Optional;

import static javax.measure.MetricPrefix.*;
import static systems.uom.common.USCustomary.*;
import static tech.units.indriya.AbstractUnit.ONE;
import static tech.units.indriya.unit.Units.HOUR;
import static tech.units.indriya.unit.Units.MINUTE;
import static tech.units.indriya.unit.Units.*;

public class UnitUtil {
    static {
        USCustomary.getInstance();
    }

    private UnitUtil() {
        // Utility Class
    }

    public static Unit<?> parse(String units) {
        if (units == null) {
            return ONE;
        }

        return switch (units.toLowerCase()) {
            case "kg.m-2.s-1", "kg m-2 s-1", "kg/m2s", "mm/s", "mm s-1" -> MILLI(METRE).divide(SECOND);
            case "mm hr^-1", "mm/hr" -> MILLI(METRE).divide(HOUR);
            case "mm/day", "mm/d" -> MILLI(METRE).divide(DAY);
            case "kg.m-2", "kg/m^2", "kg m^-2", "kg m-2", "mm", "millimeter", "millimeters", "millimeters h20",
                 "millimeters snow thickness" -> MILLI(METRE);
            case "in", "inch", "inches" -> INCH;
            case "ft", "foot", "feet" -> FOOT;
            case "1/1000 in" -> INCH.divide(1000);
            case "in/hr" -> INCH.divide(HOUR);
            case "celsius", "degrees c", "deg c", "deg_c", "degc", "c" -> CELSIUS;
            case "degc-d" -> CELSIUS.multiply(DAY);
            case "degf-d" -> FAHRENHEIT.multiply(DAY);
            case "fahrenheit", "deg f", "deg_f", "degf", "f" -> FAHRENHEIT;
            case "kelvin", "k" -> KELVIN;
            case "watt/m2", "w m-2" -> WATT.divide(SQUARE_METRE);
            case "j m**-2" -> JOULE.divide(SQUARE_METRE);
            case "kph" -> KILO(METRE).divide(HOUR);
            case "m s-1" -> METRE.divide(SECOND);
            case "%" -> PERCENT;
            case "hpa" -> HECTO(PASCAL);
            case "kpa" -> KILO(PASCAL);
            case "pa" -> PASCAL;
            case "m", "meter", "metre", "meters" -> METRE;
            case "min" -> MINUTE;
            case "km" -> KILO(METRE);
            case "degrees", "degrees_east", "degrees_north" -> USCustomary.DEGREE_ANGLE;
            case "hr" -> HOUR;
            case "ac", "acre" -> ACRE;
            case "sqft", "ft2" -> SQUARE_FOOT;
            default -> parseSimpleUnitFormat(units);
        };
    }

    public static boolean equals(String units1, String units2) {
        return Optional.ofNullable(units1)
                .map(u -> u.equalsIgnoreCase(units2))
                .orElse(units2 == null);
    }

    private static Unit<?> parseSimpleUnitFormat(String units) {
        if (units == null || units.isBlank()) {
            return ONE;
        }

        try {
            UnitFormat format = SimpleUnitFormat.getInstance();
            return format.parse(units);
        } catch (MeasurementParseException mpe) {
            return ONE;
        }
    }
}
