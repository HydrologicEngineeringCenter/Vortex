package mil.army.usace.hec.vortex.geo;

import mil.army.usace.hec.vortex.GdalRegister;
import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.io.DataReader;
import mil.army.usace.hec.vortex.io.DataWriter;
import org.gdal.osr.SpatialReference;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import ucar.unidata.geoloc.Earth;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.projection.LambertConformal;
import ucar.unidata.geoloc.projection.proj4.LambertConformalConicEllipse;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WktFactoryTest {
    private final String tempDir = System.getProperty("java.io.tmpdir");

    static {
        GdalRegister.getInstance();
    }

    @Test
    void lambertOffsetsAreConvertedFromKilometersToMeters() {
        Projection spherical = new LambertConformal(33.75, -79, 36.1666666666667,
                34.3333333333333, 609.601219202438, 12.5);
        Projection ellipsoidal = new LambertConformalConicEllipse(33.75, -79,
                36.1666666666667, 34.3333333333333, 609.601219202438, 12.5,
                new Earth(6378137, 6356752.314140356, 298.257222101));

        for (Projection projection : List.of(spherical, ellipsoidal)) {
            SpatialReference srs = new SpatialReference(WktFactory.createWkt(projection));
            try {
                assertEquals(609601.219202438, srs.GetProjParm("false_easting"), 1E-6);
                assertEquals(12500, srs.GetProjParm("false_northing"), 1E-6);
                assertEquals(1.0, srs.GetLinearUnits(), 1E-12);
            } finally {
                srs.delete();
            }
        }
    }

    @Test
    void lambertWktRoundTripPreservesOffsets() {
        SpatialReference srs = new SpatialReference();
        srs.SetProjCS("Lambert Conformal Conic 2SP");
        srs.SetWellKnownGeogCS("WGS84");
        srs.SetLCC(36.1666666666667, 34.3333333333333, 33.75, -79,
                609601.219202438, 12500);
        srs.SetLinearUnits("Meter", 1.0);
        String wkt = srs.ExportToWkt();
        srs.delete();

        LambertConformalConicEllipse projection =
                (LambertConformalConicEllipse) WktParser.getProjection(wkt);
        assertEquals(609.601219202438, projection.getFalseEasting(), 1E-9);
        assertEquals(12.5, projection.getFalseNorthing(), 1E-9);

        SpatialReference roundTrip = new SpatialReference(WktFactory.createWkt(projection));
        try {
            assertEquals(609601.219202438, roundTrip.GetProjParm("false_easting"), 1E-6);
            assertEquals(12500, roundTrip.GetProjParm("false_northing"), 1E-6);
        } finally {
            roundTrip.delete();
        }
    }

    @Test
    void lambertUsSurveyFootOffsetIsConvertedToKilometers() {
        LambertConformalConicEllipse projection =
                (LambertConformalConicEllipse) WktParser.getProjection(WktFactory.fromEpsg(6543));

        assertEquals(609.601219202438, projection.getFalseEasting(), 1E-6);
        assertEquals(0, projection.getFalseNorthing(), 1E-9);
    }

    @Disabled //because this test was taking a really long time
    @Test
    void MrmsPrecipWktPassesRegression() throws Exception {
        String inFile = new File(getClass().getResource(
                "/MRMS_GaugeCorr_QPE_01H_00.00_20170102-120000.grib2").getFile()).toString();

        String variableName = "GaugeCorrQPE01H_altitude_above_msl";

        DataReader reader = DataReader.builder().path(inFile).variable(variableName).build();
        List<VortexGrid> data = reader.getDtos().stream().map(grid -> (VortexGrid) grid).collect(Collectors.toList());
        VortexGrid grid = data.get(0);
        String wkt = grid.wkt();

        SpatialReference srs = new SpatialReference(wkt);
        assertEquals(6371229, srs.GetSemiMajor(), 1E-3);
        assertEquals(0, srs.GetInvFlattening(), 1E-3);
        assertEquals("degree", srs.GetAngularUnitsName().toLowerCase());

        List<VortexData> grids = new ArrayList<>();
        grids.add(grid);

        DataWriter.builder()
                .data(grids)
                .destination(Paths.get(tempDir, "test.dss"))
                .build()
                .write();
    }

    @Test
    void RtmaTemperatureWktPassesRegression() throws Exception {
        String inFile = new File(getClass().getResource(
                "/201701021200_TMPK.grib2").getFile()).toString();

        String variableName = "Temperature_height_above_ground";

        DataReader reader = DataReader.builder().path(inFile).variable(variableName).build();
        List<VortexGrid> data = reader.getDtos().stream().map(grid -> (VortexGrid) grid).collect(Collectors.toList());
        VortexGrid grid = data.get(0);
        String wkt = grid.wkt();

        SpatialReference srs = new SpatialReference(wkt);
        assertEquals(6371200, srs.GetSemiMajor(), 1E-3);
        assertEquals(0.0, srs.GetInvFlattening(), 1E-3);
        assertEquals("Lambert_Conformal_Conic_2SP", srs.GetAttrValue("PROJECTION"));
        assertEquals(25, srs.GetProjParm("standard_parallel_1"), 1E-3);
        assertEquals(25, srs.GetProjParm("standard_parallel_2"), 1E-3);
        assertEquals(25, srs.GetProjParm("latitude_of_origin"), 1E-3);
        assertEquals(265, srs.GetProjParm("central_meridian"), 1E-3);
        assertEquals(0, srs.GetProjParm("false_easting"), 1E-3);
        assertEquals(0, srs.GetProjParm("false_northing"), 1E-3);
        assertEquals("degree", srs.GetAngularUnitsName().toLowerCase());
        assertEquals("meter", srs.GetLinearUnitsName().toLowerCase());

        List<VortexData> grids = new ArrayList<>();
        grids.add(grid);

        DataWriter.builder()
                .data(grids)
                .destination(Paths.get(tempDir, "test.dss"))
                .build()
                .write();
    }
}
