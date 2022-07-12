package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.VortexData;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

class TiffDataWriterTest {
    private final String tempDir = System.getProperty("java.io.tmpdir");

    @Test
    void TiffDataWriterWritesGribFile(){
        String inFile = new File(getClass().getResource(
                "/MRMS_GaugeCorr_QPE_01H_00.00_20170102-120000.grib2").getFile()).toString();
        String variableName = "GaugeCorrQPE01H_altitude_above_msl";

        DataReader reader = DataReader.builder()
                .path(inFile)
                .variable(variableName)
                .build();

        List<VortexData> dtos = new ArrayList<>(reader.getDtos());

        Path destination = Paths.get(tempDir, "data.tiff");

        DataWriter writer = DataWriter.builder()
                .destination(destination)
                .data(dtos)
                .build();

        writer.write();
    }

    @Test
    void TiffDataWriterWritesDssFile(){
        String inFile = new File(getClass().getResource(
                "/normalizer/qpe.dss").getFile()).toString();
        String variableName = "///PRECIPITATION/02JAN2017:1200/02JAN2017:1300//";

        DataReader reader = DataReader.builder()
                .path(inFile)
                .variable(variableName)
                .build();

        List<VortexData> dtos = new ArrayList<>(reader.getDtos());

        Path destination = Paths.get(tempDir,"data.tiff");

        DataWriter writer = DataWriter.builder()
                .destination(destination)
                .data(dtos)
                .build();

        writer.write();
    }

    @Test
    void TiffDataWriterWritesSnodasFile() {
        String inFile = new File(getClass().getResource("/regression/io/snodas_reader/SNODAS_20191101.tar").getFile()).toString();

        DataReader reader = DataReader.builder()
                .path(inFile)
                .build();

        List<VortexData> dtos = new ArrayList<>(reader.getDtos());

        Path destination = Paths.get(tempDir,"data.tiff");

        DataWriter writer = DataWriter.builder()
                .destination(destination)
                .data(dtos)
                .build();

        writer.write();
    }

}