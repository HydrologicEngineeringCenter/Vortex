package mil.army.usace.hec.vortex.ui;

import mil.army.usace.hec.vortex.geo.ResamplingMethod;

import javax.swing.*;
import java.util.Arrays;

public class ResamplingMethodComboBox extends JComboBox<String> {

    public ResamplingMethodComboBox() {
        String[] methods = Arrays.stream(ResamplingMethod.values())
                .map(ResamplingMethod::getDisplayString)
                .toArray(String[]::new);

        setModel(new DefaultComboBoxModel<>(methods));
        refresh();
    }

    public ResamplingMethod getSelected() {
        String selected = String.valueOf(getSelectedItem());
        return ResamplingMethod.fromString(selected);
    }

    public void refresh() {
        setSelectedItem(ResamplingMethod.BILINEAR.getDisplayString());
    }
}
