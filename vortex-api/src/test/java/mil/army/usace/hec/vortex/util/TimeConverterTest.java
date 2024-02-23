package mil.army.usace.hec.vortex.util;

import org.junit.jupiter.api.Test;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import static org.junit.jupiter.api.Assertions.*;

class TimeConverterTest {
    @Test
    public void testToZonedDateTimeWithZ() {
        String input = "2021-07-20 15:50:55 Z";
        ZonedDateTime expected = ZonedDateTime.of(2021, 7, 20, 15, 50, 55, 0, ZoneId.of("Z"));
        ZonedDateTime result = TimeConverter.toZonedDateTime(input);
        assertNotNull(result);
        assertTimeEquals(expected, result);
    }

    @Test
    public void testToZonedDateTimeWithoutZ() {
        String input = "2021-07-20 15:50:55";
        ZonedDateTime expected = ZonedDateTime.of(2021, 7, 20, 15, 50, 55, 0, ZoneId.of("UTC"));
        ZonedDateTime result = TimeConverter.toZonedDateTime(input);
        assertNotNull(result);
        assertTimeEquals(expected, result);
    }

    @Test
    public void testToZonedDateTimeIsoDateTime() {
        String input = "2021-07-20T15:50:55Z";
        ZonedDateTime expected = ZonedDateTime.parse(input);
        ZonedDateTime result = TimeConverter.toZonedDateTime(input);
        assertNotNull(result);
        assertTimeEquals(expected, result);
    }

    @Test
    public void testToZonedDateTimeIsoLocalDateTime() {
        String input = "2021-07-20T15:50:55";
        ZonedDateTime expected = LocalDateTime.parse(input).atZone(ZoneId.of("UTC"));
        ZonedDateTime result = TimeConverter.toZonedDateTime(input);
        assertNotNull(result);
        assertTimeEquals(expected, result);
    }

    @Test
    public void testToZonedDateTimeSimpleDate() {
        String input = "2021-7-20";
        ZonedDateTime expected = LocalDate.parse(input, DateTimeFormatter.ofPattern("uuuu-M-d")).atStartOfDay(ZoneId.of("UTC"));
        ZonedDateTime result = TimeConverter.toZonedDateTime(input);
        assertNotNull(result);
        assertTimeEquals(expected, result);
    }

    @Test
    public void testToZonedDateTimeFullDateTimeWithZoneOffset() {
        String input = "2021-07-20 15:50:55 -07:00";
        ZonedDateTime expected = ZonedDateTime.of(LocalDateTime.of(2021, 7, 20, 15, 50, 55), ZoneOffset.ofHours(-7));
        ZonedDateTime result = TimeConverter.toZonedDateTime(input);
        assertNotNull(result);
        assertTimeEquals(expected.withZoneSameInstant(ZoneId.of("UTC")), result);
    }

    @Test
    public void testToZonedDateTimeDateOnly() {
        String input = "2021-07-20";
        ZonedDateTime expected = LocalDate.parse(input).atStartOfDay(ZoneId.of("UTC"));
        ZonedDateTime result = TimeConverter.toZonedDateTime(input);
        assertNotNull(result);
        assertTimeEquals(expected, result);
    }

    @Test
    public void testToZonedDateTimeMonthYearOnly() {
        String input = "2021-07";
        ZonedDateTime expected = YearMonth.parse(input, DateTimeFormatter.ofPattern("uuuu-MM")).atDay(1).atStartOfDay(ZoneId.of("UTC"));
        ZonedDateTime result = TimeConverter.toZonedDateTime(input);
        assertNotNull(result);
        assertTimeEquals(expected, result);
    }

    @Test
    public void testToZonedDateTimeWithDifferentZoneId() {
        String input = "2021-07-20 15:50:55 America/New_York";
        ZonedDateTime expected = ZonedDateTime.of(LocalDateTime.of(2021, 7, 20, 15, 50, 55), ZoneId.of("America/New_York"));
        ZonedDateTime result = TimeConverter.toZonedDateTime(input);
        assertNotNull(result);
        assertTimeEquals(expected.withZoneSameInstant(ZoneId.of("UTC")), result);
    }

    @Test
    public void testToZonedDateTimeWithHyphenatedAmericanFormat() {
        String input = "07-20-2021 15:50";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm").withZone(ZoneId.of("UTC"));
        ZonedDateTime expected = ZonedDateTime.parse(input, formatter);
        ZonedDateTime result = TimeConverter.toZonedDateTime(input);
        assertNotNull(result);
        assertTimeEquals(expected, result);
    }

    @Test
    public void testToZonedDateTimeWithSlashAmericanFormat() {
        String input = "07/20/2021 15:50";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm").withZone(ZoneId.of("UTC"));
        ZonedDateTime expected = ZonedDateTime.parse(input, formatter);
        ZonedDateTime result = TimeConverter.toZonedDateTime(input);
        assertNotNull(result);
        assertTimeEquals(expected, result);
    }

    // Test for an invalid format
    @Test
    public void testToZonedDateTimeInvalidFormat() {
        String input = "invalid-date-time";
        assertNull(TimeConverter.toZonedDateTime(input));
    }

    private void assertTimeEquals(ZonedDateTime expected, ZonedDateTime result) {
        boolean isEqual = expected.isEqual(result);
        assertTrue(isEqual);
    }
}