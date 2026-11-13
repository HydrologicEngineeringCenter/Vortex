package mil.army.usace.hec.vortex.io;

import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NetcdfTimeAxisTest {
    @Test
    void newAxisDeduplicatesRecordsAfterQuantization() {
        ZonedDateTime start = ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        VortexDataInterval first = VortexDataInterval.of(start.plusSeconds(10), start.plusSeconds(20));
        VortexDataInterval sameStoredMinute = VortexDataInterval.of(start.plusSeconds(40), start.plusSeconds(50));
        VortexDataInterval next = VortexDataInterval.of(start.plusMinutes(1), start.plusMinutes(1));

        NetcdfTimeAxis axis = NetcdfTimeAxis.of(ChronoUnit.MINUTES, false,
                List.of(first, next, sameStoredMinute, first));

        assertEquals(2, axis.recordCount());
        assertEquals(0, axis.indexOf(first));
        assertEquals(0, axis.indexOf(sameStoredMinute));
        assertEquals(1, axis.indexOf(next));
    }
}
