package mil.army.usace.hec.vortex.util;

import javax.measure.Unit;

import static javax.measure.MetricPrefix.*;
import static systems.uom.common.USCustomary.FAHRENHEIT;
import static systems.uom.common.USCustomary.INCH;
import static tech.units.indriya.AbstractUnit.ONE;
import static tech.units.indriya.unit.Units.*;

public class UnitUtil {
    public static Unit<?> getUnits(String units) {
        return switch (units.toLowerCase()) {
            case "kg.m-2.s-1", "kg m-2 s-1", "kg/m2s", "mm/s", "mm s-1" -> MILLI(METRE).divide(SECOND);
            case "mm hr^-1", "mm/hr" -> MILLI(METRE).divide(HOUR);
            case "mm/day", "mm/d" -> MILLI(METRE).divide(DAY);
            case "kg.m-2", "kg/m^2", "kg m^-2", "kg m-2", "mm", "millimeters h20", "millimeters snow thickness" ->
                    MILLI(METRE);
            case "in", "inch", "inches" -> INCH;
            case "1/1000 in" -> ONE.divide(INCH.multiply(1000));
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
            case "pa" -> PASCAL;
            case "m" -> METRE;
            case "min" -> MINUTE;
            default -> ONE;
        };
    }
}
