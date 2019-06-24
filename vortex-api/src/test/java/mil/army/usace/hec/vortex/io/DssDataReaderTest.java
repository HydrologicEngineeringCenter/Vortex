package mil.army.usace.hec.vortex.io;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DssDataReaderTest {

    @Test
    void GetVariablesFromDss6ReturnsSetOfStrings() {
        Path pathToDss = new File(getClass().getResource("/normalizer/prism.dss").getFile()).toPath();
        Set<String> variables = DssDataReader.getVariables(pathToDss);
        assertEquals(2, variables.size());
    }

    @Test
    void GetVariablesFromDss7ReturnsSetOfStrings(){
        Path pathToDss = new File(getClass().getResource("/normalizer/qpe.dss").getFile()).toPath();
        Set<String> variables = DssDataReader.getVariables(pathToDss);
        assertEquals(49, variables.size());
    }
}