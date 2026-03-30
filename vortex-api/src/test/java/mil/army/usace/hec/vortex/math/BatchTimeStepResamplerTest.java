package mil.army.usace.hec.vortex.math;

import hec.heclib.dss.HecDataManager;
import mil.army.usace.hec.vortex.TestUtil;
import mil.army.usace.hec.vortex.VortexProperty;
import mil.army.usace.hec.vortex.io.DataReader;
import mil.army.usace.hec.vortex.util.DssUtil;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class BatchTimeStepResamplerTest {

    @Test
    void hourlyToDailyProducesExpectedGridCount() {
        String source = TestUtil.getResourceFile("/truckee/truckee_river_qpe.dss").toString();
        String destination = TestUtil.createTempFile("time_step_resampler_out.dss");
        assertNotNull(destination);

        try {
            Set<String> variables = DataReader.getVariables(source);
            assertFalse(variables.isEmpty(), "Source should have variables");

            BatchTimeStepResampler resampler = BatchTimeStepResampler.builder()
                    .pathToInput(source)
                    .variables(variables)
                    .timeStep(TimeStep.HR_24)
                    .destination(destination)
                    .writeOptions(Map.of())
                    .build();

            resampler.run();

            Set<String> destinationVariables = DataReader.getVariables(destination);
            assertFalse(destinationVariables.isEmpty(), "Destination should have variables");
        } finally {
            try {
                HecDataManager.close(destination, false);
                Files.deleteIfExists(Path.of(destination));
            } catch (IOException e) {
                Logger.getAnonymousLogger().log(Level.SEVERE, e, e::getMessage);
            }
        }
    }

    @Test
    void hourlyToSixHourProducesMoreGridsThanDaily() {
        String source = TestUtil.getResourceFile("/truckee/truckee_river_qpe.dss").toString();
        String destDaily = TestUtil.createTempFile("time_step_resampler_daily.dss");
        String dest6hr = TestUtil.createTempFile("time_step_resampler_6hr.dss");
        assertNotNull(destDaily);
        assertNotNull(dest6hr);

        try {
            Set<String> variables = DataReader.getVariables(source);

            BatchTimeStepResampler dailyResampler = BatchTimeStepResampler.builder()
                    .pathToInput(source)
                    .variables(variables)
                    .timeStep(TimeStep.HR_24)
                    .destination(destDaily)
                    .writeOptions(Map.of())
                    .build();
            dailyResampler.run();

            BatchTimeStepResampler sixHrResampler = BatchTimeStepResampler.builder()
                    .pathToInput(source)
                    .variables(variables)
                    .timeStep(TimeStep.HR_6)
                    .destination(dest6hr)
                    .writeOptions(Map.of())
                    .build();
            sixHrResampler.run();

            int dailyCount = DataReader.getVariables(destDaily).size();
            int sixHrCount = DataReader.getVariables(dest6hr).size();

            assertTrue(sixHrCount > dailyCount,
                    "6-hour output (" + sixHrCount + ") should have more grids than daily (" + dailyCount + ")");
        } finally {
            try {
                HecDataManager.close(destDaily, false);
                Files.deleteIfExists(Path.of(destDaily));
            } catch (IOException e) {
                Logger.getAnonymousLogger().log(Level.SEVERE, e, e::getMessage);
            }
            try {
                HecDataManager.close(dest6hr, false);
                Files.deleteIfExists(Path.of(dest6hr));
            } catch (IOException e) {
                Logger.getAnonymousLogger().log(Level.SEVERE, e, e::getMessage);
            }
        }
    }

    @Test
    void progressEventsAreFired() {
        String source = TestUtil.getResourceFile("/truckee/truckee_river_qpe.dss").toString();
        String destination = TestUtil.createTempFile("time_step_resampler_progress.dss");
        assertNotNull(destination);

        try {
            Set<String> variables = DataReader.getVariables(source);

            BatchTimeStepResampler resampler = BatchTimeStepResampler.builder()
                    .pathToInput(source)
                    .variables(variables)
                    .timeStep(TimeStep.HR_24)
                    .destination(destination)
                    .writeOptions(Map.of())
                    .build();

            List<String> statusMessages = new ArrayList<>();
            List<Integer> progressValues = new ArrayList<>();
            boolean[] completed = {false};

            resampler.addPropertyChangeListener(evt -> {
                VortexProperty property = VortexProperty.parse(evt.getPropertyName());
                if (VortexProperty.PROGRESS == property && evt.getNewValue() instanceof Integer) {
                    progressValues.add((Integer) evt.getNewValue());
                } else if (VortexProperty.STATUS == property) {
                    statusMessages.add(String.valueOf(evt.getNewValue()));
                } else if (VortexProperty.COMPLETE == property) {
                    completed[0] = true;
                }
            });

            resampler.run();

            assertFalse(statusMessages.isEmpty(), "Should have received status messages");
            assertFalse(progressValues.isEmpty(), "Should have received progress updates");
            assertTrue(completed[0], "Should have received completion event");
            assertEquals(100, progressValues.get(progressValues.size() - 1), "Final progress should be 100");
        } finally {
            try {
                HecDataManager.close(destination, false);
                Files.deleteIfExists(Path.of(destination));
            } catch (IOException e) {
                Logger.getAnonymousLogger().log(Level.SEVERE, e, e::getMessage);
            }
        }
    }

    @Test
    void condenseVariablesReducesDssPathnames() {
        String source = TestUtil.getResourceFile("/truckee/truckee_river_qpe.dss").toString();

        Set<String> variables = DataReader.getVariables(source);
        Set<String> condensed = DssUtil.condenseVariables(source, variables);

        assertTrue(condensed.size() <= variables.size(),
                "Condensed variables (" + condensed.size() + ") should be <= original (" + variables.size() + ")");
        assertFalse(condensed.isEmpty(), "Condensed variables should not be empty");

        for (String variable : condensed) {
            assertTrue(variable.contains("*"), "Condensed DSS pathname should contain wildcard: " + variable);
        }
    }

    @Test
    void nonDssPathReturnsVariablesUnchanged() {
        Set<String> variables = Set.of("temperature", "precipitation");
        Set<String> condensed = DssUtil.condenseVariables("/some/path/data.nc", variables);

        assertEquals(variables, condensed, "Non-DSS variables should be returned unchanged");
    }
}
