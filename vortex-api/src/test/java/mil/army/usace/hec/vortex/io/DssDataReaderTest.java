package mil.army.usace.hec.vortex.io;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DssDataReaderTest {

    @Test
    void GetVariablesFromDss6ReturnsSetOfStrings() {
        String pathToDss = new File(getClass().getResource("/normalizer/prism.dss").getFile()).toString();
        Set<String> variables = DssDataReader.getVariables(pathToDss);
        assertEquals(2, variables.size());
    }

    @Test
    void GetVariablesFromDss7ReturnsSetOfStrings(){
        String pathToDss = new File(getClass().getResource("/normalizer/qpe.dss").getFile()).toString();
        Set<String> variables = DssDataReader.getVariables(pathToDss);
        assertEquals(49, variables.size());
    }
}