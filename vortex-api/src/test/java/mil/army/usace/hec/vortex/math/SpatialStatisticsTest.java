package mil.army.usace.hec.vortex.math;

import hec.heclib.dss.DSSPathname;
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

class SpatialStatisticsTest {

    /**
     * Clip the HMS spatial results to the same spatial extents as the LiDAR
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

    @Test
    void tiSnowDepthDataWriterTest(){
        String variableName = "SNOW DEPTH";
        String outputDirectory = "C:\\PROJECTS\\SIRO\\HMS\\MoresCreek\\Full\\MoresCreek\\maps\\WY2023-2025_TI_HRRR_1000m\\";
        String inFile = outputDirectory + "swe.dss";

        Map<String, String> variablesAndOutputFileNames = Map.ofEntries(
                Map.entry("//WY2023-2025_TI_1000m/SWE/08DEC2022:0000/08DEC2022:2400/RUN:WY2023-2025_TI_HRRR_1000m/","TI_snow_depth_2022_12_08T0000_2022_12_08T2400"),
                Map.entry("//WY2023-2025_TI_1000m/SWE/09FEB2023:0000/09FEB2023:2400/RUN:WY2023-2025_TI_HRRR_1000m/","TI_snow_depth_2023_02_09T0000_2023_02_09T2400"),
                Map.entry("//WY2023-2025_TI_1000m/SWE/16MAR2023:0000/16MAR2023:2400/RUN:WY2023-2025_TI_HRRR_1000m/","TI_snow_depth_2023_03_16T0000_2023_03_16T2400"),
                Map.entry("//WY2023-2025_TI_1000m/SWE/05APR2023:0000/05APR2023:2400/RUN:WY2023-2025_TI_HRRR_1000m/","TI_snow_depth_2023_04_05T0000_2023_04_05T2400"),
                Map.entry("//WY2023-2025_TI_1000m/SWE/28DEC2023:0000/28DEC2023:2400/RUN:WY2023-2025_TI_HRRR_1000m/","TI_snow_depth_2023_12_28T0000_2023_12_28T2400"),
                Map.entry("//WY2023-2025_TI_1000m/SWE/15JAN2024:0000/15JAN2024:2400/RUN:WY2023-2025_TI_HRRR_1000m/","TI_snow_depth_2024_01_15T0000_2024_01_15T2400"),
                Map.entry("//WY2023-2025_TI_1000m/SWE/12FEB2024:0000/12FEB2024:2400/RUN:WY2023-2025_TI_HRRR_1000m/","TI_snow_depth_2024_02_12T0000_2024_02_12T2400"),
                Map.entry("//WY2023-2025_TI_1000m/SWE/15MAR2024:0000/15MAR2024:2400/RUN:WY2023-2025_TI_HRRR_1000m/","TI_snow_depth_2024_03_15T0000_2024_03_15T2400"),
                Map.entry("//WY2023-2025_TI_1000m/SWE/15APR2024:0000/15APR2024:2400/RUN:WY2023-2025_TI_HRRR_1000m/","TI_snow_depth_2024_04_15T0000_2024_04_15T2400"),
                Map.entry("//WY2023-2025_TI_1000m/SWE/13JAN2025:0000/13JAN2025:2400/RUN:WY2023-2025_TI_HRRR_1000m/","TI_snow_depth_2025_01_13T0000_2025_01_13T2400"),
                Map.entry("//WY2023-2025_TI_1000m/SWE/28JAN2025:0000/28JAN2025:2400/RUN:WY2023-2025_TI_HRRR_1000m/","TI_snow_depth_2025_01_28T0000_2025_01_28T2400"),
                Map.entry("//WY2023-2025_TI_1000m/SWE/04APR2025:0000/04APR2025:2400/RUN:WY2023-2025_TI_HRRR_1000m/","TI_snow_depth_2025_04_04T0000_2025_04_04T2400"),
                Map.entry("//WY2023-2025_TI_1000m/SWE/01MAY2025:0000/01MAY2025:2400/RUN:WY2023-2025_TI_HRRR_1000m/","TI_snow_depth_2025_05_01T0000_2025_05_01T2400"));

// density interpreted from Mores Creek Summit SNOTEL station for each date of interest
// SWE * density = snow depth
        Map<String, Double> variablesAndDensities = Map.ofEntries(
                Map.entry("//WY2023-2025_TI_1000m/SWE/08DEC2022:0000/08DEC2022:2400/RUN:WY2023-2025_TI_HRRR_1000m/",3.87),
                Map.entry("//WY2023-2025_TI_1000m/SWE/09FEB2023:0000/09FEB2023:2400/RUN:WY2023-2025_TI_HRRR_1000m/",3.27),
                Map.entry("//WY2023-2025_TI_1000m/SWE/16MAR2023:0000/16MAR2023:2400/RUN:WY2023-2025_TI_HRRR_1000m/",2.98),
                Map.entry("//WY2023-2025_TI_1000m/SWE/05APR2023:0000/05APR2023:2400/RUN:WY2023-2025_TI_HRRR_1000m/",2.83),
                Map.entry("//WY2023-2025_TI_1000m/SWE/28DEC2023:0000/28DEC2023:2400/RUN:WY2023-2025_TI_HRRR_1000m/",3.06),
                Map.entry("//WY2023-2025_TI_1000m/SWE/15JAN2024:0000/15JAN2024:2400/RUN:WY2023-2025_TI_HRRR_1000m/",4.15),
                Map.entry("//WY2023-2025_TI_1000m/SWE/12FEB2024:0000/12FEB2024:2400/RUN:WY2023-2025_TI_HRRR_1000m/",3.05),
                Map.entry("//WY2023-2025_TI_1000m/SWE/15MAR2024:0000/15MAR2024:2400/RUN:WY2023-2025_TI_HRRR_1000m/",3.03),
                Map.entry("//WY2023-2025_TI_1000m/SWE/15APR2024:0000/15APR2024:2400/RUN:WY2023-2025_TI_HRRR_1000m/",2.32),
                Map.entry("//WY2023-2025_TI_1000m/SWE/13JAN2025:0000/13JAN2025:2400/RUN:WY2023-2025_TI_HRRR_1000m/",3.33),
                Map.entry("//WY2023-2025_TI_1000m/SWE/28JAN2025:0000/28JAN2025:2400/RUN:WY2023-2025_TI_HRRR_1000m/",3.2),
                Map.entry("//WY2023-2025_TI_1000m/SWE/04APR2025:0000/04APR2025:2400/RUN:WY2023-2025_TI_HRRR_1000m/",2.36),
                Map.entry("//WY2023-2025_TI_1000m/SWE/01MAY2025:0000/01MAY2025:2400/RUN:WY2023-2025_TI_HRRR_1000m/",2.12));

        for (String variable : variablesAndOutputFileNames.keySet()) {
            String outputFileName = outputDirectory + variablesAndOutputFileNames.get(variable) + ".tif";
            Double density = variablesAndDensities.get(variable);

            DataReader reader = DataReader.builder()
                    .path(inFile)
                    .variable(variable)
                    .build();

            List<VortexData> dtos = new ArrayList<>(reader.getDtos());

            List<VortexData> outputGrids = new ArrayList<>();
            VortexGrid vortexGrid = (VortexGrid) dtos.get(0);

            Calculator calculator = Calculator.builder()
                    .inputGrid(vortexGrid)
                    .multiplyValue(density.floatValue())
                    .build();

            VortexGrid intermediateSnowDepthGrid = calculator.calculate();

            // override the short name, full name, and description to refer to snow depth
            DSSPathname dssPathname = new DSSPathname(vortexGrid.fullName());
            dssPathname.setCPart(variableName);

            VortexGrid outputGrid = VortexGrid.toBuilder(intermediateSnowDepthGrid)
                    .shortName(variableName)
                    .fullName(dssPathname.getPathname())
                    .description(variableName)
                    .build();

            outputGrids.add(outputGrid);

            Path destination = Paths.get(outputFileName);

            DataWriter writer = DataWriter.builder()
                    .destination(destination)
                    .data(outputGrids)
                    .build();

            writer.write();
        }
    }
}