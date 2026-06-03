package mil.army.usace.hec.vortex.io;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

// Kept in its own class because BilDataReaderTest has a static initializer
// that fails when the GDAL native library is absent; the failure poisons
// every @Test in that class. This class has no static state, so the
// missing-native-lib case can be Assumptions.abort()'d cleanly.
class BilDataReaderErrorPathTest {

    @Test
    void missingFile_surfacesAsDataReadException() throws Exception {
        DataReader reader = DataReader.builder()
                .path("/does/not/exist/missing.bil")
                .build();

        try {
            reader.getDtos();
            fail("expected DataReadException for missing .bil but got no exception");
        } catch (DataReadException e) {
            assertEquals(DataReadException.Kind.IO_ERROR, e.getKind());
            assertTrue(e.getMessage().contains("missing.bil"),
                    "expected path in message but got: " + e.getMessage());
        } catch (UnsatisfiedLinkError | NoClassDefFoundError e) {
            Assumptions.abort("GDAL native library not available: " + e.getMessage());
        }
    }
}
