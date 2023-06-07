package mil.army.usace.hec.vortex.ui;

import mil.army.usace.hec.vortex.geo.ResamplingMethod;

import javax.swing.*;
import java.awt.*;

public class ResamplingMethodSelectionPanel extends JPanel {
    private final ResamplingMethodComboBox resamplingMethodComboBox;

    public ResamplingMethodSelectionPanel() {
        JLabel resamplingMethodLabel = new JLabel(TextProperties.getInstance().getProperty("ImportMetWizResamplingMethodL"));
        JPanel resamplingMethodLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        resamplingMethodLabelPanel.add(resamplingMethodLabel);

        resamplingMethodComboBox = new ResamplingMethodComboBox();
        JPanel resamplingMethodComboBoxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        resamplingMethodComboBoxPanel.add(resamplingMethodComboBox);

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(resamplingMethodLabelPanel);
        add(resamplingMethodComboBoxPanel);
    }

    public ResamplingMethod getSelected() {
        return resamplingMethodComboBox.getSelected();
    }

    public void refresh() {
        resamplingMethodComboBox.refresh();
    }
}
