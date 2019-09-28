package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.VortexData;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

class TiffDataWriterTest {

    @Test
    void TiffDataWriterWritesGribFile(){
        Path inFile = new File(getClass().getResource(
                "/MRMS_GaugeCorr_QPE_01H_00.00_20170102-120000.grib2").getFile()).toPath();
        String variableName = "GaugeCorrQPE01H_altitude_above_msl";

        DataReader reader = DataReader.builder()
                .path(inFile)
                .variable(variableName)
                .build();

        List<VortexData> dtos = new ArrayList<>(reader.getDtos());

        Path destination = Paths.get("C:/Temp/.tiff");

        DataWriter writer = DataWriter.builder()
                .destination(destination)
                .data(dtos)
                .build();

        writer.write();
    }

    @Test
    void TiffDataWriterWritesDssFile(){
        Path inFile = new File(getClass().getResource(
                "/normalizer/qpe.dss").getFile()).toPath();
        String variableName = "///PRECIPITATION/02JAN2017:1200/02JAN2017:1300//";

        DataReader reader = DataReader.builder()
                .path(inFile)
                .variable(variableName)
                .build();

        List<VortexData> dtos = new ArrayList<>(reader.getDtos());

        Path destination = Paths.get("C:/Temp/.tiff");

        DataWriter writer = DataWriter.builder()
                .destination(destination)
                .data(dtos)
                .build();

        writer.write();
    }

}