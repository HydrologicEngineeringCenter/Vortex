package mil.army.usace.hec.vortex.geo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.gdal.ogr.DataSource;
import org.gdal.ogr.Layer;
import org.gdal.ogr.ogr;
import org.gdal.ogr.ogrConstants;
import org.gdal.osr.SpatialReference;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class VectorUtilsTest {

    @TempDir
    Path directory;

    @Test
    void ShapefileWithProjectionPassesValidation() {
        Path path = createShapefile(true);
        assertTrue(VectorUtils.validateProjection(path).isValid());
        assertTrue(VectorUtils.validateProjection(path).getMessages().isEmpty());
    }

    @Test
    void MissingShapefileProjectionBlocksUntilSidecarIsAdded() throws Exception {
        Path path = createShapefile(false);
        var validation = VectorUtils.validateProjection(path);
        assertFalse(validation.isValid());
        assertTrue(validation.getMessages().get(0).contains(path.toString()));
        assertTrue(validation.getMessages().get(0).contains("no projection"));
        Files.writeString(directory.resolve("zones.prj"), WktFactory.fromEpsg(4326));
        assertTrue(VectorUtils.validateProjection(path).isValid());
    }

    @Test
    void UnreadableVectorDatasetBlocksValidation() {
        var validation = VectorUtils.validateProjection(directory.resolve("missing.shp"));
        assertFalse(validation.isValid());
        assertFalse(validation.getMessages().isEmpty());
    }

    private Path createShapefile(boolean projected) {
        mil.army.usace.hec.vortex.GdalRegister.getInstance();
        Path path = directory.resolve("zones.shp");
        SpatialReference srs = projected ? new SpatialReference(WktFactory.fromEpsg(4326)) : null;
        DataSource source = ogr.GetDriverByName("ESRI Shapefile").CreateDataSource(path.toString());
        assertNotNull(source);
        try {
            Layer layer = source.CreateLayer("zones", srs, ogrConstants.wkbPolygon);
            assertNotNull(layer);
            layer.delete();
        } finally {
            source.delete();
            if (srs != null) {
                srs.delete();
            }
        }
        return path;
    }

    @Test
    void GetWktReturnsWktString() {
        File inFile = new File(getClass().getResource(
                "/Truckee_River_Watershed_5mi_buffer/Truckee_River_Watershed_5mi_buffer.shp").getFile());
        String wkt = VectorUtils.getWkt(inFile.toPath());
        assertTrue(wkt.contains("USA_Contiguous_Albers_Equal_Area_Conic_USGS_version"));
    }
}
