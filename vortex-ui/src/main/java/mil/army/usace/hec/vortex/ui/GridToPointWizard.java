package mil.army.usace.hec.vortex.ui;

import mil.army.usace.hec.vortex.convert.GridToPointConverter;
import mil.army.usace.hec.vortex.geo.VectorUtils;
import mil.army.usace.hec.vortex.ui.util.FileSaveUtil;
import mil.army.usace.hec.vortex.util.DssUtil;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;

public class GridToPointWizard extends ProcessingWizard {

    private SourceFileSelectionPanel sourceFileSelectionPanel;
    private DestinationSelectionPanel destinationSelectionPanel;

    private JCheckBox averageCheckBox;
    private JCheckBox minCheckBox;
    private JCheckBox maxCheckBox;
    private JCheckBox medianCheckBox;
    private JCheckBox pct25thCheckBox;
    private JCheckBox pct75thCheckBox;
    private JCheckBox pctCellsGreaterZeroCheckBox;
    private JCheckBox pctCellsGreater25thPctBox;

    private JTextField zonesShapefile;
    private JTextField sourceFileTextField;
    private JList<String> chosenSourceGridsList;
    private JComboBox<String> fieldComboBox;

    public GridToPointWizard(Frame frame) {
        super(frame);
    }

    @Override
    protected String getTitlePropertyKey() {
        return "GridToPointWiz_Title";
    }

    @Override
    protected List<JPanel> createStepPanels() {
        return List.of(
                stepOnePanel(),
                stepTwoPanel(),
                stepThreePanel(),
                stepFourPanel(),
                createProgressPanel(),
                createProgressPanel()
        );
    }

    @Override
    protected int getLastInteractiveStep() {
        return 3;
    }

    @Override
    protected boolean validateStep(int stepIndex) {
        return switch (stepIndex) {
            case 0 -> validateStepOne();
            case 1 -> validateStepTwo();
            case 2 -> validateStepThree();
            case 3 -> validateStepFour();
            default -> true;
        };
    }

