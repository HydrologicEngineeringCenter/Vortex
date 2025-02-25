package mil.army.usace.hec.vortex.util;

import org.junit.jupiter.api.Test;
import tech.units.indriya.unit.TransformedUnit;

import javax.measure.Unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static systems.uom.common.USCustomary.INCH;
import static systems.uom.common.USCustomary.SQUARE_FOOT;

class UnitUtilTest {

    @Test
    void unit1divide1000inches() {
        String str = "1/1000 in";

        Unit<?> units = INCH.divide(1000);
        Unit<?> parsed = TransformedUnit.parse("in/1000");

        Unit<?> retrieved = UnitUtil.parse(str);
        assertEquals(units, retrieved);
        assertEquals(units, parsed);
    }

    @Test
    void unitSquareFeet() {
        String ft2 = "ft2";
        assertEquals(SQUARE_FOOT, UnitUtil.parse(ft2));

        String sqft = "sqft";
        assertEquals(SQUARE_FOOT, UnitUtil.parse(sqft));
    }
}