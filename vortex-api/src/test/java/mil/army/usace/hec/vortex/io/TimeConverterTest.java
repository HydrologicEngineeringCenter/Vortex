package mil.army.usace.hec.vortex.io;

import org.junit.jupiter.api.Test;

import java.time.*;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

class TimeConverterTest {
    @Test
    void testToZonedDateTimeWithZ() {
        String input = "2021-07-20 15:50:55 Z";
        ZonedDateTime expected = ZonedDateTime.of(2021, 7, 20, 15, 50, 55, 0, ZoneId.of("Z"));
        ZonedDateTime result = TimeConverter.toZonedDateTime(input);
        assertNotNull(result);
        assertTimeEquals(expected, result);
    }

    @Test
    void testToZonedDateTimeWithoutZ() {
        String input = "2021-07-20 15:50:55";
        ZonedDateTime expected = ZonedDateTime.of(2021, 7, 20, 15, 50, 55, 0, ZoneId.of("UTC"));
        ZonedDateTime result = TimeConverter.toZonedDateTime(input);
        assertNotNull(result);
        assertTimeEquals(expected, result);
    }

    @Test
    void testToZonedDateTimeIsoDateTime() {
        String input = "2021-07-20T15:50:55Z";
        ZonedDateTime expected = ZonedDateTime.parse(input);
        ZonedDateTime result = TimeConverter.toZonedDateTime(input);
        assertNotNull(result);
        assertTimeEquals(expected, result);
    }

    @Test
    void testToZonedDateTimeIsoLocalDateTime() {
        String input = "2021-07-20T15:50:55";
        ZonedDateTime expected = LocalDateTime.parse(input).atZone(ZoneId.of("UTC"));
        ZonedDateTime result = TimeConverter.toZonedDateTime(input);
        assertNotNull(result);
        assertTimeEquals(expected, result);
    }

    @Test
    void testToZonedDateTimeSimpleDate() {
        String input = "2021-7-20";
        ZonedDateTime expected = LocalDate.parse(input, DateTimeFormatter.ofPattern("uuuu-M-d")).atStartOfDay(ZoneId.of("UTC"));
        ZonedDateTime result = TimeConverter.toZonedDateTime(input);
        assertNotNull(result);
        assertTimeEquals(expected, result);
    }

    @Test
    void testToZonedDateTimeFullDateTimeWithZoneOffset() {
        String input = "2021-07-20 15:50:55 -07:00";
        ZonedDateTime expected = ZonedDateTime.of(LocalDateTime.of(2021, 7, 20, 15, 50, 55), ZoneOffset.ofHours(-7));
        ZonedDateTime result = TimeConverter.toZonedDateTime(input);
        assertNotNull(result);
        assertTimeEquals(expected.withZoneSameInstant(ZoneId.of("UTC")), result);
    }

    @Test
    void testToZonedDateTimeDateOnly() {
        String input = "2021-07-20";
        ZonedDateTime expected = LocalDate.parse(input).atStartOfDay(ZoneId.of("UTC"));
        ZonedDateTime result = TimeConverter.toZonedDateTime(input);
        assertNotNull(result);
        assertTimeEquals(expected, result);
    }

    @Test
    void testToZonedDateTimeMonthYearOnly() {
        String input = "2021-07";
        ZonedDateTime expected = YearMonth.parse(input, DateTimeFormatter.ofPattern("uuuu-MM")).atDay(1).atStartOfDay(ZoneId.of("UTC"));
        ZonedDateTime result = TimeConverter.toZonedDateTime(input);
        assertNotNull(result);
        assertTimeEquals(expected, result);
    }

    @Test
    void testToZonedDateTimeWithDifferentZoneId() {
        String input = "2021-07-20 15:50:55 America/New_York";
        ZonedDateTime expected = ZonedDateTime.of(LocalDateTime.of(2021, 7, 20, 15, 50, 55), ZoneId.of("America/New_York"));
        ZonedDateTime result = TimeConverter.toZonedDateTime(input);
        assertNotNull(result);
        assertTimeEquals(expected.withZoneSameInstant(ZoneId.of("UTC")), result);
    }

    @Test
    void testToZonedDateTimeWithHyphenatedAmericanFormat() {
        String input = "07-20-2021 15:50";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm").withZone(ZoneId.of("UTC"));
        ZonedDateTime expected = ZonedDateTime.parse(input, formatter);
        ZonedDateTime result = TimeConverter.toZonedDateTime(input);
        assertNotNull(result);
        assertTimeEquals(expected, result);
    }

    @Test
    void testToZonedDateTimeWithSlashAmericanFormat() {
        String input = "07/20/2021 15:50";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm").withZone(ZoneId.of("UTC"));
        ZonedDateTime expected = ZonedDateTime.parse(input, formatter);
        ZonedDateTime result = TimeConverter.toZonedDateTime(input);
        assertNotNull(result);
        assertTimeEquals(expected, result);
    }

    // Test for an invalid format
    @Test
    void testToZonedDateTimeInvalidFormat() {
        String input = "invalid-date-time";
        assertNull(TimeConverter.toZonedDateTime(input));
    }

    private void assertTimeEquals(ZonedDateTime expected, ZonedDateTime result) {
        boolean isEqual = expected.isEqual(result);
        assertTrue(isEqual);
    }
}