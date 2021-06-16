package mil.army.usace.hec.vortex.ui;

import javax.swing.*;
import java.util.ArrayList;

public class CellSizesComboBox extends JComboBox<String> {

    public CellSizesComboBox() {
        ArrayList<String> cellSizeChoices = new ArrayList<>();
        cellSizeChoices.add("10");
        cellSizeChoices.add("20");
        cellSizeChoices.add("50");
        cellSizeChoices.add("100");
        cellSizeChoices.add("200");
        cellSizeChoices.add("500");
        cellSizeChoices.add("1000");
        cellSizeChoices.add("2000");
        cellSizeChoices.add("5000");
        cellSizeChoices.add("10000");
        setModel(new DefaultComboBoxModel<>(cellSizeChoices.toArray(String[]::new)));
        setSelectedItem("2000");
    }
}
