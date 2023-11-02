package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexGrid;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

class Hdf5DataWriterTest {
    private final String tempDir = System.getProperty("java.io.tmpdir");

    @Test
    void Hdf5DataWriterWritesDssFile() {
        String inFile = new File(getClass().getResource(
                "/normalizer/qpe.dss").getFile()).toString();
        String variableName = "///PRECIPITATION/02JAN2017:1200/02JAN2017:1300//";

        DataReader reader = DataReader.builder()
                .path(inFile)
                .variable(variableName)
                .build();

        List<VortexData> dtos = new ArrayList<>(reader.getDtos());

        Path destination = Paths.get(tempDir, "data.p01.tmp.hdf");

        DataWriter writer = DataWriter.builder()
                .destination(destination)
                .data(dtos)
                .options(Map.of(
                        "dataset_name","data1/data2/Values"
                ))
                .build();

        writer.write();
    }


    @Test
    void Hdf5RasPrecipDataWriterWritesDssFileMultipleTs() {

        String inFile = new File(getClass().getResource("/normalizer/qpe.dss").getFile()).toString();

        Path destination = Paths.get(tempDir, "data.p15.hdf");

        for(String datapath:DataReader.getVariables(inFile)){

            DataReader reader = DataReader.builder()
                    .path(inFile)
                    .variable(datapath)
                    .build();

            List<VortexData> dtos = new ArrayList<>(reader.getDtos());

            DataWriter writer = DataWriter.builder()
                    .destination(destination)
                    .data(dtos)
                    .build();

            writer.write();

        }
    }


    /*
    This is a slightly weird unit test but it is designed to make sure the hdf5 hyperslab selected for writes
    is being set properly.  The test is doing the following:
     1) Gets a set of data from qpe.dss.  In all it will result in 49 grids of data
     2) Change the actual grid data to an incrementing number for each row.
        The 1st row we write should be all 0s, the second all 1s, the third all 2s, etc, etc up to 48.
     3) Batch the grids into buffers the size of "bufsize"
     4) when the buffer reaches bufsize, write the buffer and clear it out
    */
    @Test
    void Hdf5RasPrecipDataWriterWritesDssFileMultipleTsBatched() {

        String inFile = new File(getClass().getResource("/normalizer/qpe.dss").getFile()).toString();

        Path destination = Paths.get(tempDir, "data3.p99.hdf");

        int count = 0;
        int bufsize = 4;

        List<VortexData> buffer = new ArrayList<>();

        for(String datapath:DataReader.getVariables(inFile)){

            DataReader reader = DataReader.builder()
                    .path(inFile)
                    .variable(datapath)
                    .build();

            List<VortexData> dtos = new ArrayList<>(reader.getDtos());
            VortexData dtdata = dtos.get(0);
            setDataToVal(dtdata,count);
            buffer.add(dtdata);

            if (count%bufsize==0) {
                DataWriter writer = DataWriter.builder()
                        .destination(destination)
                        .data(buffer)
                        .build();

                writer.write();
                buffer.clear();
            }
            count++;
        }
    }

    void setDataToVal(VortexData data, int val) {
        try {
            VortexGrid grid = (VortexGrid) data;
            Field dataField = VortexGrid.class.getDeclaredField("data");
            dataField.setAccessible(true);
            float[] griddata = (float[]) dataField.get(grid);
            Arrays.fill(griddata, val);
        }
        catch(Exception ex){
            ex.printStackTrace();
        }

    }



}


