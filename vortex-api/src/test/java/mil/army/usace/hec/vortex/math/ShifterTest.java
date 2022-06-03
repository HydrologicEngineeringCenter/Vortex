package mil.army.usace.hec.vortex.math;

import hec.heclib.dss.DssDataType;
import hec.heclib.grid.GridData;
import hec.heclib.grid.GridInfo;
import hec.heclib.grid.GriddedData;
import mil.army.usace.hec.vortex.io.DataReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShifterTest {

    @Test
    void NumberOfGridsInEqualsNumberOfGridsOut(){
        String source = new File(getClass().getResource("/shifter/shifter_in.dss").getFile()).toString();

        Set<String> sourceVariables = DataReader.getVariables(source);

        String destination = new File(getClass().getResource("/shifter/shifter_out.dss").getFile()).toString();

        Shifter shifter = Shifter.builder()
                .shift(Duration.ofHours(-12))
                .pathToFile(source)
                .grids(sourceVariables)
                .destination(destination)
                .build();

        shifter.shift();

        Set<String> destinationVariables = DataReader.getVariables(destination);

        assertEquals(sourceVariables.size(), destinationVariables.size());
    }

    @Test
    void UaSweDataTypeInEqualsDataTypeOut(){
        URL inUrl = Objects.requireNonNull(getClass().getResource(
                "/shifter/ua_swe_in.dss"));

        String in = new File(inUrl.getFile()).toString();

        Set<String> variables = DataReader.getVariables(in);

        URL outUrl = Objects.requireNonNull(getClass().getResource(
                "/shifter/ua_swe_out.dss"));

        String out = new File(outUrl.getFile()).toString();

        Shifter shifter = Shifter.builder()
                .shift(Duration.ofHours(-6))
                .pathToFile(in)
                .grids(variables)
                .destination(out)
                .build();

        shifter.shift();

        int[] status = new int[1];
        GriddedData griddedData = new GriddedData();
        griddedData.setDSSFileName(out);
        griddedData.setPathname("///SWE/29FEB2020:1800/01MAR2020:1800//");
        GridData gridData = new GridData();
        griddedData.retrieveGriddedData(true, gridData, status);
        if (status[0] < 0) {
            Assertions.fail();
        }

        GridInfo gridInfo = gridData.getGridInfo();
        Assertions.assertEquals(gridInfo.getDataType(), DssDataType.PER_AVER.value());

        griddedData.done();
    }

}