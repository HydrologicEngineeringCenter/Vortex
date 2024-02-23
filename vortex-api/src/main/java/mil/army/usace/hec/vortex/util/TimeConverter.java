package mil.army.usace.hec.vortex.util;

import hec.heclib.util.HecTime;
import ucar.nc2.time.CalendarDate;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.logging.Logger;

public class TimeConverter {
    private static final Logger logger = Logger.getLogger(TimeConverter.class.getName());
    private static final ZoneId UTC = ZoneId.of("UTC");

    private static final DateTimeFormatter ymdFormatter = new DateTimeFormatterBuilder()
            .appendPattern("yyyy[-][/][MM][M][-][/][dd]['T'][ ][HH][:][mm][:][ss][ ][VV][XXX]")
            .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
            .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
            .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
            .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
            .toFormatter();

    private static final DateTimeFormatter mdyFormatter = new DateTimeFormatterBuilder()
            .appendPattern("[MM][M][-][/][dd][-][/]yyyy['T'][ ][HH][:][mm][:][ss][ ][VV][XXX]")
            .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
            .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
            .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
            .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
            .toFormatter();

    private TimeConverter() {
        // Utility class - Private constructor
    }

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
        TemporalAccessor parsedDate = parseYearMonthDayFormat(dateTimeString);
        if (parsedDate == null) parsedDate = parseMonthDayYear(dateTimeString);

        if (parsedDate instanceof ZonedDateTime zonedDateTime) {
            return zonedDateTime.withZoneSameInstant(UTC);
        } else if (parsedDate instanceof LocalDateTime localDateTime) {
            return localDateTime.atZone(UTC);
        } else if (parsedDate instanceof LocalDate localDate) {
            return localDate.atStartOfDay(UTC);
        } else {
            logger.warning(String.format("Unable to parse: %s", dateTimeString));
            return null;
        }
    }

    private static TemporalAccessor parseYearMonthDayFormat(String dateTimeString) {
        try {
            return ymdFormatter.parseBest(dateTimeString, ZonedDateTime::from, LocalDateTime::from, LocalDate::from);
        } catch (Exception e) {
            return null;
        }
    }

    private static TemporalAccessor parseMonthDayYear(String dateTimeString) {
        try {
            return mdyFormatter.parseBest(dateTimeString, ZonedDateTime::from, LocalDateTime::from, LocalDate::from);
        } catch (Exception e) {
            return null;
        }
    }
}