    @Override
    protected void submitStep(int stepIndex) {
        if (stepIndex == 3) {
            SwingWorker<Void, Void> task = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    gridToPointTask();
                    return null;
                }

                @Override
                protected void done() {
                    nextAction();
                }
            };
            task.execute();
        }
    }

    @Override
    protected void clearWizardState() {
        sourceFileSelectionPanel.clear();
        zonesShapefile.setText("");

        averageCheckBox.setSelected(true);
        minCheckBox.setSelected(false);
        maxCheckBox.setSelected(false);
        medianCheckBox.setSelected(false);
        pct25thCheckBox.setSelected(false);
        pct75thCheckBox.setSelected(false);
        pctCellsGreaterZeroCheckBox.setSelected(false);
        pctCellsGreater25thPctBox.setSelected(false);

        destinationSelectionPanel.getDestinationTextField().setText("");
        destinationSelectionPanel.getFieldA().setText("");
        destinationSelectionPanel.getFieldF().setText("");
    }

    @Override
    protected void showSaveResult() {
        String savedFile = destinationSelectionPanel.getDestinationTextField().getText();
        FileSaveUtil.showFileLocation(this, Path.of(savedFile));
    }

    // --- Step Panels ---

    private JPanel stepOnePanel() {
        sourceFileSelectionPanel = new SourceFileSelectionPanel(GridToPointWizard.class.getName());
        sourceFileTextField = sourceFileSelectionPanel.getSourceFileTextField();
        chosenSourceGridsList = sourceFileSelectionPanel.getChosenSourceGridsList();
        return sourceFileSelectionPanel;
    }

    private boolean validateStepOne() {
        return sourceFileSelectionPanel.validateInput();
    }

    private JPanel stepTwoPanel() {
        GridBagLayout gridBagLayout = new GridBagLayout();
        gridBagLayout.columnWidths = new int[]{this.getPreferredSize().width, 0};
        gridBagLayout.rowHeights = new int[]{50, 100, 50, 50, 0};
        gridBagLayout.columnWeights = new double[]{1.0, Double.MIN_VALUE};
        gridBagLayout.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};

        JPanel stepTwoPanel = new JPanel(gridBagLayout);
        stepTwoPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,7));

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(0, 0, 5, 0);
        gridBagConstraints.gridx = 0;

        gridBagConstraints.gridy = 0;
        stepTwoPanel.add(dataSourceSectionPanel(), gridBagConstraints);

        return stepTwoPanel;
    }

    private JPanel dataSourceSectionPanel() {
        JLabel dataSourceLabel = new JLabel(TextProperties.getInstance().getProperty("GridToPointWizZonesShapefileL"));
        JPanel dataSourceLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        dataSourceLabelPanel.add(dataSourceLabel);

        JPanel dataSourceTextFieldPanel = new JPanel();
        dataSourceTextFieldPanel.setLayout(new BoxLayout(dataSourceTextFieldPanel, BoxLayout.X_AXIS));
        dataSourceTextFieldPanel.add(Box.createRigidArea(new Dimension(4,0)));

        zonesShapefile = new JTextField();
        dataSourceTextFieldPanel.add(zonesShapefile);

        dataSourceTextFieldPanel.add(Box.createRigidArea(new Dimension(8,0)));

        FileBrowseButton dataSourceBrowseButton = new FileBrowseButton(this.getClass().getName(), "");
        dataSourceBrowseButton.setIcon(IconResources.loadIcon("images/Open16.gif"));
        dataSourceBrowseButton.setPreferredSize(new Dimension(22,22));
        dataSourceBrowseButton.addActionListener(evt -> dataSourceBrowseAction(dataSourceBrowseButton));
        dataSourceTextFieldPanel.add(dataSourceBrowseButton);

        JLabel fieldLabel = new JLabel(TextProperties.getInstance().getProperty("GridToPointWizFieldL"));
        JPanel fieldLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        fieldLabelPanel.add(fieldLabel);

        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        fieldComboBox = new JComboBox<>(model);
        fieldComboBox.setPreferredSize(new Dimension(150,22));
        fieldComboBox.setMaximumRowCount(7);
        fieldComboBox.setEditable(false);

        zonesShapefile.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { shapefilePath(); }
            public void removeUpdate(DocumentEvent e) { shapefilePath(); }
            public void insertUpdate(DocumentEvent e) { shapefilePath(); }
            public void shapefilePath() {
                Path pathToShp = Paths.get(zonesShapefile.getText());
                if (Files.exists(pathToShp) && Files.isRegularFile(pathToShp)) {
                    Set<String> fields = VectorUtils.getFields(pathToShp);
                    model.addAll(fields);
                    fieldComboBox.setSelectedIndex(0);

                    for (String field : fields) {
                        if (field.equalsIgnoreCase("name")) {
                            fieldComboBox.setSelectedItem(field);
                            break;
                        }
                    }
                } else {
                    fieldComboBox.removeAllItems();
                }
            }
        });

        JPanel fieldComboBoxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        fieldComboBoxPanel.add(fieldComboBox);

        JPanel dataSourceSectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        dataSourceSectionPanel.setLayout(new BoxLayout(dataSourceSectionPanel, BoxLayout.Y_AXIS));
        dataSourceSectionPanel.add(dataSourceLabelPanel);
        dataSourceSectionPanel.add(dataSourceTextFieldPanel);
        dataSourceSectionPanel.add(fieldLabelPanel);
        dataSourceSectionPanel.add(fieldComboBoxPanel);

        return dataSourceSectionPanel;
    }

    private boolean validateStepTwo() {
        Path pathToShp = Path.of(zonesShapefile.getText());
        if (!Files.isRegularFile(pathToShp) || Files.notExists(pathToShp)) {
            JOptionPane.showMessageDialog(this, "Zones shapefile is required.",
                    "Error: Missing Field", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        String fieldSelection = String.valueOf(fieldComboBox.getSelectedItem());
        if (fieldSelection.equals("null") || fieldSelection.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Shapefile field selection is required.",
                    "Error: Missing Field", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        return true;
    }

    private JPanel stepThreePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        JLabel statisticsLabel = new JLabel(TextProperties.getInstance().getProperty("GridToPointWizStatisticsL"));
        statisticsLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

        averageCheckBox = new JCheckBox(TextProperties.getInstance().getProperty("GridToPointWizAverageL"),true);
        minCheckBox = new JCheckBox(TextProperties.getInstance().getProperty("GridToPointWizMinL"),false);
        maxCheckBox = new JCheckBox(TextProperties.getInstance().getProperty("GridToPointWizMaxL"),false);
        medianCheckBox = new JCheckBox(TextProperties.getInstance().getProperty("GridToPointWizMedianL"),false);
        pct25thCheckBox = new JCheckBox(TextProperties.getInstance().getProperty("GridToPointWiz25thPctL"),false);
        pct75thCheckBox = new JCheckBox(TextProperties.getInstance().getProperty("GridToPointWiz75thPctL"),false);
        pctCellsGreaterZeroCheckBox = new JCheckBox(TextProperties.getInstance().getProperty("GridToPointWizPctCellsGreaterZeroL"),false);
        pctCellsGreater25thPctBox = new JCheckBox(TextProperties.getInstance().getProperty("GridToPointWizPctCellsGreater25thPctL"),false);

        Box optionsBox = Box.createVerticalBox();
        optionsBox.add(averageCheckBox);
        optionsBox.add(minCheckBox);
        optionsBox.add(maxCheckBox);
        optionsBox.add(medianCheckBox);
        optionsBox.add(pct25thCheckBox);
        optionsBox.add(pct75thCheckBox);
        optionsBox.add(pctCellsGreaterZeroCheckBox);
        optionsBox.add(pctCellsGreater25thPctBox);

        optionsBox.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));

        panel.add(statisticsLabel, BorderLayout.NORTH);
        panel.add(optionsBox, BorderLayout.CENTER);

        panel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));

        return panel;
    }

    private boolean validateStepThree() {
        boolean isStatisticSelected = averageCheckBox.isSelected() ||
                minCheckBox.isSelected() ||
                maxCheckBox.isSelected() ||
                medianCheckBox.isSelected() ||
                pct25thCheckBox.isSelected() ||
                pct75thCheckBox.isSelected() ||
                pctCellsGreaterZeroCheckBox.isSelected() ||
                pctCellsGreater25thPctBox.isSelected();

        if (!isStatisticSelected) {
            JOptionPane.showMessageDialog(this,
                    TextProperties.getInstance().getProperty("GridToPointWizNoStatisticsSelected"),
                    TextProperties.getInstance().getProperty("GridToPointWizInvalid"),
                    JOptionPane.ERROR_MESSAGE);
            return false;
        } else {
            return true;
        }
    }

    private JPanel stepFourPanel() {
        destinationSelectionPanel = new DestinationSelectionPanel(this);

        JTextField fieldB = destinationSelectionPanel.getFieldB();
        fieldB.setText("*");
        fieldB.setEnabled(false);

        return destinationSelectionPanel;
    }

    private boolean validateStepFour() {
        String destinationFile = destinationSelectionPanel.getDestinationTextField().getText();
        if(destinationFile == null || destinationFile.isEmpty() ) {
            JOptionPane.showMessageDialog(this, "Destination file is required.",
                    "Error: Missing Field", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        return true;
    }

    // --- Processing Task ---

    private void gridToPointTask() {
        String pathToSource = sourceFileTextField.getText();
        List<String> chosenSourceGrids = getItemsInList(chosenSourceGridsList);
        if (chosenSourceGrids == null) return;
        Set<String> sourceGrids = new HashSet<>(chosenSourceGrids);

        String shapefile = zonesShapefile.getText();
        String fieldSelection = String.valueOf(fieldComboBox.getSelectedItem());

        List<String> chosenSourceList = getItemsInList(chosenSourceGridsList);
        if (chosenSourceList == null) return;
        Map<String, Set<String>> pathnameParts = DssUtil.getPathnameParts(chosenSourceList);

        List<String> partAList = new ArrayList<>(pathnameParts.get("aParts"));
        List<String> partCList = new ArrayList<>(pathnameParts.get("cParts"));
        List<String> partFList = new ArrayList<>(pathnameParts.get("fParts"));

        String partA = (partAList.size() == 1) ? partAList.get(0) : "*";
        String partC = (partCList.size() == 1) ? partCList.get(0) : "PRECIPITATION";
        String partF = (partFList.size() == 1) ? partFList.get(0) : "*";

        Map<String, String> writeOptions = new HashMap<>();
        String dssFieldA = destinationSelectionPanel.getFieldA().getText();
        String dssFieldC = destinationSelectionPanel.getFieldC().getText();
        String dssFieldF = destinationSelectionPanel.getFieldF().getText();

        String destination = destinationSelectionPanel.getDestinationTextField().getText();
        if (destination.toLowerCase().endsWith(".dss")) {
            writeOptions.put("partA", (dssFieldA.isEmpty()) ? partA : dssFieldA);
            writeOptions.put("partC", (dssFieldC.isEmpty()) ? partC : dssFieldC);
            writeOptions.put("partF", (dssFieldF.isEmpty()) ? partF : dssFieldF);
        }

        String unitsString = destinationSelectionPanel.getUnitsString();
        if (!unitsString.isEmpty())
            writeOptions.put("units", unitsString);

        String dataType = destinationSelectionPanel.getDataType();
        if (dataType != null && !dataType.isEmpty())
            writeOptions.put("dataType", dataType);

        writeOptions.put("isAccumulate", "true");

        writeOptions.put("Average", String.valueOf(averageCheckBox.isSelected()));
        writeOptions.put("Min", String.valueOf(minCheckBox.isSelected()));
        writeOptions.put("Max", String.valueOf(maxCheckBox.isSelected()));
        writeOptions.put("Median", String.valueOf(medianCheckBox.isSelected()));
        writeOptions.put("1Q", String.valueOf(pct25thCheckBox.isSelected()));
        writeOptions.put("3Q", String.valueOf(pct75thCheckBox.isSelected()));
        writeOptions.put("Pct>0", String.valueOf(pctCellsGreaterZeroCheckBox.isSelected()));
        writeOptions.put("Pct>1Q", String.valueOf(pctCellsGreater25thPctBox.isSelected()));

        GridToPointConverter converter = GridToPointConverter.builder()
                .pathToGrids(pathToSource)
                .variables(sourceGrids)
                .pathToFeatures(shapefile)
                .field(fieldSelection)
                .destination(destination)
                .writeOptions(writeOptions)
                .build();

        converter.addPropertyChangeListener(createProgressListener());

        SwingWorker<Void, Void> task = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                converter.run();
                return null;
            }

            @Override
            protected void done() {
                setButtonsForRestartOrClose();
                progressMessagePanel.setValue(100);
            }
        };

        task.execute();
    }

    // --- Browse Action ---

    private void dataSourceBrowseAction(FileBrowseButton fileBrowseButton) {
        JFileChooser fileChooser = new JFileChooser(fileBrowseButton.getPersistedBrowseLocation());

        fileChooser.setAcceptAllFileFilterUsed(false);
        FileNameExtensionFilter acceptableExtension = new FileNameExtensionFilter("Shapefiles (*.shp)", "shp");
        fileChooser.addChoosableFileFilter(acceptableExtension);

        int userChoice = fileChooser.showOpenDialog(this);

        if(userChoice == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            zonesShapefile.setText(selectedFile.getAbsolutePath());
            fileBrowseButton.setPersistedBrowseLocation(selectedFile);
        }
    }

    /* Add main for quick UI Testing */
    static public void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) { e.printStackTrace(); }

        GridToPointWizard gridToPointWizard = new GridToPointWizard(null);
        gridToPointWizard.buildAndShowUI();
    }
}
