package mil.army.usace.hec.vortex.geo;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class VectorUtilsTest {

    @Test
    void GetWktReturnsWktString() {
        File inFile = new File(getClass().getResource(
                "/Truckee_River_Watershed_5mi_buffer/Truckee_River_Watershed_5mi_buffer.shp").getFile());
        String wkt = VectorUtils.getWkt(inFile.toPath());
        assertTrue(wkt.contains("USA_Contiguous_Albers_Equal_Area_Conic_USGS_version"));
    }
}