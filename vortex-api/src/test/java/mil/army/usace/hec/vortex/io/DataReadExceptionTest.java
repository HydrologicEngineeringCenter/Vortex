package mil.army.usace.hec.vortex.io;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class DataReadExceptionTest {

    @Test
    void ioErrorFactorySetsKindAndFields() {
        IOException cause = new IOException("native");
        DataReadException e = DataReadException.ioError("/data.dss", "//A/B/C/", "boom", cause);

        assertEquals(DataReadException.Kind.IO_ERROR, e.getKind());
        assertEquals("/data.dss", e.getDataPath());
        assertEquals("//A/B/C/", e.getRecordPathname());
        assertEquals(0, e.getStatusCode());
        assertEquals("boom", e.getMessage());
        assertSame(cause, e.getCause());
    }

    @Test
    void ioErrorOverloadOmitsCause() {
        DataReadException e = DataReadException.ioError("/data.dss", null, "boom");
        assertNull(e.getCause());
        assertNull(e.getRecordPathname());
    }

    @Test
    void ofPreservesAllFields() {
        DataReadException e = DataReadException.of(
                DataReadException.Kind.MISSING_RECORD, "/x.dss", "//Z/", -2, "missing", null);

        assertEquals(DataReadException.Kind.MISSING_RECORD, e.getKind());
        assertEquals(-2, e.getStatusCode());
        assertEquals("/x.dss", e.getDataPath());
        assertEquals("//Z/", e.getRecordPathname());
    }

    @Test
    void ofRejectsNullKind() {
        assertThrows(NullPointerException.class, () ->
                DataReadException.of(null, "/x", null, 0, "msg", null));
    }

    @Test
    void isAnIOException() {
        DataReadException e = DataReadException.ioError("/x", null, "msg");
        assertInstanceOf(IOException.class, e);
    }

    @Test
    void roundTripsThroughSerialization() throws Exception {
        DataReadException original = DataReadException.of(
                DataReadException.Kind.UNSUPPORTED, "/x.dss", "//R/", -3, "no good", null);

        byte[] bytes;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(original);
            bytes = baos.toByteArray();
        }

        DataReadException restored;
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            restored = (DataReadException) ois.readObject();
        }

        assertEquals(original.getKind(), restored.getKind());
        assertEquals(original.getDataPath(), restored.getDataPath());
        assertEquals(original.getRecordPathname(), restored.getRecordPathname());
        assertEquals(original.getStatusCode(), restored.getStatusCode());
        assertEquals(original.getMessage(), restored.getMessage());
    }
}
