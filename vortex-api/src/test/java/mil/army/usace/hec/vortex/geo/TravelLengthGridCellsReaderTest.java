package mil.army.usace.hec.vortex.geo;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

class TravelLengthGridCellsReaderTest {

    @Test
    void TravelLengthGridCellsReaderReadsGridCells(){
        File inFile = new File(getClass().getResource(
                "/punxsutawney.mod").getFile());

        List<TravelLengthGridCell> gridCells = TravelLengthGridCellsReader.builder()
                .pathToSource(inFile.toString())
                .build()
                .read();

        Assertions.assertEquals(163, gridCells.size());
    }

}