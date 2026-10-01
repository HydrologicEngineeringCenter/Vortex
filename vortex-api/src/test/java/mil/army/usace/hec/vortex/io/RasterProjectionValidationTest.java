package mil.army.usace.hec.vortex.io;

import mil.army.usace.hec.vortex.GdalRegister;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.VortexDataType;
import hec.heclib.dss.HecDataManager;
import hec.heclib.grid.GridData;
import hec.heclib.grid.GridInfo;
import hec.heclib.grid.GriddedData;
import hec.heclib.grid.SpecifiedGridInfo;
import mil.army.usace.hec.vortex.geo.WktFactory;
import mil.army.usace.hec.vortex.util.DssUtil;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.iosp.netcdf3.N3raf;
import ucar.nc2.write.NetcdfFormatWriter;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class RasterProjectionValidationTest {
    @TempDir
    Path directory;

    @BeforeAll
    static void registerMetadataTestProvider() throws Exception {
        NetcdfFiles.registerIOProvider(MetadataOnlyNetcdfReader.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"asc", "tif", "bil"})
    void missingProjectionBlocksAdvancingBeforeVariablesAreSelected(String extension) throws Exception {
        Path raster = createRaster("missing", extension, false);
        try (DataReader reader = DataReader.builder().path(raster.toString()).build()) {
            Validation validation = reader.isValid();
            Validation directValidation = RasterProjectionValidation.validateRaster(raster.toString());
            assertFalse(directValidation.isValid());
            assertEquals(validation.getMessages(), directValidation.getMessages());
            assertFalse(validation.isValid(), "A missing projection must block advancing from step one");
            assertFalse(RasterProjectionValidation.validateSource(raster.toString(), List.of()).isValid());
            assertEquals(1, validation.getMessages().size());
            assertTrue(validation.getMessages().get(0).contains(raster.toString()));
            assertTrue(validation.getMessages().get(0).contains("no projection"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"asc", "tif", "bil"})
    void definedProjectionDoesNotWarn(String extension) throws Exception {
        Path raster = createRaster("projected", extension, true);
        try (DataReader reader = DataReader.builder().path(raster.toString()).build()) {
            Validation validation = reader.isValid();
            assertTrue(validation.isValid());
            assertTrue(RasterProjectionValidation.validateSource(raster.toString(), List.of()).isValid());
            assertTrue(RasterProjectionValidation.validateRaster(raster.toString()).isValid());
            assertTrue(validation.getMessages().isEmpty());
        }
    }

    @Test
    void addingSidecarClearsWarningWithoutChangingAsciiGrid() throws Exception {
        Path raster = createRaster("na2_wa_2yr24hr", "asc", false);
        String original = Files.readString(raster);
        try (DataReader reader = DataReader.builder().path(raster.toString()).build()) {
            assertFalse(reader.isValid().isValid());
            assertFalse(reader.isValid().getMessages().isEmpty());
            Files.writeString(directory.resolve("na2_wa_2yr24hr.prj"), WktFactory.fromEpsg(4267));
            assertTrue(reader.isValid().isValid(), "Fixing the projection must allow advancing on retry");
            assertTrue(reader.isValid().getMessages().isEmpty());
            assertEquals(original, Files.readString(raster));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"asc", "bil"})
    void archiveChecksEveryRasterAndUsesItsSidecar(String extension) throws Exception {
        createRaster("first", extension, true);
        createRaster("second", extension, false);
        Path archive = directory.resolve("rasters_" + extension + ".zip");
        // Put the projected raster first so checking only the first entry would miss the warning.
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            for (String basename : new String[]{"first", "second"}) {
                for (String suffix : new String[]{extension, "hdr", "prj"}) {
                    Path file = directory.resolve(basename + "." + suffix);
                    if (!Files.exists(file)) {
                        continue;
                    }
                    zip.putNextEntry(new ZipEntry(file.getFileName().toString()));
                    Files.copy(file, zip);
                    zip.closeEntry();
                }
            }
        }
        try (DataReader reader = DataReader.builder().path(archive.toString()).build()) {
            Validation validation = reader.isValid();
            assertFalse(validation.isValid(), "Any unprojected raster must block the entire archive");
            assertFalse(RasterProjectionValidation.validateSource(archive.toString(), List.of()).isValid());
            assertEquals(1, validation.getMessages().size());
            assertTrue(validation.getMessages().get(0).contains("second." + extension));
            assertTrue(validation.getMessages().get(0).contains(archive.toString()));
        }
    }

    @Test
    void unreadableRasterIsAnErrorRatherThanAProjectionWarning() throws Exception {
        Path missing = directory.resolve("does-not-exist.asc");
        try (DataReader reader = DataReader.builder().path(missing.toString()).build()) {
            Validation validation = reader.isValid();
            assertFalse(validation.isValid());
            assertFalse(RasterProjectionValidation.validateRaster(missing.toString()).isValid());
            assertFalse(validation.getMessages().isEmpty());
            assertFalse(validation.getMessages().get(0).contains("no projection"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"nc", "dss"})
    void ValidatesDecodedProjectionForEachSelectedVariable(String extension) throws Exception {
        Path output = directory.resolve("source." + extension);
        ZonedDateTime time = ZonedDateTime.parse("2000-01-01T00:00:00Z");
        VortexGrid grid = VortexGrid.builder()
                .nx(2).ny(2).dx(0.1).dy(-0.1).originX(-124).originY(47)
                .wkt(WktFactory.fromEpsg(4326)).data(new float[]{1, 2, 3, 4}).noDataValue(-9999)
                .units("mm").shortName("precipitation").fullName("precipitation").description("precipitation")
                .startTime(time).endTime(time).interval(Duration.ZERO)
                .dataType(VortexDataType.INSTANTANEOUS).build();
        try {
            DataWriter.builder().destination(output).data(List.of(grid,
                    VortexGrid.toBuilder(grid).shortName("temperature").fullName("temperature")
                            .description("temperature").units("degC").build())).build().write();
            var variables = DataReader.getVariables(output.toString());
            assertEquals(2, variables.size());
            var validation = RasterProjectionValidation.validateSource(output.toString(), variables);
            assertTrue(validation.isValid(), validation.getMessages().toString());
            assertTrue(validation.getMessages().isEmpty());
        } finally {
            if (extension.equals("dss")) {
                HecDataManager.closeAllFiles();
            }
        }
    }

    @Test
    void UnreadableVariableSourceReportsAValidationFailure() {
        String path = directory.resolve("missing.nc").toString();
        var validation = RasterProjectionValidation.validateSource(path, List.of("precipitation"));
        assertFalse(validation.isValid());
        assertTrue(validation.getMessages().get(0).contains(path));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void validatesNetcdfProjectionWithoutReadingGridValues(boolean useVariableDsReader) throws Exception {
        Path source = directory.resolve("metadata-only.nc");
        var builder = NetcdfFormatWriter.createNewNetcdf3(source.toString());
        builder.addDimension("latitude", 2);
        builder.addDimension("longitude", 2);
        builder.addVariable("latitude", DataType.DOUBLE, "latitude")
                .addAttribute(new Attribute("units", "degrees_north"));
        builder.addVariable("longitude", DataType.DOUBLE, "longitude")
                .addAttribute(new Attribute("units", "degrees_east"));
        builder.addVariable("precipitation", DataType.FLOAT, "latitude longitude")
                .addAttribute(new Attribute("units", "mm"));
        try (var writer = builder.build()) {
            writer.write("latitude", Array.factory(DataType.DOUBLE, new int[]{2}, new double[]{46, 47}));
            writer.write("longitude", Array.factory(DataType.DOUBLE, new int[]{2}, new double[]{-124, -123}));
            writer.write("precipitation", Array.factory(DataType.FLOAT, new int[]{2, 2}, new float[]{1, 2, 3, 4}));
        }
        MetadataOnlyNetcdfReader.activeLocation = source.toString().replace('\\', '/');
        try {
            if (useVariableDsReader) {
                try (var dataset = NetcdfDatasets.openDataset(source.toString());
                     var reader = new VariableDsReader(dataset, (VariableDS) dataset.findVariable("precipitation"),
                             "precipitation")) {
                    assertThrows(DataReadException.class, () -> reader.getDto(0));
                    MetadataOnlyNetcdfReader.gridReads.set(0);
                    Validation validation = RasterProjectionValidation.validateReaderProjection(reader);
                    assertTrue(validation.isValid(), validation.getMessages().toString());
                    assertTrue(validation.getMessages().isEmpty());
                    assertEquals(0, MetadataOnlyNetcdfReader.gridReads.get());
                }
                return;
            }
            try (var reader = DataReader.builder().path(source.toString()).variable("precipitation").build()) {
                assertInstanceOf(GridDatasetReader.class, reader);
                assertThrows(DataReadException.class, () -> reader.getDto(0), "The fixture must reject grid-value reads");
            }
            MetadataOnlyNetcdfReader.gridReads.set(0);
            Validation validation = RasterProjectionValidation.validateSource(source.toString(), List.of("precipitation"));
            assertTrue(validation.isValid(), validation.getMessages().toString());
            assertTrue(validation.getMessages().isEmpty());
            assertEquals(0, MetadataOnlyNetcdfReader.gridReads.get());
        } finally {
            MetadataOnlyNetcdfReader.activeLocation = null;
        }
    }

    /** Reads real NetCDF coordinates but fails if validation requests the meteorological values. */
    public static class MetadataOnlyNetcdfReader extends N3raf {
        private static final AtomicInteger gridReads = new AtomicInteger();
        private static volatile String activeLocation;

        @Override
        public boolean isValidFile(RandomAccessFile file) throws IOException {
            return file.getLocation().replace('\\', '/').equals(activeLocation) && super.isValidFile(file);
        }

        @Override
        public Array readData(Variable variable, Section section) throws IOException, InvalidRangeException {
            if (variable.getShortName().equals("precipitation")) {
                gridReads.incrementAndGet();
                throw new IOException("Grid values must not be read during projection validation");
            }
            return super.readData(variable, section);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "invalid WKT"})
    void validatesLaterDssRecordsWhenSelectionExpandsToAFullSeries(String secondProjection) throws Exception {
        Path source = directory.resolve("series.dss");
        String first = "/A/B/PRECIPITATION/01JAN2000:0000/01JAN2000:0100/F/";
        String second = "/A/B/PRECIPITATION/01JAN2000:0100/01JAN2000:0200/F/";
        try {
            writeDssRecord(source, first, 0, WktFactory.fromEpsg(4326));
            writeDssRecord(source, second, 1, secondProjection);

            // Importing only the projected record must remain valid.
            assertTrue(RasterProjectionValidation.validateSource(source.toString(), List.of(first)).isValid());

            // Gap filling and time-step resampling expand this selection to every time record.
            var series = DssUtil.condenseVariables(source.toString(), Set.of(first));
            Validation validation = RasterProjectionValidation.validateSource(source.toString(), series);
            assertFalse(validation.isValid(), "A later record's projection must block the full series");
            assertEquals(1, validation.getMessages().size());
            String message = validation.getMessages().get(0);
            assertTrue(message.contains(source.toString()));
            assertTrue(message.contains(second), "Identify the offending DSS record");
            assertTrue(message.contains(secondProjection.isEmpty() ? "no projection" : "invalid projection"));
        } finally {
            HecDataManager.closeAllFiles();
        }
    }

    @Test
    void acceptsFullyProjectedDssSeriesWithoutValidatingUnrelatedRecords() throws Exception {
        Path source = directory.resolve("series.dss");
        String first = "/A/B/PRECIPITATION/01JAN2000:0000/01JAN2000:0100/F/";
        try {
            writeDssRecord(source, first, 0, WktFactory.fromEpsg(4326));
            writeDssRecord(source, "/A/B/PRECIPITATION/01JAN2000:0100/01JAN2000:0200/F/",
                    1, WktFactory.fromEpsg(4326));
            writeDssRecord(source, "/A/OTHER/PRECIPITATION/01JAN2000:0100/01JAN2000:0200/F/", 1, "");

            var series = DssUtil.condenseVariables(source.toString(), Set.of(first));
            Validation validation = RasterProjectionValidation.validateSource(source.toString(), series);
            assertTrue(validation.isValid(), validation.getMessages().toString());
            assertTrue(validation.getMessages().isEmpty());
        } finally {
            HecDataManager.closeAllFiles();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"/TrinityXMRG2DSS.dss", "/normalizer/qpe.dss", "/normalizer/prism.dss"})
    void acceptsExistingDssProjectionsFromRecordMetadata(String resource) throws Exception {
        String source = Path.of(getClass().getResource(resource).toURI()).toString();
        try {
            var series = DssUtil.condenseVariables(source, DataReader.getVariables(source));
            assertFalse(series.isEmpty());
            Validation validation = RasterProjectionValidation.validateSource(source, series);
            assertTrue(validation.isValid(), validation.getMessages().toString());
            assertTrue(validation.getMessages().isEmpty());
        } finally {
            HecDataManager.closeAllFiles();
        }
    }

    private void writeDssRecord(Path source, String pathname, int hour, String projection) {
        ZonedDateTime time = ZonedDateTime.parse("2000-01-01T00:00:00Z").plusHours(hour);
        VortexGrid grid = VortexGrid.builder()
                .nx(2).ny(2).dx(0.1).dy(-0.1).originX(-124).originY(47)
                .wkt(WktFactory.fromEpsg(4326)).data(new float[]{1, 2, 3, 4}).noDataValue(-9999)
                .units("mm").shortName("precipitation").fullName("precipitation").description("precipitation")
                .startTime(time).endTime(time.plusHours(1)).interval(Duration.ofHours(1))
                .dataType(VortexDataType.ACCUMULATION).build();
        GridInfo info = DssUtil.getGridInfo(grid);
        // Store malformed or missing WKT directly, without parsing it in the Vortex writer.
        ((SpecifiedGridInfo) info).setSpatialReference("", projection, 0, 0);
        GriddedData accessor = new GriddedData();
        try {
            accessor.setDSSFileName(source.toString());
            accessor.setPathname(pathname);
            assertEquals(0, accessor.storeGriddedData(info, new GridData(grid.data(), info)));
        } finally {
            accessor.done();
        }
    }

    private Path createRaster(String basename, String extension, boolean projected) throws Exception {
        GdalRegister.getInstance();
        Path raster = directory.resolve(basename + "." + extension);
        if (extension.equals("asc")) {
            Files.writeString(raster, """
                    ncols 2
                    nrows 2
                    xllcorner -124
                    yllcorner 46
                    cellsize 0.004166666667
                    NODATA_value -9999
                    1 2
                    3 4
                    """);
            if (projected) {
                Files.writeString(directory.resolve(basename + ".prj"), WktFactory.fromEpsg(4267));
            }
        } else {
            String driver = extension.equals("bil") ? "EHdr" : "GTiff";
            Dataset dataset = gdal.GetDriverByName(driver)
                    .Create(raster.toString(), 2, 2, 1, gdalconst.GDT_Float32);
            assertNotNull(dataset);
            try {
                dataset.SetGeoTransform(new double[]{-124, 0.004166666667, 0, 46, 0, -0.004166666667});
                if (projected) {
                    dataset.SetProjection(WktFactory.fromEpsg(4267));
                }
            } finally {
                dataset.delete();
            }
        }
        return raster;
    }
}
