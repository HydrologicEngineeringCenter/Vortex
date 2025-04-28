package mil.army.usace.hec.vortex.util;

import si.uom.NonSI;
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
            // length
            case "kg.m-2", "kg/m^2", "kg m^-2", "kg m-2", "mm", "millimeter", "millimeters", "millimeters h20",
                 "millimeters snow thickness" -> MILLI(METRE);
            case "m", "meter", "metre", "meters" -> METRE;
            case "km" -> KILO(METRE);
            case "1/1000 in" -> INCH.divide(1000);
            case "in", "inch", "inches" -> INCH;
            case "ft", "foot", "feet" -> FOOT;
            case "mile", "mi" -> MILE;
            // flow
            case "m3/s", "cms" -> CUBIC_METRE.divide(SECOND);
            case "cfs" -> CUBIC_FOOT.divide(SECOND);
            // velocity
            case "kg.m-2.s-1", "kg m-2 s-1", "kg/m2s", "mm/s", "mm s-1" -> MILLI(METRE).divide(SECOND);
            case "mm hr^-1", "mm/hr" -> MILLI(METRE).divide(HOUR);
            case "mm/day", "mm/d" -> MILLI(METRE).divide(DAY);
            case "m s-1", "m/s" -> METRE.divide(SECOND);
            case "kph" -> KILO(METRE).divide(HOUR);
            case "in/hr" -> INCH.divide(HOUR);
            case "in/day" -> INCH.divide(DAY);
            case "ft/s", "feet/s", "ft s-1" -> FOOT.divide(SECOND);
            case "mph", "mile/hr", "mile hr^-1" -> MILE.divide(HOUR);
            // temperature
            case "kelvin", "k" -> KELVIN;
            case "celsius", "degrees c", "deg c", "deg_c", "degc", "c" -> CELSIUS;
            case "fahrenheit", "deg f", "deg_f", "degf", "f" -> FAHRENHEIT;
            // degree days
            case "degc-d" -> CELSIUS.multiply(DAY);
            case "degf-d" -> FAHRENHEIT.multiply(DAY);
            // length per degree day
            case "mm/deg-d" -> MILLI(METRE).divide(CELSIUS.multiply(DAY));
            case "in/deg-d" -> INCH.divide(FAHRENHEIT.multiply(DAY));
            // energy (work) per area
            case "j m**-2", "j/m2" -> JOULE.divide(SQUARE_METRE);
            // power per area
            case "watt/m2", "w m-2", "w/m2" -> WATT.divide(SQUARE_METRE);
            case "lang/min" -> JOULE.divide(SQUARE_METRE).multiply(41840).divide(MINUTE);
            // pressure
            case "hpa" -> HECTO(PASCAL);
            case "kpa" -> KILO(PASCAL);
            case "pa" -> PASCAL;
            case "inhg", "in hg", "in-hg" -> NonSI.INCH_OF_MERCURY;
            // time
            case "min" -> MINUTE;
            case "hr", "hour", "hours" -> HOUR;
            // angle
            case "degrees", "degrees_east", "degrees_north" -> USCustomary.DEGREE_ANGLE;
            // area
            case "ac", "acre" -> ACRE;
            case "sqft", "ft2" -> SQUARE_FOOT;
            case "m2" -> SQUARE_METRE;
            case "thou m2" -> SQUARE_METRE.multiply(1000);
            // volume
            case "m3" -> CUBIC_METRE;
            case "thou m3" -> CUBIC_METRE.multiply(1000);
            case "ac-ft" -> ACRE_FOOT;
            // mass
            case "tons" -> TON;
            case "tonnes" -> KILOGRAM.multiply(1000);
            // concentration
            case "mg/l" -> MILLI(GRAM).divide(LITRE);
            // percent
            case "%" -> PERCENT;
            // unspecified/none
            case "unspecif", "undef" -> ONE;
            // attempt to parse units
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
