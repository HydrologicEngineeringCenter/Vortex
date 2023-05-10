package mil.army.usace.hec.vortex.util;

import hec.heclib.util.HecTime;
import ucar.nc2.time.CalendarDate;

import java.time.ZonedDateTime;
import java.util.Date;

public class TimeConverter {

    private TimeConverter(){}

    public static ZonedDateTime toZonedDateTime(HecTime hecTime){
        return ZonedDateTime.parse(hecTime.getXMLDateTime(0));
    }

    public static ZonedDateTime toZonedDateTime(CalendarDate calendarDate){
        return ZonedDateTime.parse(calendarDate.toString());
    }

    public static HecTime toHecTime(ZonedDateTime zonedDateTime){
        return new HecTime(Date.from(zonedDateTime.toInstant()), 0);
    }

}
