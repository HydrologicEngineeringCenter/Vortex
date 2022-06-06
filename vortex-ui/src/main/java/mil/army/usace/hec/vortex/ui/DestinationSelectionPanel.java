package mil.army.usace.hec.vortex.ui;

import mil.army.usace.hec.vortex.ui.util.FileSaveUtil;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.util.Arrays;

public class DestinationSelectionPanel extends JPanel {
    private final JFrame parent;
    private final JPanel dssPartsSelectionPanel;
    private JTextField selectDestinationTextField;
    private JTextField unitsTextField;
    private JComboBox<String> dataTypeCombo;

    @SuppressWarnings("unused")
    private JTextField dssFieldA, dssFieldB, dssFieldC, dssFieldD, dssFieldE, dssFieldF;

    public DestinationSelectionPanel(JFrame parent) {
        this.parent = parent;

        GridBagLayout gridBagLayout = new GridBagLayout();
        gridBagLayout.columnWidths = new int[]{parent.getPreferredSize().width, 0};
        gridBagLayout.rowHeights = new int[] {50, 100, 0};
        gridBagLayout.columnWeights = new double[]{1.0, Double.MIN_VALUE};
        gridBagLayout.rowWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};

        this.setLayout(gridBagLayout);
        this.setBorder(BorderFactory.createEmptyBorder(5,5,5,8));

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.anchor = GridBagConstraints.NORTH;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.gridx = 0;

        /* Select Destination Section Panel */
        gridBagConstraints.gridy = 0;
        this.add(dssFileSelectionPanel(), gridBagConstraints);

