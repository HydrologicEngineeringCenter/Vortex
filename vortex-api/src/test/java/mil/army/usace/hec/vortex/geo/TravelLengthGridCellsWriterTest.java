package mil.army.usace.hec.vortex.geo;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

class TravelLengthGridCellsWriterTest {

    @Test
    void WriteShpWritesShapefile(){
        File inFile = new File(getClass().getResource(
                "/punxsutawney.mod").getFile());

        List<TravelLengthGridCell> gridCells = TravelLengthGridCellsReader.builder()
                .pathToSource(inFile.toString())
                .build()
                .read();

        TravelLengthGridCellsWriter writer = TravelLengthGridCellsWriter.builder()
                .pathToDestination(Paths.get("C:/Temp/punx.shp"))
                .travelLengthGridCells(gridCells)
                .cellSize(2000)
                .projection(WktFactory.create("UTM17N"))
                .build();

        writer.write();
    }

}