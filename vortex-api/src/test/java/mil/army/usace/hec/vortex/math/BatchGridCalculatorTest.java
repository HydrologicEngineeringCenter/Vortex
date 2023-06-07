package mil.army.usace.hec.vortex.math;

import hec.heclib.dss.HecDSSUtilities;
import hec.heclib.grid.GridData;
import hec.heclib.grid.GridInfo;
import hec.heclib.grid.GriddedData;
import mil.army.usace.hec.vortex.TestUtil;
import mil.army.usace.hec.vortex.geo.ResamplingMethod;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.*;

class BatchGridCalculatorTest {

    @BeforeAll
    static void clean() {
        String destination = TestUtil.getResourceFile("/calculator/calculated.dss").toString();
        HecDSSUtilities dssUtilities = new HecDSSUtilities();
        dssUtilities.setDSSFileName(destination);
        String[] paths = dssUtilities.getCatalog(false);
        dssUtilities.delete(new Vector<>(Arrays.asList(paths)));
        dssUtilities.done();
    }

    @Test
    void multiply() {
        String pathToSource = TestUtil.getResourceFile("/normalizer/qpe.dss").toString();

        List<String> variables = new ArrayList<>();
        variables.add("///PRECIPITATION/02JAN2017:1200/02JAN2017:1300//");

        Operation operation = Operation.MULTIPLY;

        String pathToRaster = TestUtil.getResourceFile("/calculator/constant.tiff").toString();

        ResamplingMethod resamplingMethod = ResamplingMethod.BILINEAR;

        String destination = TestUtil.getResourceFile("/calculator/calculated.dss").toString();

        Map<String, String> writeOptions = new HashMap<>();
        writeOptions.put("partF", "MULTIPLY");

        BatchGridCalculator batchGridCalculator = BatchGridCalculator.builder()
                .pathToInput(pathToSource)
                .variables(variables)
                .setOperation(operation)
                .setPathToRaster(pathToRaster)
                .setResamplingMethod(resamplingMethod)
                .destination(destination)
                .writeOptions(writeOptions)
                .build();

        batchGridCalculator.process();

        int[] status = new int[1];
        GriddedData griddedData = new GriddedData();
        griddedData.setDSSFileName(destination);
        griddedData.setPathname("///PRECIPITATION/02JAN2017:1200/02JAN2017:1300/MULTIPLY/");
        GridData gridData = new GridData();
        griddedData.retrieveGriddedData(true, gridData, status);
        if (status[0] < 0) {
            Assertions.fail();
        }
        griddedData.done();

        GridInfo gridInfo = gridData.getGridInfo();
        // Max of source grid is 1.7
        Assertions.assertEquals(3.4, gridInfo.getMaxValue(), 1E-3);
    }

    @Test
    void divide() {
        String pathToSource = TestUtil.getResourceFile("/normalizer/qpe.dss").toString();

        List<String> variables = new ArrayList<>();
        variables.add("///PRECIPITATION/02JAN2017:1200/02JAN2017:1300//");

        Operation operation = Operation.DIVIDE;

        String pathToRaster = TestUtil.getResourceFile("/calculator/constant.tiff").toString();

        ResamplingMethod resamplingMethod = ResamplingMethod.BILINEAR;

        String destination = TestUtil.getResourceFile("/calculator/calculated.dss").toString();

        Map<String, String> writeOptions = new HashMap<>();
        writeOptions.put("partF", "DIVIDE");

        BatchGridCalculator batchGridCalculator = BatchGridCalculator.builder()
                .pathToInput(pathToSource)
                .variables(variables)
                .setOperation(operation)
                .setPathToRaster(pathToRaster)
                .setResamplingMethod(resamplingMethod)
                .destination(destination)
                .writeOptions(writeOptions)
                .build();

        batchGridCalculator.process();

        int[] status = new int[1];
        GriddedData griddedData = new GriddedData();
        griddedData.setDSSFileName(destination);
        griddedData.setPathname("///PRECIPITATION/02JAN2017:1200/02JAN2017:1300/DIVIDE/");
        GridData gridData = new GridData();
        griddedData.retrieveGriddedData(true, gridData, status);
        if (status[0] < 0) {
            Assertions.fail();
        }
        griddedData.done();

        GridInfo gridInfo = gridData.getGridInfo();
        // Max of source grid is 1.7
        Assertions.assertEquals(0.85, gridInfo.getMaxValue(), 1E-3);
    }

