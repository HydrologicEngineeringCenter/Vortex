package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.util.ImageUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

class AscDataWriterTest {
    @Test
    void AscDataWriterWritesDssFile(){
        String inFile = new File(getClass().getResource(
                "/normalizer/qpe.dss").getFile()).toString();
        String variableName = "///PRECIPITATION/02JAN2017:1200/02JAN2017:1300//";

        DataReader reader = DataReader.builder()
                .path(inFile)
                .variable(variableName)
                .build();

        List<VortexData> dtos = new ArrayList<>(reader.getDtos());

        String fileName = ImageUtils.generateFileName("qpe", (VortexGrid) dtos.get(0), ImageFileType.ASC);

        Path destination = Paths.get("C:/Temp",fileName);

        DataWriter writer = DataWriter.builder()
                .destination(destination)
                .data(dtos)
                .build();

        writer.write();
    }

}