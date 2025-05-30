package mil.army.usace.hec.vortex.geo;

import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.io.DataReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

class TransposerTest {

    @Test
    void testTranspose(){
        String inFile = new File(getClass().getResource(
                "/normalizer/qpe.dss").getFile()).toString();
        String variableName = "///PRECIPITATION/02JAN2017:1200/02JAN2017:1300//";

        DataReader reader = DataReader.builder()
                .path(inFile)
                .variable(variableName)
                .build();

        List<VortexGrid> grids = reader.getDtos().stream().map(grid -> (VortexGrid)grid).collect(Collectors.toList());
        VortexGrid grid = grids.get(0);

        Transposer transposer = Transposer.builder()
                .grid(grid)
                .angle(30)
                .build();

        transposer.transpose();
    }

    @Test
    void TransposeFtWorthGrid(){
        String inFile = new File(getClass().getResource(
                "/transposer/precip2000_Jun.dss").getFile()).toString();
        String variableName = "/SHG/WGRFC/PRECIP/01JUN2000:0600/01JUN2000:0700/METVUE/";

        DataReader reader = DataReader.builder()
                .path(inFile)
                .variable(variableName)
                .build();

        List<VortexGrid> grids = reader.getDtos().stream().map(grid -> (VortexGrid)grid).collect(Collectors.toList());
        VortexGrid grid = grids.get(0);

        Transposer transposer = Transposer.builder()
                .grid(grid)
                .angle(30)
                .build();

        VortexGrid transposed = transposer.transpose();

        float[] vortex = transposed.data();
        List<Float> list = new ArrayList<>();
        for (int i = 0; i < vortex.length; i++){
            list.add(vortex[i]);
        }

        File persisted = new File(getClass().getResource(
                "/transposer/ftworth_output_serialized").getFile());

        ArrayList arraylist = new ArrayList<>();
        try {
            FileInputStream fis = new FileInputStream(persisted.toString());
            ObjectInputStream ois = new ObjectInputStream(fis);
            arraylist = (ArrayList) ois.readObject();
            ois.close();
            fis.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        float[] data = transposed.data();
        for (int i = 0; i < arraylist.size(); i++) {
            Assertions.assertEquals(arraylist.get(i), data[i]);
        }

//        try{
//            FileOutputStream fos= new FileOutputStream("C:/Temp/ftworth_output_serialized");
//            ObjectOutputStream oos= new ObjectOutputStream(fos);
//            oos.writeObject(list);
//            oos.close();
//            fos.close();
//        }catch(IOException ioe){
//            ioe.printStackTrace();
//        }
    }

}