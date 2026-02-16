package mil.army.usace.hec.vortex.math;

import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.io.BatchImporter;
import mil.army.usace.hec.vortex.io.DataReader;
import mil.army.usace.hec.vortex.io.DataWriter;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class SpatialResultsTest {

    /**
     * Reads selected SWE gridded datasets from a DSS file and writes each selection to an output GeoTIFF.
     * For Mores Creek Temperature Index snow modeling.
     */
    @Test
    void tiSweDataWriterTest(){
        String outputDirectory = "C:\\Projects\\SIRO\\HMS\\MoresCreek\\maps\\WY2023-2025_TI_HRRR_2000m\\";
        String inputDirectory = "C:\\Projects\\SIRO\\HMS\\MoresCreek\\results\\WY2023-2025_TI_HRRR_2000m\\";
        String inFile = inputDirectory + "ti_swe.dss";

        Map<String, String> variablesAndOutputFileNames = Map.ofEntries(
                Map.entry("//WY2023-2025_TI/SWE/08DEC2022:0000/08DEC2022:2400/RUN:WY2023-2025_TI_HRRR/","TI_swe_2022_12_08T0000_2022_12_08T2400"),
                Map.entry("//WY2023-2025_TI/SWE/09FEB2023:0000/09FEB2023:2400/RUN:WY2023-2025_TI_HRRR/","TI_swe_2023_02_09T0000_2023_02_09T2400"),
                Map.entry("//WY2023-2025_TI/SWE/16MAR2023:0000/16MAR2023:2400/RUN:WY2023-2025_TI_HRRR/","TI_swe_2023_03_16T0000_2023_03_16T2400"),
                Map.entry("//WY2023-2025_TI/SWE/28DEC2023:0000/28DEC2023:2400/RUN:WY2023-2025_TI_HRRR/","TI_swe_2023_12_28T0000_2023_12_28T2400"),
                Map.entry("//WY2023-2025_TI/SWE/15JAN2024:0000/15JAN2024:2400/RUN:WY2023-2025_TI_HRRR/","TI_swe_2024_01_15T0000_2024_01_15T2400"),
                Map.entry("//WY2023-2025_TI/SWE/13FEB2024:0000/13FEB2024:2400/RUN:WY2023-2025_TI_HRRR/","TI_swe_2024_02_13T0000_2024_02_13T2400"),
                Map.entry("//WY2023-2025_TI/SWE/13JAN2025:0000/13JAN2025:2400/RUN:WY2023-2025_TI_HRRR/","TI_swe_2025_01_13T0000_2025_01_13T2400"),
                Map.entry("//WY2023-2025_TI/SWE/29JAN2025:0000/29JAN2025:2400/RUN:WY2023-2025_TI_HRRR/","TI_swe_2025_01_29T0000_2025_01_29T2400"));

        for (String variable : variablesAndOutputFileNames.keySet()) {
            String outputFileName = outputDirectory + variablesAndOutputFileNames.get(variable) + ".tif";

            DataReader reader = DataReader.builder()
                    .path(inFile)
                    .variable(variable)
                    .build();

            List<VortexData> dtos = new ArrayList<>(reader.getDtos());

            Path destination = Paths.get(outputFileName);

            DataWriter writer = DataWriter.builder()
                    .destination(destination)
                    .data(dtos)
                    .build();

            writer.write();
        }
    }

    /**
     * Reads selected SWE gridded datasets from a DSS file and writes each selection to an output GeoTIFF.
     * For Mores Creek Energy Budget snow modeling.
     */
    @Test
    void ebSnowDepthDataWriterTest(){
        String outputDirectory = "C:\\Projects\\SIRO\\HMS\\MoresCreek\\maps\\WY2023-2025_EB_HRRR_2000m\\";
        String inputDirectory = "C:\\Projects\\SIRO\\HMS\\MoresCreek\\results\\WY2023-2025_EB_HRRR_2000m\\";
        String inFile = inputDirectory + "eb_snow_depth.dss";

        Map<String, String> variablesAndOutputFileNames = Map.ofEntries(
                Map.entry("//WY2023-2025_EB/SNOW DEPTH/08DEC2022:0000/08DEC2022:2400/RUN:WY2023-2025_EB_HRRR/","EB_snowdepth_2022_12_08T0000_2022_12_08T2400"),
                Map.entry("//WY2023-2025_EB/SNOW DEPTH/09FEB2023:0000/09FEB2023:2400/RUN:WY2023-2025_EB_HRRR/","EB_snowdepth_2023_02_09T0000_2023_02_09T2400"),
                Map.entry("//WY2023-2025_EB/SNOW DEPTH/16MAR2023:0000/16MAR2023:2400/RUN:WY2023-2025_EB_HRRR/","EB_snowdepth_2023_03_16T0000_2023_03_16T2400"),
                Map.entry("//WY2023-2025_EB/SNOW DEPTH/28DEC2023:0000/28DEC2023:2400/RUN:WY2023-2025_EB_HRRR/","EB_snowdepth_2023_12_28T0000_2023_12_28T2400"),
                Map.entry("//WY2023-2025_EB/SNOW DEPTH/15JAN2024:0000/15JAN2024:2400/RUN:WY2023-2025_EB_HRRR/","EB_snowdepth_2024_01_15T0000_2024_01_15T2400"),
                Map.entry("//WY2023-2025_EB/SNOW DEPTH/13FEB2024:0000/13FEB2024:2400/RUN:WY2023-2025_EB_HRRR/","EB_snowdepth_2024_02_13T0000_2024_02_13T2400"),
                Map.entry("//WY2023-2025_EB/SNOW DEPTH/13JAN2025:0000/13JAN2025:2400/RUN:WY2023-2025_EB_HRRR/","EB_snowdepth_2025_01_13T0000_2025_01_13T2400"),
                Map.entry("//WY2023-2025_EB/SNOW DEPTH/29JAN2025:0000/29JAN2025:2400/RUN:WY2023-2025_EB_HRRR/","EB_snowdepth_2025_01_29T0000_2025_01_29T2400"));

        for (String variable : variablesAndOutputFileNames.keySet()) {
            String outputFileName = outputDirectory + variablesAndOutputFileNames.get(variable) + ".tif";

            DataReader reader = DataReader.builder()
                    .path(inFile)
                    .variable(variable)
                    .build();

            List<VortexData> dtos = new ArrayList<>(reader.getDtos());

            Path destination = Paths.get(outputFileName);

            DataWriter writer = DataWriter.builder()
                    .destination(destination)
                    .data(dtos)
                    .build();

            writer.write();
        }
    }

    /**
     * Infer snow depths from Temperature Index results.
     * Reads selected SWE gridded datasets from a DSS file and multiplies the values by a specified snow density (in/in).
     * SWE grids are from Mores Creek Temperature Index snow modeling. Snow densities are from Mores Creek Summit SNOTEL station.
     */
    @Test
    void tiInferredSnowDepthTest() {
        String directory = "C:\\Projects\\SIRO\\HMS\\MoresCreek\\maps\\WY2023-2025_TI_HRRR_2000m\\";
        String inFile = directory + "ti_swe_grids.dss";

        Map<String, String> variablesAndOutputFileNames = Map.ofEntries(
                Map.entry("/UTM11N/MORES CREEK/SWE/08DEC2022:0000/08DEC2022:2400/TI/", "TI_snowdepth_2022_12_08T0000_2022_12_08T2400"),
                Map.entry("/UTM11N/MORES CREEK/SWE/09FEB2023:0000/09FEB2023:2400/TI/", "TI_snowdepth_2023_02_09T0000_2023_02_09T2400"),
                Map.entry("/UTM11N/MORES CREEK/SWE/16MAR2023:0000/16MAR2023:2400/TI/", "TI_snowdepth_2023_03_16T0000_2023_03_16T2400"),
                Map.entry("/UTM11N/MORES CREEK/SWE/28DEC2023:0000/28DEC2023:2400/TI/", "TI_snowdepth_2023_12_28T0000_2023_12_28T2400"),
                Map.entry("/UTM11N/MORES CREEK/SWE/15JAN2024:0000/15JAN2024:2400/TI/", "TI_snowdepth_2024_01_15T0000_2024_01_15T2400"),
                Map.entry("/UTM11N/MORES CREEK/SWE/13FEB2024:0000/13FEB2024:2400/TI/", "TI_snowdepth_2024_02_13T0000_2024_02_13T2400"),
                Map.entry("/UTM11N/MORES CREEK/SWE/13JAN2025:0000/13JAN2025:2400/TI/", "TI_snowdepth_2025_01_13T0000_2025_01_13T2400"),
                Map.entry("/UTM11N/MORES CREEK/SWE/29JAN2025:0000/29JAN2025:2400/TI/", "TI_snowdepth_2025_01_29T0000_2025_01_29T2400"));

        // Snow densities are from Mores Creek Summit SNOTEL station
        Map<String, Double> variablesAndSnowDensities = Map.ofEntries(
                Map.entry("/UTM11N/MORES CREEK/SWE/08DEC2022:0000/08DEC2022:2400/TI/", 3.87),
                Map.entry("/UTM11N/MORES CREEK/SWE/09FEB2023:0000/09FEB2023:2400/TI/", 3.27),
                Map.entry("/UTM11N/MORES CREEK/SWE/16MAR2023:0000/16MAR2023:2400/TI/", 2.98),
                Map.entry("/UTM11N/MORES CREEK/SWE/28DEC2023:0000/28DEC2023:2400/TI/", 3.06),
                Map.entry("/UTM11N/MORES CREEK/SWE/15JAN2024:0000/15JAN2024:2400/TI/", 4.15),
                Map.entry("/UTM11N/MORES CREEK/SWE/13FEB2024:0000/13FEB2024:2400/TI/", 3.05),
                Map.entry("/UTM11N/MORES CREEK/SWE/13JAN2025:0000/13JAN2025:2400/TI/", 3.33),
                Map.entry("/UTM11N/MORES CREEK/SWE/29JAN2025:0000/29JAN2025:2400/TI/", 3.2));


        for (String dssPathname : variablesAndOutputFileNames.keySet()) {
            String outputFileName = directory + variablesAndOutputFileNames.get(dssPathname) + ".tif";

            Double densityDbl = variablesAndSnowDensities.get(dssPathname);
            if (densityDbl == null) {
                throw new IllegalStateException("No density found for DSS pathname: " + dssPathname);
            }
            float density = densityDbl.floatValue();

            // 1) Read the DSS grid for the specified pathname
            DataReader reader = DataReader.builder()
                    .path(inFile)
                    .variable(dssPathname)
                    .build();

            VortexGrid inputGrid = reader.getDtos().stream()
                    .filter(v -> v instanceof VortexGrid)
                    .map(v -> (VortexGrid) v)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No grid found in DSS for pathname: " + dssPathname));

            // 2) Multiply grid by density
            Calculator calculator = Calculator.builder()
                    .inputGrid(inputGrid)
                    .multiplyValue(density)
                    .build();

            VortexGrid calculated = calculator.calculate();

            // 3) Write to GeoTIFF
            Path destination = Paths.get(outputFileName);
            DataWriter writer = DataWriter.builder()
                    .destination(destination)
                    .data(List.of(calculated))
                    .build();

            writer.write();
        }
    }


    /**
     * Clip the DSS spatial results from HEC-HMS to the same spatial extents as the LiDAR
     */
    @Test
    void clipEbSpatialResultsTest() {
        String inputDirectory = "C:\\Projects\\SIRO\\HMS\\MoresCreek\\results\\WY2023-2025_EB_HRRR_2000m\\";
        Path shapefile = Path.of("C:/Projects/SIRO/Data/LiDAR/lidar_outline.shp");

        Map<String, String> geoOptions = new HashMap<>();
        geoOptions.put("resamplingMethod", "bilinear");
        geoOptions.put("pathToShp", shapefile.toString());

        Map<String, String> writeOptions = Map.ofEntries(
                Map.entry("partB", "WY2023-2025_EB"),
                Map.entry("partF", "RUN:WY2023-2025_EB_HRRR"));

        List<String> inFiles = new ArrayList<>();
        inFiles.add(inputDirectory + "eb_snow_depth.dss");

        // List of DSS pathnames (variables) to process
        List<String> variables = List.of(
                "//WY2023-2025_EB/SNOW DEPTH/08DEC2022:1400/08DEC2022:1500/RUN:WY2023-2025_EB_HRRR/",
                "//WY2023-2025_EB/SNOW DEPTH/09FEB2023:1400/09FEB2023:1500/RUN:WY2023-2025_EB_HRRR/",
                "//WY2023-2025_EB/SNOW DEPTH/16MAR2023:1400/16MAR2023:1500/RUN:WY2023-2025_EB_HRRR/",
                "//WY2023-2025_EB/SNOW DEPTH/28DEC2023:1300/28DEC2023:1400/RUN:WY2023-2025_EB_HRRR/",
                "//WY2023-2025_EB/SNOW DEPTH/15JAN2024:1300/15JAN2024:1400/RUN:WY2023-2025_EB_HRRR/",
                "//WY2023-2025_EB/SNOW DEPTH/12FEB2024:1300/12FEB2024:1400/RUN:WY2023-2025_EB_HRRR/",
                "//WY2023-2025_EB/SNOW DEPTH/13JAN2025:1300/13JAN2025:1400/RUN:WY2023-2025_EB_HRRR/",
                "//WY2023-2025_EB/SNOW DEPTH/28JAN2025:1300/28JAN2025:1400/RUN:WY2023-2025_EB_HRRR/");

        File outFile = new File(inputDirectory + "eb_snow_depth_clipped.dss");

        BatchImporter importer = BatchImporter.builder()
                .inFiles(inFiles)
                .variables(variables)
                .geoOptions(geoOptions)
                .destination(outFile.toString())
                .writeOptions(writeOptions)
                .build();

        importer.process();
    }

    /**
     * Compute the difference between simulated snow depth using Energy Budget snow method and LiDAR flight snow depth.
     */
    @Test
    void ebSnowDepthSimulatedObservedDifferenceTest(){
        String rasterDirectory = "C:\\Projects\\SIRO\\Data\\LiDAR\\TIFF_2000m\\";
        String outputDirectory = "C:\\Projects\\SIRO\\HMS\\MoresCreek\\results\\WY2023-2025_EB_HRRR_2000m\\";
        Path destination = Paths.get(outputDirectory + "eb_snow_depth_differences.dss");
        String inFile = outputDirectory + "eb_snow_depth_clipped.dss";

        Map<String, String> simulatedDssObservedTiff = Map.ofEntries(
                Map.entry("//WY2023-2025_EB/SNOW DEPTH/08DEC2022:1400/08DEC2022:1500/RUN:WY2023-2025_EB_HRRR/","lidar_grids_2000m_inches_utm11n_mores_creek_snow_depth_2022_12_08T1400_lidar_2km.tiff"),
                Map.entry("//WY2023-2025_EB/SNOW DEPTH/09FEB2023:1400/09FEB2023:1500/RUN:WY2023-2025_EB_HRRR/","lidar_grids_2000m_inches_utm11n_mores_creek_snow_depth_2023_02_09T1400_lidar_2km.tiff"),
                Map.entry("//WY2023-2025_EB/SNOW DEPTH/16MAR2023:1400/16MAR2023:1500/RUN:WY2023-2025_EB_HRRR/","lidar_grids_2000m_inches_utm11n_mores_creek_snow_depth_2023_03_16T1400_lidar_2km.tiff"),
                Map.entry("//WY2023-2025_EB/SNOW DEPTH/28DEC2023:1300/28DEC2023:1400/RUN:WY2023-2025_EB_HRRR/","lidar_grids_2000m_inches_utm11n_mores_creek_snow_depth_2023_12_28T1300_lidar_2km.tiff"),
                Map.entry("//WY2023-2025_EB/SNOW DEPTH/15JAN2024:1300/15JAN2024:1400/RUN:WY2023-2025_EB_HRRR/","lidar_grids_2000m_inches_utm11n_mores_creek_snow_depth_2024_01_15T1300_lidar_2km.tiff"),
                Map.entry("//WY2023-2025_EB/SNOW DEPTH/12FEB2024:1300/12FEB2024:1400/RUN:WY2023-2025_EB_HRRR/","lidar_grids_2000m_inches_utm11n_mores_creek_snow_depth_2024_02_13T1300_lidar_2km.tiff"),
                Map.entry("//WY2023-2025_EB/SNOW DEPTH/13JAN2025:1300/13JAN2025:1400/RUN:WY2023-2025_EB_HRRR/","lidar_grids_2000m_inches_utm11n_mores_creek_snow_depth_2025_01_13T1300_lidar_2km.tiff"),
                Map.entry("//WY2023-2025_EB/SNOW DEPTH/28JAN2025:1300/28JAN2025:1400/RUN:WY2023-2025_EB_HRRR/","lidar_grids_2000m_inches_utm11n_mores_creek_snow_depth_2025_01_29T1300_lidar_2km.tiff"));

        for (Map.Entry<String, String> entry : simulatedDssObservedTiff.entrySet()) {

            DataReader dssReader = DataReader.builder()
                    .path(inFile)
                    .variable(entry.getKey())
                    .build();

            String rasterFile = rasterDirectory + entry.getValue();

            DataReader rasterReader = DataReader.builder()
                    .path(rasterFile)
                    .build();

            VortexGrid raster = (VortexGrid) rasterReader.getDto(0);

            Map<String, String> writeOptions = Map.ofEntries(
                    Map.entry("partA", "UTM11N"),
                    Map.entry("partB", "WY2023-2025_EB"),
                    Map.entry("partC", ""),
                    Map.entry("partF",  "OBS - SIM")
            );

            GridCalculatableUnit unit = GridCalculatableUnit.builder()
                    .reader(dssReader)
                    .raster(raster)
                    .operation(Operation.SUBTRACT)
                    .destination(destination)
                    .writeOptions(writeOptions)
                    .build();

            unit.process();
        }
    }
}