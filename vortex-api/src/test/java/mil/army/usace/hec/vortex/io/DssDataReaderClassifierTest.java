package mil.army.usace.hec.vortex.io;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the HEC-DSS status-code → {@link DataReadException.Kind} classification
 * table. Native-library-free; exercises only the pure mapping in
 * {@link DssDataReader#dssFailure(String, String, int)}.
 */
class DssDataReaderClassifierTest {

    @Test
    void status_negativeTwo_isMissingRecord() {
        DataReadException e = DssDataReader.dssFailure("/x.dss", "//A/B/C/", -2);
        assertEquals(DataReadException.Kind.MISSING_RECORD, e.getKind());
        assertEquals(-2, e.getStatusCode());
    }

    @Test
    void status_negativeThree_isUnsupported() {
        DataReadException e = DssDataReader.dssFailure("/x.dss", "//A/B/C/", -3);
        assertEquals(DataReadException.Kind.UNSUPPORTED, e.getKind());
        assertEquals(-3, e.getStatusCode());
    }

    @Test
    void status_negativeOne_isIoError() {
        DataReadException e = DssDataReader.dssFailure("/x.dss", "//A/B/C/", -1);
        assertEquals(DataReadException.Kind.IO_ERROR, e.getKind());
    }

    @Test
    void status_otherNegative_isIoError() {
        DataReadException e = DssDataReader.dssFailure("/x.dss", "//A/B/C/", -42);
        assertEquals(DataReadException.Kind.IO_ERROR, e.getKind());
        assertEquals(-42, e.getStatusCode());
    }

    @Test
    void message_includesFileAndPathnameAndKind() {
        DataReadException e = DssDataReader.dssFailure("/x.dss", "//A/B/C/", -2);
        String msg = e.getMessage();
        assertEquals("/x.dss", e.getDataPath());
        assertEquals("//A/B/C/", e.getRecordPathname());
        // The message is for human consumption; assert key tokens are present
        // without locking the exact format.
        assertTrue(msg.contains("/x.dss"));
        assertTrue(msg.contains("//A/B/C/"));
        assertTrue(msg.contains("MISSING_RECORD"));
    }

    @Test
    void status_zero_isIoError_andMessageExplains() {
        // Status 0 conventionally means success in HEC-DSS; getting here means
        // the native call returned no data despite reporting success. Verify
        // the message includes an explanatory phrase rather than just reporting
        // the bare status code.
        DataReadException e = DssDataReader.dssFailure("/x.dss", "//A/B/C/", 0);
        assertEquals(DataReadException.Kind.IO_ERROR, e.getKind());
        assertEquals(0, e.getStatusCode());
        String msg = e.getMessage();
        assertTrue(msg.contains("/x.dss"));
        assertTrue(msg.toLowerCase().contains("no data") || msg.toLowerCase().contains("jni"),
                "expected an explanatory phrase for status=0 but got: " + msg);
    }
}
