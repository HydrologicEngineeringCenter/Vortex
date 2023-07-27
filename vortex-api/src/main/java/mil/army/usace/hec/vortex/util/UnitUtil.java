package mil.army.usace.hec.vortex.util;

import javax.measure.Unit;

import static javax.measure.MetricPrefix.*;
import static systems.uom.common.USCustomary.*;
import static tech.units.indriya.AbstractUnit.ONE;
import static tech.units.indriya.unit.Units.*;
import static tech.units.indriya.unit.Units.DAY;
import static tech.units.indriya.unit.Units.HOUR;

public class UnitUtil {
    public static Unit<?> getUnits(String units) {
        switch (units.toLowerCase()) {
            case "kg.m-2.s-1":
            case "kg m-2 s-1":
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
            case "kg m-2":
            case "mm":
            case "millimeters h20":
            case "millimeters snow thickness":
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
            case "deg_c":
            case "degc":
            case "c":
                return CELSIUS;
            case "degc-d":
                return CELSIUS.multiply(DAY);
            case "fahrenheit":
            case "deg f":
            case "deg_f":
            case "degf":
            case "f":
                return FAHRENHEIT;
            case "kelvin":
            case "k":
                return KELVIN;
            case "watt/m2":
                return WATT.divide(SQUARE_METRE);
            case "j m**-2":
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
}
