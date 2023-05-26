package mil.army.usace.hec.vortex.io;

import hec.heclib.dss.DssDataType;
import hec.heclib.grid.GridData;
import hec.heclib.grid.GridInfo;
import hec.heclib.grid.GriddedData;
import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.geo.Resampler;
import mil.army.usace.hec.vortex.geo.WktFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Envelope;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetcdfDataReaderTest {

    @Test
    void Sresa1bPrecipRateImport(){
        String inFile = new File(getClass().getResource("/sresa1b_ncar_ccsm3-example.nc").getFile()).toString();
        String variableName = "pr";

        DataReader reader = DataReader.builder()
                .path(inFile)
                .variable(variableName)
                .build();

        List<VortexGrid> dtos = reader.getDtos().stream().map(grid -> (VortexGrid) grid).collect(Collectors.toList());
        assertEquals(1, dtos.size());
        assertEquals(32768, dtos.get(0).data().length);
    }

    @Test
    void GpcpPrecipImport(){
        String inFile = new File(getClass().getResource("/gpcp_cdr_v23rB1_y2019_m01.nc").getFile()).toString();
        String variableName = "precip";

        DataReader reader = DataReader.builder()
                .path(inFile)
                .variable(variableName)
                .build();

        List<VortexGrid> dtos = reader.getDtos().stream().map(grid -> (VortexGrid) grid).collect(Collectors.toList());
        assertEquals(1, dtos.size());
        assertEquals(10368, dtos.get(0).data().length);
    }

    @Test
    void CpcTmaxImport(){
        String inFile = new File(getClass().getResource("/tmax.2017.nc").getFile()).toString();
        String variableName = "tmax";

        DataReader reader = DataReader.builder()
                .path(inFile)
                .variable(variableName)
                .build();

        List<VortexGrid> dtos = reader.getDtos().stream().map(grid -> (VortexGrid) grid).collect(Collectors.toList());
        assertEquals(365, dtos.size());
        assertEquals(259200, dtos.get(0).data().length);
    }

    @Test
    void CpcPrecipImport(){
        String inFile = new File(getClass().getResource("/precip.2017.nc").getFile()).toString();
        String variableName = "precip";

        DataReader reader = DataReader.builder()
                .path(inFile)
                .variable(variableName)
                .build();

        List<VortexGrid> dtos = reader.getDtos().stream().map(grid -> (VortexGrid) grid).collect(Collectors.toList());
        assertEquals(365, dtos.size());
        assertEquals(259200, dtos.get(0).data().length);
    }

    @Test
    void EcmwfEra40Import(){
        String inFile = new File(getClass().getResource("/ECMWF_ERA-40_subset.nc").getFile()).toString();
        String variableName = "p2t";

        DataReader reader = DataReader.builder()
                .path(inFile)
                .variable(variableName)
                .build();

        List<VortexGrid> dtos = reader.getDtos().stream().map(grid -> (VortexGrid) grid).collect(Collectors.toList());
        assertEquals(62, dtos.size());
        assertEquals(10512, dtos.get(0).data().length);
    }

    @Test
    void GpmSubImport(){
        String inFile = new File(getClass().getResource("/3B-HHR.MS.MRG.3IMERG.20170103-S110000-E112959.0660.V06B.HDF5.SUB.hdf5").getFile()).toString();
        String variableName = "precipitationCal";

        DataReader reader = DataReader.builder()
                .path(inFile)
                .variable(variableName)
                .build();

        List<VortexGrid> dtos = reader.getDtos().stream().map(grid -> (VortexGrid) grid).collect(Collectors.toList());
    }

    @Test
    void Sfav2Import(){
        String inFile = new File(getClass().getResource("/sfav2_CONUS_24h_2010030112.nc").getFile()).toString();
        String variableName = "Data";

        DataReader reader = DataReader.builder()
                .path(inFile)
                .variable(variableName)
                .build();

        List<VortexGrid> grids = reader.getDtos().stream().map(grid -> (VortexGrid) grid).collect(Collectors.toList());
        VortexGrid grid = grids.get(0);
        assertTrue(grid.startTime().isEqual(ZonedDateTime.of(2010, 2, 28, 12, 0, 0, 0, ZoneId.of("UTC"))));
        assertTrue(grid.endTime().isEqual(ZonedDateTime.of(2010, 3, 1, 12, 0, 0, 0, ZoneId.of("UTC"))));
    }

    @Test
    void AorcTempImport(){
        String inFile = new File(getClass().getResource("/AORC_TMP_MARFC_1984010100.nc4").getFile()).toString();

        DataReader reader = DataReader.builder()
                .path(inFile)
                .variable("TMP_2maboveground")
                .build();

        List<VortexGrid> grids = reader.getDtos().stream().map(grid -> (VortexGrid) grid).collect(Collectors.toList());
        VortexGrid grid = grids.get(0);
        assertTrue(grid.startTime().isEqual(ZonedDateTime.of(1984, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))));
        assertTrue(grid.endTime().isEqual(ZonedDateTime.of(1984, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))));
    }

    @Test
    void AorcPrecipImport(){
        String inFile = new File(getClass().getResource("/AORC_APCP_MARFC_1984010100.nc4").getFile()).toString();

        DataReader reader = DataReader.builder()
                .path(inFile)
                .variable("APCP_surface")
                .build();

        List<VortexGrid> grids = reader.getDtos().stream().map(grid -> (VortexGrid) grid).collect(Collectors.toList());
        VortexGrid grid = grids.get(0);
        assertTrue(grid.startTime().isEqual(ZonedDateTime.of(1983, 12, 31, 23, 0, 0, 0, ZoneId.of("UTC"))));
        assertTrue(grid.endTime().isEqual(ZonedDateTime.of(1984, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))));
    }

    @Test
    void ArizonaSweDaily(){
        String inFile = new File(getClass().getResource("/01.nc").getFile()).toString();

        DataReader reader = DataReader.builder()
                .path(inFile)
                .variable("SWE")
                .build();

        VortexData vortexData = reader.getDto(0);
        VortexGrid vortexGrid = (VortexGrid) vortexData;
        assertEquals(ZonedDateTime.of(2020, 3, 1, 0, 0, 0, 0, ZoneId.of("Z")), vortexGrid.startTime());
        assertEquals(ZonedDateTime.of(2020, 3, 2, 0, 0, 0, 0, ZoneId.of("Z")), vortexGrid.endTime());
    }

    @Test
    void GCIP_EOP_Stage_IV(){
        String inFile = new File(getClass().getResource("/ST4.2005010100.01h").getFile()).toString();

        Set<String> variables = DataReader.getVariables(inFile);
        assertTrue(variables.contains("Total_precipitation_surface_1_Hour_Accumulation"));
        assertEquals(2, variables.size());
    }

    @Test
    void NLDAS_Forcing_APCP(){
        String inFile = new File(getClass().getResource("/NLDAS_FORA0125_H.A19820101.0000.002.grb.SUB.nc4").getFile()).toString();

        //from NLDAS documentation: precipitation is backward-accumulated (over the entire previous hour before the time listed in the dataset)

        DataReader reader = DataReader.builder()
                .path(inFile)
                .variable("APCP")
                .build();

        VortexData vortexData = reader.getDto(0);
        VortexGrid vortexGrid = (VortexGrid) vortexData;
        assertEquals(ZonedDateTime.of(1981, 12, 31, 23, 0, 0, 0, ZoneId.of("Z")), vortexGrid.startTime());
        assertEquals(ZonedDateTime.of(1982, 1, 1, 0, 0, 0, 0, ZoneId.of("Z")), vortexGrid.endTime());
    }

    @Test
    void NLDAS_Forcing_TMP(){
        String inFile = new File(getClass().getResource("/NLDAS_FORA0125_H.A19820101.0000.002.grb.SUB.nc4").getFile()).toString();

        DataReader reader = DataReader.builder()
                .path(inFile)
                .variable("TMP")
                .build();

        VortexData vortexData = reader.getDto(0);
        VortexGrid vortexGrid = (VortexGrid) vortexData;
        assertEquals(ZonedDateTime.of(1982, 1, 1, 0, 0, 0, 0, ZoneId.of("Z")), vortexGrid.startTime());
        assertEquals(ZonedDateTime.of(1982, 1, 1, 0, 0, 0, 0, ZoneId.of("Z")), vortexGrid.endTime());
    }

    @Test
    void MRMS_RadarOnly_QPE(){
        String inFile = new File(getClass().getResource("/RadarOnly_QPE_01H_00.00_20210706-000000.grib2").getFile()).toString();

        DataReader reader = DataReader.builder()
                .path(inFile)
                .variable("RadarOnly_QPE_01H_altitude_above_msl")
                .build();

        VortexData vortexData = reader.getDto(0);
        VortexGrid vortexGrid = (VortexGrid) vortexData;
        assertEquals(ZonedDateTime.of(2021, 7, 5, 23, 0, 0, 0, ZoneId.of("Z")), vortexGrid.startTime());
        assertEquals(ZonedDateTime.of(2021, 7, 6, 0, 0, 0, 0, ZoneId.of("Z")), vortexGrid.endTime());
    }

    @Test
    void MRMS_MultiSensor_QPE(){
        String inFile = new File(getClass().getResource("/MultiSensor_QPE_01H_Pass2_00.00_20210706-000000.grib2").getFile()).toString();

        DataReader reader = DataReader.builder()
                .path(inFile)
                .variable("MultiSensor_QPE_01H_Pass2_altitude_above_msl")
                .build();

        VortexData vortexData = reader.getDto(0);
        VortexGrid vortexGrid = (VortexGrid) vortexData;
        assertEquals(ZonedDateTime.of(2021, 7, 5, 23, 0, 0, 0, ZoneId.of("Z")), vortexGrid.startTime());
        assertEquals(ZonedDateTime.of(2021, 7, 6, 0, 0, 0, 0, ZoneId.of("Z")), vortexGrid.endTime());
    }

    @Test
    void MRMS_PrecipRate(){
        String inFile = new File(getClass().getResource("/PrecipRate_00.00_20210706-000000.grib2").getFile()).toString();

        DataReader reader = DataReader.builder()
                .path(inFile)
                .variable("PrecipRate_altitude_above_msl")
                .build();

        //According to the docs, PrecipRate files are 2 minute: https://www.nssl.noaa.gov/projects/mrms/operational/tables.php

        VortexData vortexData = reader.getDto(0);
        VortexGrid vortexGrid = (VortexGrid) vortexData;
        assertEquals(ZonedDateTime.of(2021, 7, 5, 23, 58, 0, 0, ZoneId.of("Z")), vortexGrid.startTime());
        assertEquals(ZonedDateTime.of(2021, 7, 6, 0, 0, 0, 0, ZoneId.of("Z")), vortexGrid.endTime());
    }

    @Test
    void cmip() {
        URL inUrl = Objects.requireNonNull(getClass().getResource(
                "/cmip/Extraction_pr.nc"));

        String inFile = new File(inUrl.getFile()).toString();

        URL outURL = Objects.requireNonNull(getClass().getResource(
                "/cmip/cmip.dss"));

        String outFile = new File(outURL.getFile()).toString();

        DataReader reader = DataReader.builder()
                .path(inFile)
                .variable("pr")
                .build();

        VortexData data = reader.getDto(10);

        DataWriter writer = DataWriter.builder()
                .destination(Paths.get(outFile))
                .data(Collections.singletonList(data))
                .build();

        writer.write();

        int[] status = new int[1];
        GriddedData griddedData = new GriddedData();
        griddedData.setDSSFileName(outFile);
        griddedData.setPathname("///PRECIPITATION/11JAN2050:0000/11JAN2050:2400//");
        GridData gridData = new GridData();
        griddedData.retrieveGriddedData(true, gridData, status);
        if (status[0] < 0) {
            Assertions.fail();
        }

        GridInfo gridInfo = gridData.getGridInfo();
        Assertions.assertEquals("11 January 2050, 00:00", gridInfo.getStartTime());
        Assertions.assertEquals("11 January 2050, 24:00", gridInfo.getEndTime());
        Assertions.assertEquals("MM", gridInfo.getDataUnits());
        Assertions.assertEquals(DssDataType.PER_CUM.value(), gridInfo.getDataType());
        Assertions.assertEquals(17.903835, gridInfo.getMaxValue(), 1E-2);
        Assertions.assertEquals(5.481848, gridInfo.getMinValue(), 1E-2);

        griddedData.done();
    }

    @Test
    void gefs() {
        URL inUrl = Objects.requireNonNull(getClass().getResource(
                "/gefs/gep01.t06z.pgrb2s.0p25.f000.grb2"));

        String inFile = new File(inUrl.getFile()).toString();
        List<String> inFiles = Collections.singletonList(inFile);

        URL outURL = Objects.requireNonNull(getClass().getResource(
                "/gefs/gefs.dss"));

        String outFile = new File(outURL.getFile()).toString();

        List<String> vars = Collections.singletonList("Precipitable_water_entire_atmosphere_single_layer_ens");

        BatchImporter importer = BatchImporter.builder()
                .inFiles(inFiles)
                .variables(vars)
                .destination(outFile)
                .build();

        importer.process();

        int[] status = new int[1];
        GriddedData griddedData = new GriddedData();
        griddedData.setDSSFileName(outFile);
        griddedData.setPathname("///PRECIPITATION/27JUN2022:0600/27JUN2022:0900//");
        GridData gridData = new GridData();
        griddedData.retrieveGriddedData(true, gridData, status);
        if (status[0] < 0) {
            Assertions.fail();
        }

        GridInfo gridInfo = gridData.getGridInfo();
        Assertions.assertEquals("27 June 2022, 06:00", gridInfo.getStartTime());
        Assertions.assertEquals("27 June 2022, 09:00", gridInfo.getEndTime());
        Assertions.assertEquals("MM", gridInfo.getDataUnits());
        Assertions.assertEquals(DssDataType.PER_CUM.value(), gridInfo.getDataType());
        Assertions.assertEquals(75.1, gridInfo.getMaxValue(), 1E-2);
        Assertions.assertEquals(0.09999999, gridInfo.getMinValue(), 1E-2);
        Assertions.assertEquals(20.607605, gridInfo.getMeanValue(), 1E-2);

        griddedData.done();
    }

    @Test
    void cordex() {
        URL inUrl = Objects.requireNonNull(getClass().getResource(
                "/cordex/precip.nc"));

        String inFile = new File(inUrl.getFile()).toString();

        URL outUrl = Objects.requireNonNull(getClass().getResource(
                "/cordex/cordex.dss"));

        String outFile = new File(outUrl.getFile()).toString();

        String variable = "pr";

        DataReader reader = DataReader.builder()
                .path(inFile)
                .variable(variable)
                .build();

        VortexGrid grid = (VortexGrid) reader.getDto(0);

        Envelope envelope = new Envelope(
                273985.492595975,
                923708.987813649,
                796388.154005694,
                1066866.62606159
        );

        String wkt = WktFactory.create("UTM17N");

        VortexGrid resampled = Resampler.builder()
                .grid(grid)
                .envelope(envelope)
                .envelopeWkt(wkt)
                .targetWkt(WktFactory.create("UTM17N"))
                .cellSize(2000.0)
                .method("Bilinear")
                .build()
                .resample();


        DataWriter writer = DataWriter.builder()
                .destination(outFile)
                .data(Collections.singletonList(resampled))
                .build();

        writer.write();

        int[] status = new int[1];
        GriddedData griddedData = new GriddedData();
        griddedData.setDSSFileName(outFile);
        griddedData.setPathname("///PRECIPITATION/01JAN2001:0000/01JAN2001:2400//");
        GridData gridData = new GridData();
        griddedData.retrieveGriddedData(true, gridData, status);
        if (status[0] < 0) {
            Assertions.fail();
        }

        GridInfo gridInfo = gridData.getGridInfo();
        Assertions.assertEquals("1 January 2001, 00:00", gridInfo.getStartTime());
        Assertions.assertEquals("1 January 2001, 24:00", gridInfo.getEndTime());
        Assertions.assertEquals("MM", gridInfo.getDataUnits());
        Assertions.assertEquals(DssDataType.PER_CUM.value(), gridInfo.getDataType());
        Assertions.assertEquals(0.6857273578643799, gridInfo.getMaxValue(), 1E-5);
        Assertions.assertEquals(0.0, gridInfo.getMinValue(), 1E-5);
        Assertions.assertEquals(0.015706462785601616, gridInfo.getMeanValue(), 1E-5);

        griddedData.done();
    }
}