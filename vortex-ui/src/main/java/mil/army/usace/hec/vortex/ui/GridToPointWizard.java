package mil.army.usace.hec.vortex.ui;

import mil.army.usace.hec.vortex.convert.GridToPointConverter;
import mil.army.usace.hec.vortex.geo.VectorUtils;
import mil.army.usace.hec.vortex.io.DataReader;
import mil.army.usace.hec.vortex.util.DssUtil;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GridToPointWizard extends JFrame {
    private final Frame frame;
    private DestinationSelectionPanel destinationSelectionPanel;

    private Container contentCards;
    private CardLayout cardLayout;
    private JButton backButton, nextButton, cancelButton;
    private int cardNumber;

    private JTextField zonesShapefile;
    private JTextField sourceFileTextField;
    private JList<String> availableSourceGridsList, chosenSourceGridsList;
    private JComboBox<String> field;
    private String fieldValue;
    private JProgressBar progressBar;

    private static final Logger logger = Logger.getLogger(GridToPointWizard.class.getName());

    public GridToPointWizard(Frame frame) {
        this.frame = frame;

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    public void buildAndShowUI() {
        /* Setting Wizard's names and layout */
        this.setTitle(TextProperties.getInstance().getProperty("GridToPointWiz_Title"));
        this.setIconImage(IconResources.loadImage("images/vortex_black.png"));
        this.setSize(600, 400);
        this.setLayout(new BorderLayout());

        /* Initializing Card Container */
        initializeContentCards();

        /* Initializing Button Panel (Back, Next, Cancel) */
        initializeButtonPanel();

        /* Add contentCards to wizard, and then show wizard */
        this.add(contentCards, BorderLayout.CENTER);
        this.setLocationRelativeTo(frame);
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
        cancelButton.addActionListener(evt -> {
            this.setVisible(false);
            this.dispose();
        });

        /* Adding Buttons to NavigationPanel */
        buttonPanel.add(backButton);
        buttonPanel.add(nextButton);
        buttonPanel.add(cancelButton);

        /* Add buttonPanel to GridToPointWizard */
        this.add(buttonPanel, BorderLayout.SOUTH);
    }

    private void nextAction() {
        if(!validateCurrentStep()) return;
        submitCurrentStep();
        cardNumber++;
        backButton.setEnabled(true);

        if(cardNumber == 3) {
            backButton.setEnabled(false);
            nextButton.setEnabled(false);
        } // If: Step Four (Processing...) Then disable Back and Next button

        if(cardNumber == 4) {
            backButton.setVisible(false);
            nextButton.setText(TextProperties.getInstance().getProperty("GridToPointWiz_Restart"));
            nextButton.setToolTipText(TextProperties.getInstance().getProperty("GridToPointWiz_Restart_TT"));
            nextButton.setEnabled(true);
            cancelButton.setText(TextProperties.getInstance().getProperty("GridToPointWiz_Close"));
            cancelButton.setToolTipText(TextProperties.getInstance().getProperty("GridToPointWiz_Close_TT"));
        } // If: Step Five (Change Cancel to Close)

        cardLayout.next(contentCards);
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
        sourceFileTextField.setText("");
        Objects.requireNonNull(getDefaultListModel(availableSourceGridsList)).clear();
        Objects.requireNonNull(getDefaultListModel(chosenSourceGridsList)).clear();

        /* Clearing Step Two Panel */
        zonesShapefile.setText("");

        /* Clearing Step Three Panel */
        destinationSelectionPanel.getDestinationTextField().setText("");
        destinationSelectionPanel.getFieldA().setText("");
        destinationSelectionPanel.getFieldB().setText("");
        destinationSelectionPanel.getFieldF().setText("");

        /* Clearing Step Four Panel */
        progressBar.setIndeterminate(true);
        progressBar.setStringPainted(false);
        progressBar.setValue(0);
        progressBar.setString("0%");
    }

    private boolean validateCurrentStep() {
        switch(cardNumber) {
            case 0: return validateStepOne();
            case 1: return validateStepTwo();
            case 2: return validateStepThree();
            case 3: return validateStepFour();
            default: return unknownStepError();
        }
    }

    private void submitCurrentStep() {
        switch(cardNumber) {
            case 0: submitStepOne(); break;
            case 1: submitStepTwo(); break;
            case 2: submitStepThree(); break;
            case 3: submitStepFour(); break;
            default: unknownStepError(); break;
        }
    }

    private boolean unknownStepError() {
        logger.log(Level.SEVERE, "Unknown Step in Wizard");
        return false;
    }

    private JPanel stepOnePanel() {
        /* selectSourceFilePanel and selectSourceGridsPanel*/
        JPanel selectSourceFilePanel = stepOneSourceFilePanel();
        JPanel selectSourceGridsPanel = stepOneSourceGridsPanel();

        /* Setting GridBagLayout for stepOnePanel */
        GridBagLayout gridBagLayout = new GridBagLayout();
        gridBagLayout.columnWidths = new int[]{this.getPreferredSize().width, 0};
        gridBagLayout.rowHeights = new int[]{50, 100};
        gridBagLayout.columnWeights = new double[]{1.0, Double.MIN_VALUE};
        gridBagLayout.rowWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};

        /* Adding Panels to stepOnePanel */
        JPanel stepOnePanel = new JPanel(gridBagLayout);
        stepOnePanel.setBorder(BorderFactory.createEmptyBorder(5,9,5,8));

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(0, 0, 5, 0);
        gridBagConstraints.gridx = 0;

        gridBagConstraints.gridy = 0;
        stepOnePanel.add(selectSourceFilePanel, gridBagConstraints);

        gridBagConstraints.gridy = 1;
        gridBagConstraints.weighty = 1;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        stepOnePanel.add(selectSourceGridsPanel, gridBagConstraints);

        return stepOnePanel;
    }

    private boolean validateStepOne() {
        /* Popup Alert of Missing Inputs */
        if(sourceFileTextField.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Input dataset is required.",
                    "Error: Missing Field", JOptionPane.ERROR_MESSAGE);

            return false;
        }

        /* Popup Alert of No Variables Selected */
        DefaultListModel<String> chosenGridsModel = getDefaultListModel(chosenSourceGridsList);
        if(chosenGridsModel == null || chosenGridsModel.isEmpty()) {
            JOptionPane.showMessageDialog(this, "At least one variable must be selected.",
                    "Error: No Variables Selected", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        return true;
    }

    private void submitStepOne() {}

    private JPanel stepOneSourceFilePanel() {
        /* selectSourceFileLabel */
        JLabel selectSourceFileLabel = new JLabel(TextProperties.getInstance().getProperty("GridToPointWiz_SelectSourceFile_L"));
        selectSourceFileLabel.setBorder(BorderFactory.createEmptyBorder(0,0,5,0));

        /* TextField and Browse Panel */
        JPanel sourceFileTextFieldPanel = new JPanel();
        sourceFileTextFieldPanel.setLayout(new BoxLayout(sourceFileTextFieldPanel, BoxLayout.X_AXIS));

        sourceFileTextField = new JTextField();
        sourceFileTextField.setColumns(0);
        sourceFileTextFieldPanel.add(sourceFileTextField);

        sourceFileTextFieldPanel.add(Box.createRigidArea(new Dimension(8,0)));

        FileBrowseButton sourceFileBrowseButton = new FileBrowseButton(this.getClass().getName(), "");
        sourceFileBrowseButton.setIcon(IconResources.loadIcon("images/Open16.gif"));
        sourceFileBrowseButton.setPreferredSize(new Dimension(22,22));
        sourceFileBrowseButton.addActionListener(evt -> sourceFileBrowseAction(sourceFileBrowseButton));
        sourceFileTextFieldPanel.add(sourceFileBrowseButton);

        /* Adding everything together */
        JPanel sourceFilePanel = new JPanel(new BorderLayout());
        sourceFilePanel.add(selectSourceFileLabel, BorderLayout.NORTH);
        sourceFilePanel.add(sourceFileTextFieldPanel, BorderLayout.CENTER);

        return sourceFilePanel;
    }

    private JPanel stepOneSourceGridsPanel() {
        /* selectSourceGridsLabel */
        JLabel selectSourceGridsLabel = new JLabel(TextProperties.getInstance().getProperty("GridToPointWiz_SelectSourceGrids_L"));
        selectSourceGridsLabel.setBorder(BorderFactory.createEmptyBorder(0,0,5,0));

        /* Available Source Grids List */
        DefaultListModel<String> availableGridsListModel = new DefaultListModel<>();
        availableSourceGridsList = new JList<>(availableGridsListModel);
        availableSourceGridsList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(e.getClickCount() == 2) { stepOneAddSelectedVariables(); }
            }
        });
        JScrollPane leftScrollPanel = new JScrollPane();
        leftScrollPanel.setViewportView(availableSourceGridsList);

        /* Chosen Source Grids List */
        DefaultListModel<String> chosenGridsListModel = new DefaultListModel<>();
        chosenSourceGridsList = new JList<>(chosenGridsListModel);
        chosenSourceGridsList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(e.getClickCount() == 2) { stepOneRemoveSelectedVariables(); }
            }
        });
        JScrollPane rightScrollPanel = new JScrollPane();
        rightScrollPanel.setViewportView(chosenSourceGridsList);

        /* Transfer Buttons Panel */
        JPanel transferButtonsPanel = new JPanel();
        transferButtonsPanel.setLayout(new BoxLayout(transferButtonsPanel, BoxLayout.Y_AXIS));
        JButton addVariableButton = new JButton(IconResources.loadIcon("images/right-arrow-24.png"));
        addVariableButton.setPreferredSize(new Dimension(22,22));
        addVariableButton.setMaximumSize(new Dimension(22,22));
        addVariableButton.addActionListener(evt -> stepOneAddSelectedVariables());
        JButton removeVariableButton = new JButton(IconResources.loadIcon("images/left-arrow-24.png"));
        removeVariableButton.setPreferredSize(new Dimension(22,22));
        removeVariableButton.setMaximumSize(new Dimension(22,22));
        removeVariableButton.addActionListener(evt -> stepOneRemoveSelectedVariables());
        transferButtonsPanel.add(addVariableButton);
        transferButtonsPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        transferButtonsPanel.add(removeVariableButton);

        /* Adding grid lists and transfer buttons to sourceGridsSelectionPanel */
        JPanel sourceGridsSelectionPanel = new JPanel();
        sourceGridsSelectionPanel.setLayout(new BoxLayout(sourceGridsSelectionPanel, BoxLayout.X_AXIS));
        sourceGridsSelectionPanel.add(leftScrollPanel);
        sourceGridsSelectionPanel.add(Box.createRigidArea(new Dimension(4,0)));
        sourceGridsSelectionPanel.add(transferButtonsPanel);
        sourceGridsSelectionPanel.add(Box.createRigidArea(new Dimension(4,0)));
        sourceGridsSelectionPanel.add(rightScrollPanel);

        /* Setting Preferred Sizes of Components */
        int mainContentWidth = sourceGridsSelectionPanel.getPreferredSize().width;
        int scrollPanelWidth = mainContentWidth * 45 / 100;
        leftScrollPanel.setPreferredSize(new Dimension(scrollPanelWidth, 0));
        rightScrollPanel.setPreferredSize(new Dimension(scrollPanelWidth, 0));

        /* Adding everything together */
        JPanel sourceGridsPanel = new JPanel(new BorderLayout());
        sourceGridsPanel.add(selectSourceGridsLabel, BorderLayout.NORTH);
        sourceGridsPanel.add(sourceGridsSelectionPanel, BorderLayout.CENTER);

        return sourceGridsPanel;
    }

    private List<String> sortDssVariables(List<String> dssVariables) {
        if(dssVariables == null || dssVariables.isEmpty()) { return null; }

        DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern("ddMMMuuuu:HHmm")
                .toFormatter();

        /* Sort based on D part */
        dssVariables.sort(Comparator.comparing(s -> LocalDateTime.parse(s.split("/")[4], formatter)));

        return dssVariables;
    }

    private void stepOneAddSelectedVariables() {
        List<String> selectedVariables = availableSourceGridsList.getSelectedValuesList();

        /* Adding to Right Variables List */
        DefaultListModel<String> defaultRightModel = getDefaultListModel(chosenSourceGridsList);
        if(defaultRightModel == null) { return; }
        List<String> rightVariablesList = Collections.list(defaultRightModel.elements());
        rightVariablesList.addAll(selectedVariables);
        defaultRightModel.clear();
        rightVariablesList = sortDssVariables(rightVariablesList);
        defaultRightModel.addAll(rightVariablesList);

        /* Removing from Left Variables List */
        DefaultListModel<String> defaultLeftModel = getDefaultListModel(availableSourceGridsList);
        if(defaultLeftModel == null) { return; }
        selectedVariables.forEach(defaultLeftModel::removeElement);
    }

    private void stepOneRemoveSelectedVariables() {
        List<String> selectedVariables = chosenSourceGridsList.getSelectedValuesList();

        /* Adding to Left Variables List */
        DefaultListModel<String> defaultLeftModel = getDefaultListModel(availableSourceGridsList);
        if(defaultLeftModel == null) { return; }
        List<String> leftVariablesList = Collections.list(defaultLeftModel.elements());
        leftVariablesList.addAll(selectedVariables);
        defaultLeftModel.clear();
        leftVariablesList = sortDssVariables(leftVariablesList);
        defaultLeftModel.addAll(leftVariablesList);

        /* Removing from Left Variables List */
        DefaultListModel<String> defaultRightModel = getDefaultListModel(chosenSourceGridsList);
        if(defaultRightModel == null) { return; }
        selectedVariables.forEach(defaultRightModel::removeElement);
    }

    private void sourceFileBrowseAction(FileBrowseButton fileBrowseButton) {
        JFileChooser fileChooser = new JFileChooser(fileBrowseButton.getPersistedBrowseLocation());
        fileChooser.setMultiSelectionEnabled(true);

        // Configuring fileChooser dialog
        fileChooser.setAcceptAllFileFilterUsed(false);
        FileNameExtensionFilter acceptableExtension = new FileNameExtensionFilter("All recognized files",
                "nc", "hdf", "grib", "gb2", "grb2", "grib2", "grb", "asc", "dss");
        fileChooser.addChoosableFileFilter(acceptableExtension);

        // Pop up fileChooser dialog
        int userChoice = fileChooser.showOpenDialog(this);

        // Deal with user's choice
        if(userChoice == JFileChooser.APPROVE_OPTION) {
            /* Set path to text field and save browse location */
            File selectedFile = fileChooser.getSelectedFile();
            sourceFileTextField.setText(selectedFile.getAbsolutePath());
            fileBrowseButton.setPersistedBrowseLocation(selectedFile);

            /* Populate variables for available source grids list */
            Set<String> variables = DataReader.getVariables(selectedFile.toString());
            DefaultListModel<String> defaultListModel = getDefaultListModel(availableSourceGridsList);

            if(defaultListModel != null) {
                defaultListModel.clear();
                List<String> sortedDssVariables = sortDssVariables(new ArrayList<>(variables));
                defaultListModel.addAll(sortedDssVariables);
            }
        } // If: User selected OK -> Populate Available Source Grids
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
        zonesShapefile.setColumns(0);
        zonesShapefile.setBorder(null);
        JScrollPane layerPanel = new JScrollPane(zonesShapefile);
        dataSourceTextFieldPanel.add(layerPanel);

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
        field = new JComboBox<>(model);
        field.setPreferredSize(new Dimension(150,22));
        field.setMaximumRowCount(7);
        field.setEditable(false);

        zonesShapefile.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { shapefilePath(); }
            public void removeUpdate(DocumentEvent e) { shapefilePath(); }
            public void insertUpdate(DocumentEvent e) { shapefilePath(); }
            public void shapefilePath() {
                Path pathToShp = Paths.get(zonesShapefile.getText());
                if (Files.exists(pathToShp) && !pathToShp.toString().isEmpty()) {
                    Set<String> fields = VectorUtils.getFields(pathToShp);
                    model.addAll(fields);
                    field.setSelectedIndex(0);
                } else {
                    field.removeAllItems();
                }
            }
        });

        field.addItemListener(e -> {
            if(e.getStateChange() == ItemEvent.SELECTED) {
                Path pathToShp = Paths.get(zonesShapefile.getText());
                fieldValue = String.valueOf(VectorUtils.getValues(pathToShp, Objects.requireNonNull(field.getSelectedItem()).toString()));
                fieldValue = fieldValue.substring(1, fieldValue.length() - 1);
            }
        });

        JPanel fieldComboBoxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        fieldComboBoxPanel.add(field);

        JPanel dataSourceSectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        dataSourceSectionPanel.setLayout(new BoxLayout(dataSourceSectionPanel, BoxLayout.Y_AXIS));
        dataSourceSectionPanel.add(dataSourceLabelPanel);
        dataSourceSectionPanel.add(dataSourceTextFieldPanel);
        dataSourceSectionPanel.add(fieldLabelPanel);
        dataSourceSectionPanel.add(fieldComboBoxPanel);

        return dataSourceSectionPanel;
    }

    private boolean validateStepTwo() { return true; }

    private void submitStepTwo() {}

    private JPanel stepThreePanel() {
        destinationSelectionPanel = new DestinationSelectionPanel(this);
        return destinationSelectionPanel;
    }

    private boolean validateStepThree() {
        String destinationFile = destinationSelectionPanel.getDestinationTextField().getText();
        if(destinationFile == null || destinationFile.isEmpty() ) {
            JOptionPane.showMessageDialog(this, "Destination file is required.",
                    "Error: Missing Field", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        return true;
    }

    private void submitStepThree() {
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
        String destination = destinationSelectionPanel.getDestinationTextField().getText();
        String selectedField = Objects.requireNonNull(field.getSelectedItem()).toString();

        /* Setting parts */
        List<String> chosenSourceList = getItemsInList(chosenSourceGridsList);
        if(chosenSourceList == null) return;
        Map<String, Set<String>> pathnameParts = DssUtil.getPathnameParts(chosenSourceList);

        List<String> partAList = new ArrayList<>(pathnameParts.get("aParts"));
        List<String> partCList = new ArrayList<>(pathnameParts.get("cParts"));
        List<String> partFList = new ArrayList<>(pathnameParts.get("fParts"));
        String partA = (partAList.size() == 1) ? partAList.get(0) : "*";
        String partC = (partCList.size() == 1) ? partCList.get(0) : "PRECIPITATION";
        String partF = (partFList.size() == 1) ? partFList.get(0) : "*";

        Map<String, String> writeOptions = new HashMap<>();
        String dssFieldA = destinationSelectionPanel.getFieldA().getText();
        String dssFieldB = destinationSelectionPanel.getFieldB().getText();
        String dssFieldC = destinationSelectionPanel.getFieldC().getText();
        String dssFieldF = destinationSelectionPanel.getFieldF().getText();

        if (destination.toLowerCase().endsWith(".dss")) {
            writeOptions.put("partA", (dssFieldA.isEmpty()) ? partA : dssFieldA);
            if (!dssFieldB.isEmpty()) {
                writeOptions.put("partB", dssFieldB);
            } else {
                writeOptions.put("partB", fieldValue);
            }
            writeOptions.put("partC", (dssFieldC.isEmpty()) ? partC : dssFieldC);
            writeOptions.put("partF", (dssFieldF.isEmpty()) ? partF : dssFieldF);
        }

        String unitsString = destinationSelectionPanel.getUnitsString();
        if (!unitsString.isEmpty())
            writeOptions.put("units", unitsString);

        String dataType = destinationSelectionPanel.getDataType();
        if (dataType != null && !dataType.isEmpty())
            writeOptions.put("dataType", dataType);

        GridToPointConverter converter = GridToPointConverter.builder()
                .pathToGrids(pathToSource)
                .variables(sourceGrids)
                .pathToFeatures(shapefile)
                .field(selectedField)
                .destination(destination)
                .writeOptions(writeOptions)
                .build();

        converter.addPropertyChangeListener(evt -> {
            if (evt.getPropertyName().equalsIgnoreCase("progress")) {
                if (!(evt.getNewValue() instanceof Integer)) return;
                int progressValue = (int) evt.getNewValue();
                progressBar.setIndeterminate(false);
                progressBar.setStringPainted(true);
                progressBar.setValue(progressValue);
                progressBar.setString(progressValue + "%");
            }
        });

        converter.convert();
    }

    private JPanel stepFourPanel() {
        JPanel stepFourPanel = new JPanel(new GridBagLayout());

        JPanel insidePanel = new JPanel();
        insidePanel.setLayout(new BoxLayout(insidePanel, BoxLayout.Y_AXIS));

        JLabel processingLabel = new JLabel(TextProperties.getInstance().getProperty("GridToPointWiz_Processing_L"));
        JPanel processingPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        processingPanel.add(processingLabel);
        insidePanel.add(processingPanel);

        progressBar = new JProgressBar(0, 100);
        progressBar.setIndeterminate(true);
        progressBar.setStringPainted(false);
        JPanel progressPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        progressPanel.add(progressBar);
        insidePanel.add(progressPanel);

        stepFourPanel.add(insidePanel);

        return stepFourPanel;
    }

    private boolean validateStepFour() { return true; }

    private void submitStepFour() {}

    private JPanel stepFivePanel() {
        JPanel stepFivePanel = new JPanel(new GridBagLayout());
        JLabel completeLabel = new JLabel(TextProperties.getInstance().getProperty("GridToPointWiz_Complete_L"));
        stepFivePanel.add(completeLabel);
        return stepFivePanel;
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
        DefaultListModel<String> defaultRightModel = getDefaultListModel(list);
        if(defaultRightModel == null) { return null; }
        return Collections.list(defaultRightModel.elements());
    }

    private DefaultListModel<String> getDefaultListModel(JList<String> list) {
        ListModel<String> listModel = list.getModel();
        if(!(listModel instanceof DefaultListModel)) {
            logger.log(Level.SEVERE, list.getName() + " may have not been initialized");
            return null;
        } // If: listModel is not a DefaultListModel -- should not be happening

        return (DefaultListModel<String>) listModel;
    }

    /* Add main for quick UI Testing */
    static public void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) { e.printStackTrace(); }

        GridToPointWizard gridToPointWizard = new GridToPointWizard(null);
        gridToPointWizard.buildAndShowUI();
    }
}