        /* DSS Parts Panel (Only appears when selected destination is a DSS file */
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new Insets(10,0,0,0);
        dssPartsSelectionPanel = dssPartsSelectionPanel();
        dssPartsSelectionPanel.setVisible(false);
        this.add(dssPartsSelectionPanel, gridBagConstraints);
    }

    private JPanel dssFileSelectionPanel() {
        /* Select Destination section (of stepFourPanel) */
        JLabel selectDestinationLabel = new JLabel(TextProperties.getInstance().getProperty("NormalizerWiz_SelectDestination_L"));
        JPanel selectDestinationLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        selectDestinationLabelPanel.add(selectDestinationLabel);

        JPanel selectDestinationTextFieldPanel = new JPanel();
        selectDestinationTextFieldPanel.setLayout(new BoxLayout(selectDestinationTextFieldPanel, BoxLayout.X_AXIS));

        selectDestinationTextFieldPanel.add(Box.createRigidArea(new Dimension(4,0)));

        selectDestinationTextField = new JTextField();
        selectDestinationTextField.setColumns(0);
        selectDestinationTextField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { textUpdated(); }
            public void removeUpdate(DocumentEvent e) { textUpdated(); }
            public void insertUpdate(DocumentEvent e) { textUpdated(); }
            void textUpdated() { dssPartsSelectionPanel.setVisible(selectDestinationTextField.getText().endsWith(".dss")); }
        });
        selectDestinationTextFieldPanel.add(selectDestinationTextField);

        selectDestinationTextFieldPanel.add(Box.createRigidArea(new Dimension(4,0)));

        FileBrowseButton selectDestinationBrowseButton = new FileBrowseButton(parent.getClass().getName(), "");
        selectDestinationBrowseButton.setIcon(IconResources.loadIcon("images/Open16.gif"));
        selectDestinationBrowseButton.setPreferredSize(new Dimension(22,22));
        selectDestinationBrowseButton.addActionListener(evt -> selectDestinationBrowseAction(selectDestinationBrowseButton));
        selectDestinationTextFieldPanel.add(selectDestinationBrowseButton);

        JPanel selectDestinationSectionPanel = new JPanel();
        selectDestinationSectionPanel.setLayout(new BoxLayout(selectDestinationSectionPanel, BoxLayout.Y_AXIS));
        selectDestinationSectionPanel.add(selectDestinationLabelPanel);
        selectDestinationSectionPanel.add(selectDestinationTextFieldPanel);

        return selectDestinationSectionPanel;
    }

    private JPanel dssPartsSelectionPanel() {
        GridBagLayout dssGridLayout = new GridBagLayout();
        JPanel dssPartsPanel = new JPanel(dssGridLayout);

        GridBagConstraints dssPartsConstraints = new GridBagConstraints();
        dssPartsConstraints.fill = GridBagConstraints.HORIZONTAL;

        dssFieldA = new JTextField();
        dssFieldB = new JTextField();
        dssFieldC = new JTextField();
        dssFieldC.setText("*");
        dssFieldC.setEditable(false);
        JTextField dssFieldD = new JTextField();
        dssFieldD.setText("*");
        dssFieldD.setEditable(false);
        JTextField dssFieldE = new JTextField();
        dssFieldE.setText("*");
        dssFieldE.setEditable(false);
        dssFieldF = new JTextField();

        dssPartsConstraints.gridy = 0;
        dssPartsConstraints.gridx = 0;
        dssPartsConstraints.weightx = 1;
        JPanel partAPanel = new JPanel();
        partAPanel.setLayout(new BoxLayout(partAPanel, BoxLayout.X_AXIS));
        partAPanel.add(Box.createRigidArea(new Dimension(4,0)));
        partAPanel.add(new JLabel(TextProperties.getInstance().getProperty("NormalizerWiz_PartA_L")));
        partAPanel.add(dssFieldA);
        dssPartsPanel.add(partAPanel, dssPartsConstraints);

        dssPartsConstraints.gridy = 0;
        dssPartsConstraints.gridx = 1;
        dssPartsConstraints.weightx = 1;
        JPanel partBPanel = new JPanel();
        partBPanel.setLayout(new BoxLayout(partBPanel, BoxLayout.X_AXIS));
        partBPanel.add(Box.createRigidArea(new Dimension(4,0)));
        partBPanel.add(new JLabel(TextProperties.getInstance().getProperty("NormalizerWiz_PartB_L")));
        partBPanel.add(dssFieldB);
        dssPartsPanel.add(partBPanel, dssPartsConstraints);

        dssPartsConstraints.gridy = 0;
        dssPartsConstraints.gridx = 2;
        dssPartsConstraints.weightx = 1;
        JPanel partCPanel = new JPanel();
        partCPanel.setLayout(new BoxLayout(partCPanel, BoxLayout.X_AXIS));
        partCPanel.add(Box.createRigidArea(new Dimension(4,0)));
        partCPanel.add(new JLabel(TextProperties.getInstance().getProperty("NormalizerWiz_PartC_L")));
        partCPanel.add(dssFieldC);
        dssPartsPanel.add(partCPanel, dssPartsConstraints);

        dssPartsConstraints.gridy = 1;
        dssPartsConstraints.gridx = 0;
        dssPartsConstraints.weightx = 1;
        dssPartsConstraints.insets = new Insets(10,0,0,0);
        JPanel partDPanel = new JPanel();
        partDPanel.setLayout(new BoxLayout(partDPanel, BoxLayout.X_AXIS));
        partDPanel.add(Box.createRigidArea(new Dimension(4,0)));
        partDPanel.add(new JLabel(TextProperties.getInstance().getProperty("NormalizerWiz_PartD_L")));
        partDPanel.add(dssFieldD);
        dssPartsPanel.add(partDPanel, dssPartsConstraints);

        dssPartsConstraints.gridy = 1;
        dssPartsConstraints.gridx = 1;
        dssPartsConstraints.weightx = 1;
        JPanel partEPanel = new JPanel();
        partEPanel.setLayout(new BoxLayout(partEPanel, BoxLayout.X_AXIS));
        partEPanel.add(Box.createRigidArea(new Dimension(4,0)));
        partEPanel.add(new JLabel(TextProperties.getInstance().getProperty("NormalizerWiz_PartE_L")));
        partEPanel.add(dssFieldE);
        dssPartsPanel.add(partEPanel, dssPartsConstraints);

        dssPartsConstraints.gridy = 1;
        dssPartsConstraints.gridx = 2;
        dssPartsConstraints.weightx = 1;
        JPanel partFPanel = new JPanel();
        partFPanel.setLayout(new BoxLayout(partFPanel, BoxLayout.X_AXIS));
        partFPanel.add(Box.createRigidArea(new Dimension(4,0)));
        partFPanel.add(new JLabel(TextProperties.getInstance().getProperty("NormalizerWiz_PartF_L")));
        partFPanel.add(Box.createRigidArea(new Dimension(1,0)));
        partFPanel.add(dssFieldF);
        dssPartsPanel.add(partFPanel, dssPartsConstraints);

        dssPartsConstraints.gridy = 2;
        dssPartsConstraints.gridx = 0;
        dssPartsConstraints.weightx = 0;
        dssPartsConstraints.gridwidth = 3;
        dssPartsPanel.add(overridePanel(), dssPartsConstraints);

        return dssPartsPanel;
    }

    private JPanel overridePanel() {
        JPanel overridePanel = new JPanel(new GridBagLayout());

        JCheckBox unitCheckBox = new JCheckBox("Override DSS units");
        overridePanel.add(unitCheckBox, overrideConstraints(0,0));
        overridePanel.add(Box.createHorizontalGlue(), overrideConstraints(0,2));

        JLabel unitsLabel = new JLabel("Units string:");
        unitsLabel.setBorder(BorderFactory.createEmptyBorder(0,50,0,0));
        overridePanel.add(unitsLabel, overrideConstraints(1,0));
        unitsTextField = new JTextField();
        unitsTextField.setColumns(12);
        unitsTextField.setEnabled(false);
        overridePanel.add(unitsTextField, overrideConstraints(1,1));
        overridePanel.add(Box.createHorizontalGlue(), overrideConstraints(1,3));

        JCheckBox typeCheckBox = new JCheckBox("Override DSS data type");
        overridePanel.add(typeCheckBox, overrideConstraints(2,0));
        overridePanel.add(Box.createHorizontalGlue(), overrideConstraints(2,2));

        JLabel typeLabel = new JLabel("Data type:");
        typeLabel.setBorder(BorderFactory.createEmptyBorder(0,50,0,0));
        overridePanel.add(typeLabel, overrideConstraints(3,0));
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        model.addAll(Arrays.asList("INST-VAL", "PER-CUM", "PER-AVER", "INST-CUM"));
        dataTypeCombo = new JComboBox<>(model);
        dataTypeCombo.setEnabled(false);
        overridePanel.add(dataTypeCombo, overrideConstraints(3,1));
        overridePanel.add(Box.createHorizontalGlue(), overrideConstraints(3,3));

        unitCheckBox.addActionListener(evt -> {
            unitsTextField.setText("");
            unitsTextField.setEnabled(unitCheckBox.isSelected());
        });

        typeCheckBox.addActionListener(evt -> {
            dataTypeCombo.setSelectedIndex(-1);
            dataTypeCombo.setEnabled(typeCheckBox.isSelected());
        });

        return overridePanel;
    }

    private GridBagConstraints overrideConstraints(int y, int x) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridy = y;
        constraints.gridx = x;
        constraints.weightx = (x == 3) ? 1 : 0;

        if(y == 0 || y == 2)
            constraints.insets = new Insets(10,0,0,0);

        return constraints;
    }

    private void selectDestinationBrowseAction(FileBrowseButton fileBrowseButton) {
        JFileChooser fileChooser = new JFileChooser(fileBrowseButton.getPersistedBrowseLocation());

        // Configuring fileChooser dialog
        fileChooser.setAcceptAllFileFilterUsed(true);
        FileNameExtensionFilter acceptableExtension = new FileNameExtensionFilter("DSS Files (*.dss)", "dss");
        fileChooser.setFileFilter(acceptableExtension);

        // Pop up fileChooser dialog
        int userChoice = fileChooser.showOpenDialog(parent);

        // Deal with user's choice
        if(userChoice == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            boolean override = FileSaveUtil.promptFileOverride(SwingUtilities.getWindowAncestor(this), selectedFile.toPath());
            if(!override) return;
            String selectedPath = selectedFile.getAbsolutePath();
            if(!selectedFile.getName().contains(".")) { selectedPath = selectedPath + ".dss"; }
            selectDestinationTextField.setText(selectedPath);
            File finalFile = new File(selectedPath);
            fileBrowseButton.setPersistedBrowseLocation(finalFile);
        }
    }

    /* Getters */
    public JTextField getFieldA() {
        return dssFieldA;
    }

    public JTextField getFieldB() {
        return dssFieldB;
    }

    public JTextField getFieldC() {
        return dssFieldC;
    }

    @SuppressWarnings("unused")
    public JTextField getFieldD() {
        return dssFieldD;
    }

    @SuppressWarnings("unused")
    public JTextField getFieldE() {
        return dssFieldE;
    }

    public JTextField getFieldF() {
        return dssFieldF;
    }

    public JTextField getDestinationTextField() {
        return selectDestinationTextField;
    }

    public String getUnitsString() {
        return unitsTextField.getText();
    }

    public String getDataType() {
        Object dataType = dataTypeCombo.getSelectedItem();
        return (dataType != null) ? dataType.toString() : "";
    }
}
