package mil.army.usace.hec.vortex.geo;

import org.junit.jupiter.api.Test;

import java.io.File;

class BatchTransposerTest {

    @Test
    void SelectAllVariablesSelectsAll(){
        String pathToDss = new File(getClass().getResource("/normalizer/qpe.dss").getFile()).toString();
        String pathToTransposedDss = new File(getClass().getResource("/transposer/transposed.dss").getFile()).toString();

        BatchTransposer transposer = BatchTransposer.builder()
                .pathToInput(pathToDss)
                .selectAllVariables()
                .angle(45)
                .destination(pathToTransposedDss)
                .build();

        transposer.process();
    }

}