    @Test
    void add() {
        String pathToSource = TestUtil.getResourceFile("/normalizer/qpe.dss").toString();

        List<String> variables = new ArrayList<>();
        variables.add("///PRECIPITATION/02JAN2017:1200/02JAN2017:1300//");

        Operation operation = Operation.ADD;

        String pathToRaster = TestUtil.getResourceFile("/calculator/constant.tiff").toString();

        ResamplingMethod resamplingMethod = ResamplingMethod.BILINEAR;

        String destination = TestUtil.getResourceFile("/calculator/calculated.dss").toString();

        Map<String, String> writeOptions = new HashMap<>();
        writeOptions.put("partF", "ADD");

        BatchGridCalculator batchGridCalculator = BatchGridCalculator.builder()
                .pathToInput(pathToSource)
                .variables(variables)
                .setOperation(operation)
                .setPathToRaster(pathToRaster)
                .setResamplingMethod(resamplingMethod)
                .destination(destination)
                .writeOptions(writeOptions)
                .build();

        batchGridCalculator.process();

        int[] status = new int[1];
        GriddedData griddedData = new GriddedData();
        griddedData.setDSSFileName(destination);
        griddedData.setPathname("///PRECIPITATION/02JAN2017:1200/02JAN2017:1300/ADD/");
        GridData gridData = new GridData();
        griddedData.retrieveGriddedData(true, gridData, status);
        if (status[0] < 0) {
            Assertions.fail();
        }
        griddedData.done();

        GridInfo gridInfo = gridData.getGridInfo();
        // Max of source grid is 1.7
        Assertions.assertEquals(3.7, gridInfo.getMaxValue(), 1E-3);
    }

    @Test
    void subtract() {
        String pathToSource = TestUtil.getResourceFile("/normalizer/qpe.dss").toString();

        List<String> variables = new ArrayList<>();
        variables.add("///PRECIPITATION/02JAN2017:1200/02JAN2017:1300//");

        Operation operation = Operation.SUBTRACT;

        String pathToRaster = TestUtil.getResourceFile("/calculator/constant.tiff").toString();

        ResamplingMethod resamplingMethod = ResamplingMethod.BILINEAR;

        String destination = TestUtil.getResourceFile("/calculator/calculated.dss").toString();

        Map<String, String> writeOptions = new HashMap<>();
        writeOptions.put("partF", "SUBTRACT");

        BatchGridCalculator batchGridCalculator = BatchGridCalculator.builder()
                .pathToInput(pathToSource)
                .variables(variables)
                .setOperation(operation)
                .setPathToRaster(pathToRaster)
                .setResamplingMethod(resamplingMethod)
                .destination(destination)
                .writeOptions(writeOptions)
                .build();

        batchGridCalculator.process();

        int[] status = new int[1];
        GriddedData griddedData = new GriddedData();
        griddedData.setDSSFileName(destination);
        griddedData.setPathname("///PRECIPITATION/02JAN2017:1200/02JAN2017:1300/SUBTRACT/");
        GridData gridData = new GridData();
        griddedData.retrieveGriddedData(true, gridData, status);
        if (status[0] < 0) {
            Assertions.fail();
        }
        griddedData.done();

        GridInfo gridInfo = gridData.getGridInfo();
        // Max of source grid is 1.7
        Assertions.assertEquals(-0.3, gridInfo.getMaxValue(), 1E-3);
    }
}