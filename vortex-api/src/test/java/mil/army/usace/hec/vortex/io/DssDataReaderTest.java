package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.GdalRegister;
import mil.army.usace.hec.vortex.VortexGrid;
import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DssDataReaderTest {
    static {
        GdalRegister.getInstance();
    }

    @Test
    void GetVariablesFromDss6ReturnsSetOfStrings() {
        String pathToDss = new File(getClass().getResource("/normalizer/prism.dss").getFile()).toString();
        Set<String> variables = DssDataReader.getVariables(pathToDss);
        assertEquals(2, variables.size());
    }

    @Test
    void GetVariablesFromDss7ReturnsSetOfStrings(){
        String pathToDss = new File(getClass().getResource("/normalizer/qpe.dss").getFile()).toString();
        Set<String> variables = DssDataReader.getVariables(pathToDss);
        assertEquals(49, variables.size());
    }

    @Test
    void TrinityXMRG2DSSImport() {
        String pathToDss = new File(getClass().getResource("/TrinityXMRG2DSS.dss").getFile()).toString();

        DataReader reader = DataReader.builder()
                .path(pathToDss)
                .variable("///PRECIP/30MAY1997:1200/30MAY1997:1300/XMRG2DSS/")
                .build();

        VortexGrid grid = (VortexGrid) reader.getDto(0);
        float[] gridData = grid.data();

        String pathToGtiff = new File(getClass().getResource("/TrinityXMRG2DSS.tiff").getFile()).toString();
        Dataset dataset = gdal.Open(pathToGtiff);

        double[] geoTransform = dataset.GetGeoTransform();
        double dx = geoTransform[1];
        double dy = geoTransform[5];
        double originY = geoTransform[3];
        int nx = dataset.GetRasterYSize();
        int ny = dataset.GetRasterYSize();
        Band band = dataset.GetRasterBand(1);
        float[] data = new float[nx * ny];
        band.ReadRaster(0, 0, nx, ny, gdalconst.GDT_Float32, data);

        dataset.delete();
        band.delete();

        assertEquals(originY, grid.originY());
        assertEquals(dx, grid.dx());
        assertEquals(dy, grid.dy());
        double expectedMax = Double.MIN_VALUE;
        double actualMax = Double.MIN_VALUE;
        int isEqualCount = 0;
        for (int i=0; i<data.length; i++) {
            expectedMax = Math.max(expectedMax, data[i]);
            actualMax = Math.max(actualMax, gridData[i]);
            if (Double.compare(data[i], gridData[i]) == 0)
                isEqualCount++;
        }
        System.out.println((double) isEqualCount / data.length);

        assertEquals(expectedMax, actualMax);
    }
}