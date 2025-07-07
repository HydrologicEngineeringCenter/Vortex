package mil.army.usace.hec.vortex.io;

import hec.heclib.util.HecTime;
import ucar.nc2.time.Calendar;
import ucar.nc2.time.CalendarDate;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;
import java.util.logging.Logger;

class TimeConverter {
    private static final Logger LOGGER = Logger.getLogger(TimeConverter.class.getName());
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

    private static final DateTimeFormatter hecTimeFormatter = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("ddMMMuuuu:HHmm")
            .toFormatter(Locale.ENGLISH);

    private TimeConverter() {
        // Utility class - Private constructor
    }

    static ZonedDateTime toZonedDateTime(HecTime hecTime) {
        return ZonedDateTime.parse(hecTime.getXMLDateTime(0));
    }

    static ZonedDateTime toZonedDateTime(CalendarDate calendarDate) {
        if (Calendar.julian.equals(calendarDate.getCalendar())) {
            return fromJulian(calendarDate);
        }
        return Instant.ofEpochMilli(calendarDate.getMillis()).atZone(ZoneOffset.UTC);
    }

    static ZonedDateTime toZonedDateTime(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.isEmpty()) {
            return null;
        }

        TemporalAccessor parsedDate = parseDate(dateTimeString);

        if (parsedDate instanceof ZonedDateTime zonedDateTime) {
            return zonedDateTime.withZoneSameInstant(UTC);
        } else if (parsedDate instanceof LocalDateTime localDateTime) {
            return localDateTime.atZone(UTC);
        } else if (parsedDate instanceof LocalDate localDate) {
            return localDate.atStartOfDay(UTC);
        } else {
            return null;
        }
    }

    private static TemporalAccessor parseDate(String dateTimeString) {
        TemporalAccessor parsedDate = parseYearMonthDayFormat(dateTimeString);
        if (parsedDate == null) {
            parsedDate = parseMonthDayYear(dateTimeString);
        }
        if (parsedDate == null) {
            parsedDate = parseHecTime(dateTimeString);
        }

        return parsedDate;
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

    private static TemporalAccessor parseHecTime(String dateTimeString) {
        try {
            return hecTimeFormatter.parseBest(dateTimeString, ZonedDateTime::from, LocalDateTime::from, LocalDate::from);
        } catch (Exception e) {
            return null;
        }
    }

    private static ZonedDateTime fromJulian(CalendarDate calendarDate) {
        String isoString = calendarDate.toString();
        try {
            return ZonedDateTime.parse(isoString);
        } catch (Exception e) {
            LOGGER.warning(() -> "Could not parse date: " + isoString);
            return Instant.ofEpochMilli(calendarDate.getMillis()).atZone(ZoneOffset.UTC);
        }
    }
}
