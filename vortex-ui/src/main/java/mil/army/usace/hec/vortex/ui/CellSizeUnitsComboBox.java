package mil.army.usace.hec.vortex.ui;

import mil.army.usace.hec.vortex.geo.CellSizeUnits;

import javax.swing.*;

public class CellSizeUnitsComboBox extends JComboBox<String> {

    public CellSizeUnitsComboBox() {
        setModel(new DefaultComboBoxModel<>(CellSizeUnits.getDisplayStrings()));
    }
}
