package mil.army.usace.hec.vortex.ui;

import mil.army.usace.hec.vortex.math.Operation;

import javax.swing.*;
import java.util.Arrays;

public class OperationComboBox extends JComboBox<String> {

    public OperationComboBox() {
        String[] methods = Arrays.stream(Operation.values())
                .map(Operation::getDisplayString)
                .toArray(String[]::new);

        setModel(new DefaultComboBoxModel<>(methods));
        refresh();
    }

    public Operation getSelected() {
        String selected = String.valueOf(getSelectedItem());
        return Operation.fromDisplayString(selected);
    }

    public void refresh() {
        setSelectedItem(Operation.MULTIPLY.getDisplayString());
    }
}
