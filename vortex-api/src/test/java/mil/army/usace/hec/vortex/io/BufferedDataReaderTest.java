package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.TestUtil;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class BufferedDataReaderTest {
    @Test
    void GetTest() {
        File file = TestUtil.getResourceFile("/normalizer/qpe.dss");
        assertNotNull(file);

        String pathToFile = file.getAbsolutePath();
        String variableName = "*";

        DataReader dataReader = DataReader.builder()
                .path(pathToFile)
                .variable(variableName)
                .build();
        BufferedDataReader bufferedReader = new BufferedDataReader(pathToFile, variableName);

        assertEquals(dataReader.getDto(20), bufferedReader.get(20));
        assertEquals(dataReader.getDto(29), bufferedReader.get(29));
        assertEquals(dataReader.getDto(0), bufferedReader.get(0));
        assertEquals(dataReader.getDto(6), bufferedReader.get(6));
        assertNull(bufferedReader.get(55));
    }

    @Test
    void GetCountTest() {
        File file = TestUtil.getResourceFile("/normalizer/qpe.dss");
        assertNotNull(file);

        String pathToFile = file.getAbsolutePath();
        String variableName = "*";

        BufferedDataReader bufferedReader = new BufferedDataReader(pathToFile, variableName);
        assertEquals(49, bufferedReader.getCount());
    }
}