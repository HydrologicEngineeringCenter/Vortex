package mil.army.usace.hec.vortex.math;

import mil.army.usace.hec.vortex.io.DataReader;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.Duration;
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

}