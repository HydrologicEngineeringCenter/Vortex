package mil.army.usace.hec.vortex.util;

import hec.heclib.util.HecTime;

import java.time.ZonedDateTime;
import java.util.Date;

public class TimeConverter {

    private TimeConverter(){}

    public static ZonedDateTime toZonedDateTime(HecTime hecTime){
        return ZonedDateTime.parse(hecTime.getXMLDateTime(0));
    }

    public static HecTime toHecTime(ZonedDateTime zonedDateTime){
        return new HecTime(Date.from(zonedDateTime.toInstant()), 0);
    }

}
