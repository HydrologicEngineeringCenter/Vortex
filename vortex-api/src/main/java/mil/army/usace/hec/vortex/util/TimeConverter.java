package mil.army.usace.hec.vortex.util;

import hec.heclib.util.HecTime;
import ucar.nc2.time.CalendarDate;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.TemporalAccessor;
import java.util.Date;

public class TimeConverter {

    private TimeConverter(){}

    private static final DateTimeFormatter dateTimeFormatter = new DateTimeFormatterBuilder()
            .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'Z'"))
            .appendOptional(DateTimeFormatter.ISO_DATE_TIME)
            .appendOptional(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            .appendOptional(DateTimeFormatter.ofPattern("uuuu-M-d"))
            .toFormatter();

    public static ZonedDateTime toZonedDateTime(HecTime hecTime){
        return ZonedDateTime.parse(hecTime.getXMLDateTime(0));
    }

    public static ZonedDateTime toZonedDateTime(CalendarDate calendarDate){
        return ZonedDateTime.parse(calendarDate.toString());
    }

    public static ZonedDateTime toZonedDateTime(long epochSeconds, ZoneId zoneId) {
        Instant instant = Instant.ofEpochSecond(epochSeconds);
        return ZonedDateTime.ofInstant(instant, zoneId);
    }

    public static HecTime toHecTime(ZonedDateTime zonedDateTime){
        return new HecTime(Date.from(zonedDateTime.toInstant()), 0);
    }

    public static ZonedDateTime toZonedDateTime(String dateTimeString) {
        TemporalAccessor parsedDate = dateTimeFormatter.parseBest(dateTimeString, ZonedDateTime::from, LocalDateTime::from, LocalDate::from);

        if (parsedDate instanceof ZonedDateTime zonedDateTime) {
            return zonedDateTime;
        } else if (parsedDate instanceof LocalDateTime localDateTime) {
            return localDateTime.atZone(ZoneId.of("UTC"));
        } else if (parsedDate instanceof LocalDate localDate) {
            return localDate.atStartOfDay(ZoneId.of("UTC"));
        }

        return null;
    }

}
