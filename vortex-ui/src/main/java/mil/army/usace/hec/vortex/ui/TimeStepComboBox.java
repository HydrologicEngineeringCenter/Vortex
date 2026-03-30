package mil.army.usace.hec.vortex.ui;

import mil.army.usace.hec.vortex.math.TimeStep;

import javax.swing.*;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

class TimeStepComboBox extends JComboBox<String> {
    private static final Map<Integer, String> INTERVAL_TO_KEY = Map.ofEntries(
            Map.entry(1, "TimeStep_1min"),
            Map.entry(2, "TimeStep_2min"),
            Map.entry(3, "TimeStep_3min"),
            Map.entry(4, "TimeStep_4min"),
            Map.entry(5, "TimeStep_5min"),
            Map.entry(6, "TimeStep_6min"),
            Map.entry(10, "TimeStep_10min"),
            Map.entry(15, "TimeStep_15min"),
            Map.entry(20, "TimeStep_20min"),
            Map.entry(30, "TimeStep_30min"),
            Map.entry(60, "TimeStep_1hr"),
            Map.entry(120, "TimeStep_2hr"),
            Map.entry(180, "TimeStep_3hr"),
            Map.entry(240, "TimeStep_4hr"),
            Map.entry(360, "TimeStep_6hr"),
            Map.entry(480, "TimeStep_8hr"),
            Map.entry(720, "TimeStep_12hr"),
            Map.entry(1440, "TimeStep_24hr"),
            Map.entry(10080, "TimeStep_week"),
            Map.entry(43200, "TimeStep_month"),
            Map.entry(525600, "TimeStep_year")
    );

    private final Map<String, TimeStep> displayToTimeStep = new LinkedHashMap<>();

    TimeStepComboBox() {
        Arrays.stream(TimeStep.values()).forEach(ts -> {
            String key = INTERVAL_TO_KEY.get(ts.intervalInMinutes());
            if (key == null) return;
            String displayText = Text.format(key);
            if (displayText == null) return;
            displayToTimeStep.put(displayText, ts);
        });

        setModel(new DefaultComboBoxModel<>(displayToTimeStep.keySet().toArray(new String[0])));

        setSelectedIndex(10); // Default to 1 Hour
    }

    TimeStep getSelected() {
        String selected = String.valueOf(getSelectedItem());
        return displayToTimeStep.get(selected);
    }
}
