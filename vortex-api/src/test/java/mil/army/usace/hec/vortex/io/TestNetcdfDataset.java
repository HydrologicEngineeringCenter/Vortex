package mil.army.usace.hec.vortex.io;

import org.junit.jupiter.api.Test;
import ucar.nc2.dataset.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

class TestNetcdfDataset {

    @Test
    void test1() throws IOException {
        Path path = null;
        try {
            path = Paths.get(getClass().getClassLoader()
                    .getResource("precip.2017.nc").toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        NetcdfDataset ncd = null;
        try {
            assert path != null;
            ncd = NetcdfDatasets.openDataset(path.toString());
        } catch(Exception e){
            e.printStackTrace();
        } finally {
            ncd.close();
        }
    }
}
