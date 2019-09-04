package mil.army.usace.hec.vortex.geo;

import hec.heclib.dss.HecDssCatalog;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BatchTransposerTest {

    @Test
    void BatchTransposeFtWorthGrids(){
        String inFile = new File(getClass().getResource(
                "/transposer/precip2000_Jun.dss").getFile()).toString();

        String pathToTransposedDss = new File(getClass().getResource("/transposer/precip2000_Jun_transposed.dss").getFile()).toString();

        BatchTransposer transposer = BatchTransposer.builder()
                .pathToInput(inFile)
                .selectAllVariables()
                .angle(30)
                .destination(pathToTransposedDss)
                .build();

        transposer.process();

        HecDssCatalog catalog = new HecDssCatalog(inFile);
        String[] paths = catalog.getCatalog(false, "/*/*/*/*/*/*/");
        assertEquals(7, paths.length);
    }
}