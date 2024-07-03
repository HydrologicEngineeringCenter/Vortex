package mil.army.usace.hec.vortex.ui;

import mil.army.usace.hec.vortex.math.ShiftTimeUnit;

import javax.swing.*;
import java.util.Arrays;

class ShiftTimeUnitComboBox extends JComboBox<String> {

    ShiftTimeUnitComboBox() {
        String[] selections = Arrays.stream(ShiftTimeUnit.values())
                .map(ShiftTimeUnit::toString)
                .toArray(String[]::new);

        setModel(new DefaultComboBoxModel<>(selections));

        setSelectedIndex(1);
    }

    ShiftTimeUnit getSelected() {
        String selected = String.valueOf(getSelectedItem());
        return ShiftTimeUnit.fromString(selected);
    }
}
