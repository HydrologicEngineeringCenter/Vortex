package mil.army.usace.hec.vortex.ui;

import mil.army.usace.hec.vortex.VortexProperty;
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
import java.util.logging.Level;
import java.util.logging.Logger;

public class GridToPointWizard extends VortexWizard {
    private static final Logger logger = Logger.getLogger(GridToPointWizard.class.getName());

    private final Frame frame;
    private SourceFileSelectionPanel sourceFileSelectionPanel;
    private DestinationSelectionPanel destinationSelectionPanel;

    private Container contentCards;
    private CardLayout cardLayout;
    private JButton backButton, nextButton, cancelButton;
    private int cardNumber;
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

    private final ProgressMessagePanel progressMessagePanel = new ProgressMessagePanel();

    public GridToPointWizard(Frame frame) {
        super();
        this.frame = frame;
        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                closeAction();
            }
        });
    }

    public void buildAndShowUI() {
        /* Setting Wizard's names and layout */
        this.setTitle(TextProperties.getInstance().getProperty("GridToPointWiz_Title"));
        this.setIconImage(IconResources.loadImage("images/vortex_black.png"));
        setMinimumSize(new Dimension(600, 400));
        setLocation(getPersistedLocation());
        if (frame != null) setLocationRelativeTo(frame);
        setSize(getPersistedSize());
        this.setLayout(new BorderLayout());

        /* Initializing Card Container */
        initializeContentCards();

        /* Initializing Button Panel (Back, Next, Cancel) */
        initializeButtonPanel();

        /* Add contentCards to wizard, and then show wizard */
        this.add(contentCards, BorderLayout.CENTER);
        this.setVisible(true);
    }

    private void initializeContentCards() {
        contentCards = new Container();
        cardLayout = new CardLayout();
        contentCards.setLayout(cardLayout);
        cardNumber = 0;

        /* Adding Step Content Panels to contentCards */
        contentCards.add("Step One", stepOnePanel());
        contentCards.add("Step Two", stepTwoPanel());
        contentCards.add("Step Three", stepThreePanel());
        contentCards.add("Step Four", stepFourPanel());
        contentCards.add("Step Five", stepFivePanel());
        contentCards.add("Step Six", stepSixPanel());
    }

    private void initializeButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        /* Back Button */
        backButton = new JButton(TextProperties.getInstance().getProperty("GridToPointWiz_Back"));
        backButton.setToolTipText(TextProperties.getInstance().getProperty("GridToPointWiz_Back_TT"));
        backButton.setEnabled(false);
        backButton.addActionListener(evt -> backAction());

        /* Next Button */
        nextButton = new JButton(TextProperties.getInstance().getProperty("GridToPointWiz_Next"));
        nextButton.setToolTipText(TextProperties.getInstance().getProperty("GridToPointWiz_Next_TT"));
        nextButton.addActionListener(evt -> {
            if(nextButton.getText().equals(TextProperties.getInstance().getProperty("GridToPointWiz_Restart"))) { restartAction(); }
            else if(nextButton.getText().equals(TextProperties.getInstance().getProperty("GridToPointWiz_Next"))) { nextAction(); }
        });

        /* Cancel Button */
        cancelButton = new JButton(TextProperties.getInstance().getProperty("GridToPointWiz_Cancel"));
        cancelButton.setToolTipText(TextProperties.getInstance().getProperty("GridToPointWiz_Cancel_TT"));
        cancelButton.addActionListener(evt -> closeAction());

        /* Adding Buttons to NavigationPanel */
        buttonPanel.add(backButton);
        buttonPanel.add(nextButton);
        buttonPanel.add(cancelButton);

        /* Add buttonPanel to GridToPointWizard */
        this.add(buttonPanel, BorderLayout.SOUTH);
    }

    private void nextAction() {
        if (!validateCurrentStep()) return;
        submitCurrentStep();
        cardNumber++;
        backButton.setEnabled(true);
        updateButtonState();
        cardLayout.next(contentCards);
    }

    private void updateButtonState() {
        backButton.setEnabled(cardNumber > 0 && cardNumber < 4);
        nextButton.setEnabled(cardNumber < 4);
        cancelButton.setEnabled(cardNumber < 4);
    }

    private void setButtonsForRestartOrClose() {
        backButton.setVisible(false);
        nextButton.setText(TextProperties.getInstance().getProperty("GridToPointWiz_Restart"));
        nextButton.setToolTipText(TextProperties.getInstance().getProperty("GridToPointWiz_Restart_TT"));
        nextButton.setEnabled(true);
        cancelButton.setText(TextProperties.getInstance().getProperty("GridToPointWiz_Close"));
        cancelButton.setToolTipText(TextProperties.getInstance().getProperty("GridToPointWiz_Close_TT"));
        cancelButton.setEnabled(true);
    }

    private void backAction() {
        cardNumber--;
        if(cardNumber == 0) {
            backButton.setEnabled(false);
        }
        cardLayout.previous(contentCards);
    }

    private void restartAction() {
        cardNumber = 0;
        cardLayout.first(contentCards);

        /* Resetting Buttons */
        backButton.setVisible(true);
        backButton.setEnabled(false);

        nextButton.setEnabled(true);
        nextButton.setText(TextProperties.getInstance().getProperty("GridToPointWiz_Next"));
        nextButton.setToolTipText(TextProperties.getInstance().getProperty("GridToPointWiz_Next_TT"));

        cancelButton.setText(TextProperties.getInstance().getProperty("GridToPointWiz_Cancel"));
        cancelButton.setToolTipText(TextProperties.getInstance().getProperty("GridToPointWiz_Cancel_TT"));

        /* Clearing Step One Panel */
        sourceFileSelectionPanel.clear();

        /* Clearing Step Two Panel */
        zonesShapefile.setText("");

        // reset the statistics checkboxes
        averageCheckBox.setSelected(true);
        minCheckBox.setSelected(false);
        maxCheckBox.setSelected(false);
        medianCheckBox.setSelected(false);
        pct25thCheckBox.setSelected(false);
        pct75thCheckBox.setSelected(false);
        pctCellsGreaterZeroCheckBox.setSelected(false);
        pctCellsGreater25thPctBox.setSelected(false);

        /* Clearing Step Four Panel */
        destinationSelectionPanel.getDestinationTextField().setText("");
        destinationSelectionPanel.getFieldA().setText("");
        destinationSelectionPanel.getFieldF().setText("");

        /* Clearing Step Five Panel */
        progressMessagePanel.clear();
    }

    private boolean validateCurrentStep() {
        return switch (cardNumber) {
            case 0 -> validateStepOne();
            case 1 -> validateStepTwo();
            case 2 -> validateStepThree();
            case 3 -> validateStepFour();
            case 4 -> validateStepFive();
            default -> unknownStepError();
        };
    }

    private void submitCurrentStep() {
        switch (cardNumber) {
            case 0 -> submitStepOne();
            case 1 -> submitStepTwo();
            case 2 -> submitStepThree();
            case 3 -> submitStepFour();
            case 4 -> submitStepFive();
            default -> unknownStepError();
        }
    }

    private boolean unknownStepError() {
        logger.log(Level.SEVERE, "Unknown Step in Wizard");
        return false;
    }

    private JPanel stepOnePanel() {
        sourceFileSelectionPanel = new SourceFileSelectionPanel(GridToPointWizard.class.getName());
        sourceFileTextField = sourceFileSelectionPanel.getSourceFileTextField();
        chosenSourceGridsList = sourceFileSelectionPanel.getChosenSourceGridsList();
        return sourceFileSelectionPanel;
    }

    private boolean validateStepOne() {
        return sourceFileSelectionPanel.validateInput();
    }

    private void submitStepOne() {}

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

        /* Data Source Section Panel */
        gridBagConstraints.gridy = 0;
        stepTwoPanel.add(dataSourceSectionPanel(), gridBagConstraints);

        return stepTwoPanel;
    }

    private JPanel dataSourceSectionPanel() {
        /* Zones Shapefile section (of stepTwoPanel) */
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

        /* Field section (of stepTwoPanel) */
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

    private void submitStepTwo() {}

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


    private void submitStepThree() {}

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

    private void submitStepFour() {
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

    private void gridToPointTask() {
        String pathToSource = sourceFileTextField.getText();
        List<String> chosenSourceGrids = getItemsInList(chosenSourceGridsList);
        if (chosenSourceGrids == null) return;
        Set<String> sourceGrids = new HashSet<>(chosenSourceGrids);

        /* Shapefile */
        String shapefile = zonesShapefile.getText();
        String fieldSelection = String.valueOf(fieldComboBox.getSelectedItem());

        /* Setting parts */
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

        converter.addPropertyChangeListener(evt -> {
            VortexProperty property = VortexProperty.parse(evt.getPropertyName());
            if (VortexProperty.PROGRESS == property) {
                if (!(evt.getNewValue() instanceof Integer)) return;
                int progressValue = (int) evt.getNewValue();
                progressMessagePanel.setValue(progressValue);
            } else {
                String value = String.valueOf(evt.getNewValue());
                progressMessagePanel.write(value);
            }
        });

        SwingWorker<Void, Void> task = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                converter.run();
                return null;
            }

            @Override
            protected void done() {
                setButtonsForRestartOrClose();
            }
        };

        task.execute();
    }

    private JPanel stepFivePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(progressMessagePanel, BorderLayout.CENTER);
        return panel;
    }

    private boolean validateStepFive() { return true; }

    private void submitStepFive() {}

    private JPanel stepSixPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(progressMessagePanel, BorderLayout.CENTER);
        return panel;
    }

    private void dataSourceBrowseAction(FileBrowseButton fileBrowseButton) {
        JFileChooser fileChooser = new JFileChooser(fileBrowseButton.getPersistedBrowseLocation());

        // Configuring fileChooser dialog
        fileChooser.setAcceptAllFileFilterUsed(false);
        FileNameExtensionFilter acceptableExtension = new FileNameExtensionFilter("Shapefiles (*.shp)", "shp");
        fileChooser.addChoosableFileFilter(acceptableExtension);

        // Pop up fileChooser dialog
        int userChoice = fileChooser.showOpenDialog(this);

        // Deal with user's choice
        if(userChoice == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            zonesShapefile.setText(selectedFile.getAbsolutePath());
            fileBrowseButton.setPersistedBrowseLocation(selectedFile);
        } // If: User selected OK -> Add to 'AddFiles' List
    }

    private List<String> getItemsInList(JList<String> list) {
        DefaultListModel<String> defaultRightModel = Util.getDefaultListModel(list);
        if(defaultRightModel == null) { return null; }
        return Collections.list(defaultRightModel.elements());
    }

    private void closeAction() {
        GridToPointWizard.this.setVisible(false);
        GridToPointWizard.this.dispose();
        String savedFile = destinationSelectionPanel.getDestinationTextField().getText();
        FileSaveUtil.showFileLocation(GridToPointWizard.this, Path.of(savedFile));
    }

    /* Add main for quick UI Testing */
    static public void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) { e.printStackTrace(); }

        GridToPointWizard gridToPointWizard = new GridToPointWizard(null);
        gridToPointWizard.buildAndShowUI();
    }
}