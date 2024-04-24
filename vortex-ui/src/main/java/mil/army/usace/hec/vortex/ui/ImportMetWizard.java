package mil.army.usace.hec.vortex.ui;

import com.formdev.flatlaf.FlatLightLaf;
import mil.army.usace.hec.vortex.VortexProperty;
import mil.army.usace.hec.vortex.io.BatchImporter;
import mil.army.usace.hec.vortex.io.DataReader;
import mil.army.usace.hec.vortex.ui.util.FileSaveUtil;
import mil.army.usace.hec.vortex.util.DssUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ImportMetWizard extends VortexWizard {
    private final Frame frame;
    DestinationSelectionPanel destinationSelectionPanel;

    private Container contentCards;
    private CardLayout cardLayout;
    private JButton backButton, nextButton, cancelButton;
    private JLabel processingLabel;
    private JProgressBar progressBar;
    private int cardNumber;

    private JList<String> addFilesList, leftVariablesList, rightVariablesList;
    private JTextField dataSourceTextField, targetCellSizeTextField;
    private JTextArea targetWktTextArea;
    private ResamplingMethodSelectionPanel resamplingPanel;
    private JLabel importStatusMessageLabel;

    private static final Logger logger = Logger.getLogger(ImportMetWizard.class.getName());

    public ImportMetWizard (Frame frame) {
        super();
        this.frame = frame;
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                closeAction();
            }
        });
    }

    public void buildAndShowUI() {

        /* Setting Wizard's names and layout */
        this.setTitle(TextProperties.getInstance().getProperty("ImportMetWizNameTitle"));
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
        backButton = new JButton(TextProperties.getInstance().getProperty("ImportMetWizBack"));
        backButton.setToolTipText(TextProperties.getInstance().getProperty("ImportMetWizBackTT"));
        backButton.setEnabled(false);
        backButton.addActionListener(evt -> backAction());

        /* Next Button */
        nextButton = new JButton(TextProperties.getInstance().getProperty("ImportMetWizNext"));
        nextButton.setToolTipText(TextProperties.getInstance().getProperty("ImportMetWizNextTT"));
        nextButton.addActionListener(evt -> {
            if(nextButton.getText().equals(TextProperties.getInstance().getProperty("ImportMetWizRestart"))) { restartWizard(); }
            else if(nextButton.getText().equals(TextProperties.getInstance().getProperty("ImportMetWizNext"))) { nextAction(); }
        });

        /* Cancel Button */
        cancelButton = new JButton(TextProperties.getInstance().getProperty("ImportMetWizCancel"));
        cancelButton.setToolTipText(TextProperties.getInstance().getProperty("ImportMetWizCancelTT"));
        cancelButton.addActionListener(evt -> closeAction());

        /* Adding Buttons to NavigationPanel */
        buttonPanel.add(backButton);
        buttonPanel.add(nextButton);
        buttonPanel.add(cancelButton);

        /* Add buttonPanel to ImportMetWizard */
        this.add(buttonPanel, BorderLayout.SOUTH);
    }

    private void nextAction() {
        if(!validateCurrentStep()) return;
        submitCurrentStep();
        cardNumber++;
        backButton.setEnabled(true);

        if(cardNumber == 4) {
            backButton.setEnabled(false);
            nextButton.setEnabled(false);
        } // If: Step Five (Processing...) Then disable Back and Next button

        if(cardNumber == 5) {
            backButton.setVisible(false);
            nextButton.setText(TextProperties.getInstance().getProperty("ImportMetWizRestart"));
            nextButton.setToolTipText(TextProperties.getInstance().getProperty("ImportMetWizRestartTT"));
            nextButton.setEnabled(true);
            cancelButton.setText(TextProperties.getInstance().getProperty("ImportMetWizClose"));
            cancelButton.setToolTipText(TextProperties.getInstance().getProperty("ImportMetWizCloseTT"));
        } // If: Step Six (Change Cancel to Close)

        cardLayout.next(contentCards);
    }

    private void backAction() {
        cardNumber--;
        if(cardNumber == 0) {
            backButton.setEnabled(false);
            DefaultListModel<String> leftVariablesModel = getDefaultListModel(leftVariablesList);
            if(leftVariablesModel != null) { leftVariablesModel.clear(); }
            DefaultListModel<String> rightVariablesModel = getDefaultListModel(rightVariablesList);
            if(rightVariablesModel != null) { rightVariablesModel.clear(); }
        }
        cardLayout.previous(contentCards);
    }

    private void restartWizard() {
        cardNumber = 0;
        cardLayout.first(contentCards);

        /* Resetting Buttons */
        backButton.setVisible(true);
        backButton.setEnabled(false);

        nextButton.setEnabled(true);
        nextButton.setText(TextProperties.getInstance().getProperty("ImportMetWizNext"));
        nextButton.setToolTipText(TextProperties.getInstance().getProperty("ImportMetWizNextTT"));

        cancelButton.setText(TextProperties.getInstance().getProperty("ImportMetWizCancel"));
        cancelButton.setToolTipText(TextProperties.getInstance().getProperty("ImportMetWizCancelTT"));

        /* Clearing Step One Panel */
        Objects.requireNonNullElse(getDefaultListModel(addFilesList), new DefaultListModel<>()).clear();

        /* Clearing Step Two Panel */
        Objects.requireNonNullElse(getDefaultListModel(leftVariablesList), new DefaultListModel<>()).clear();
        Objects.requireNonNullElse(getDefaultListModel(rightVariablesList), new DefaultListModel<>()).clear();

        /* Clearing Step Three Panel */
        dataSourceTextField.setText("");
        targetWktTextArea.setText("");
        targetCellSizeTextField.setText("");
        resamplingPanel.refresh();

        /* Clearing Step Four Panel */
        destinationSelectionPanel.getDestinationTextField().setText("");
        destinationSelectionPanel.getFieldA().setText("");
        destinationSelectionPanel.getFieldB().setText("");
        destinationSelectionPanel.getFieldF().setText("");

        /* Clearing Step Five Panel */
        processingLabel.setText(TextProperties.getInstance().getProperty("ImportMetWizProcessing_L"));
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
            case 4: return validateStepFive();
            default: return unknownStepError();
        }
    }

    private void submitCurrentStep() {
        switch(cardNumber) {
            case 0: submitStepOne(); break;
            case 1: submitStepTwo(); break;
            case 2: submitStepThree(); break;
            case 3: submitStepFour(); break;
            case 4: submitStepFive(); break;
            default: unknownStepError(); break;
        }
    }

    private boolean validateStepOne() {
        /* Invalid if addFilesList is empty */
        DefaultListModel<String> addFilesListModel = getDefaultListModel(addFilesList);
        if(addFilesListModel == null) { return false; }

        if(Collections.list(addFilesListModel.elements()).isEmpty()) {
            /* Popup Alert of Missing Inputs */
            JOptionPane.showMessageDialog(this, "Input dataset is required.",
                    "Error: Missing Field", JOptionPane.ERROR_MESSAGE);
            return false;
        } // If: addFilesList is empty

        return true;
    }

    private boolean validateStepTwo() {
        DefaultListModel<String> defaultListModel = getDefaultListModel(rightVariablesList);
        if(defaultListModel == null) { return false; }

        if(defaultListModel.isEmpty()) {
            /* Popup Alert of No Variables Selected */
            JOptionPane.showMessageDialog(this, "At least one variable must be selected.",
                    "Error: No Variables Selected", JOptionPane.ERROR_MESSAGE);
            return false;
        } // If: No Variables Selected

        return true;
    }

    private boolean validateStepThree() { return true; }

    private boolean validateStepFour() {
        String destinationPath = destinationSelectionPanel.getDestinationTextField().getText();

        if(destinationPath.isEmpty()) {
            /* Popup Alert of Missing Inputs */
            JOptionPane.showMessageDialog(this, "Destination file is required.",
                    "Error: Missing Field", JOptionPane.ERROR_MESSAGE);
            return false;
        } // If: Missing Destination File

        if(!destinationPath.matches(".*(dss|nc[34]?)")) {
            JOptionPane.showMessageDialog(this, "Invalid file extension.",
                    "Error: Unsupported File Type", JOptionPane.ERROR_MESSAGE);
            return false;
        } // If: Path doesn't end with .dss or .nc

        if(destinationPath.endsWith(".dss")) {
            /* Checks if that DSS file exists */
            File dssFile = new File(destinationPath);
            if(!dssFile.exists()) {
                try {
                    boolean success = dssFile.createNewFile();
                    if(success) { logger.log(Level.INFO, "Created DSS File: " + dssFile); }
                } catch (IOException e) { e.printStackTrace(); }
            } // If: DSS file does not exist, make a new one
        } // If: Path does end with .dss

        return true;
    }

    private boolean validateStepFive() { return true; }

    private void submitStepOne() {
        /* Wait Cursor */
        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        /* Goal: Get Available Variables to put into Step Two's Variable List */
        DefaultListModel<String> defaultListModel = getDefaultListModel(addFilesList);
        if(defaultListModel == null) { return; }
        List<String> fileList = Collections.list(defaultListModel.elements());

        /* Getting Available Variables using Vortex API's DataReader */
        /* Sorting Variables by Parts if Files were DSS files */
        /* MUST be a LinkedHashSet to preserve order */
        Set<String> variables = fileList.stream()
                .map(DataReader::getVariables)
                .flatMap(Collection::stream)
                .sorted()
                .collect(Collectors.toCollection(LinkedHashSet::new));

        sortDssVariables(variables);

        /* Putting Available Variables to Step Two's Variable List */
        DefaultListModel<String> variableDefaultListModel = getDefaultListModel(leftVariablesList);
        if(variableDefaultListModel == null) { return; }
        variableDefaultListModel.addAll(variables);

        /* Normal Cursor */
        this.setCursor(Cursor.getDefaultCursor());
    }

    private void submitStepTwo() { }

    private void submitStepThree() { }

    private void submitStepFour() {
        SwingWorker<Void, Void> task = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                importerTask();
                return null;
            }

            @Override
            protected void done() { nextAction(); }
        };

        task.execute();
    }

    private void submitStepFive() { }

    private void importerTask() {
        /* Getting inFiles */
        DefaultListModel<String> defaultAddFilesModel = getDefaultListModel(addFilesList);
        if(defaultAddFilesModel == null) { return; }
        List<String> inFilesList =  Collections.list(defaultAddFilesModel.elements());
        inFilesList = inFilesList.stream().map(String::trim).collect(Collectors.toList());

        /* Getting selected variables */
        DefaultListModel<String> defaultSelectedVariablesModel = getDefaultListModel(rightVariablesList);
        if(defaultSelectedVariablesModel == null) { return; }
        List<String> selectedVariables = Collections.list(defaultSelectedVariablesModel.elements());

        /* Getting geoOptions */
        Map<String, String> geoOptions = new HashMap<>();
        // Clipping Datasource
        String clippingDatasource = dataSourceTextField.getText();
        if(!clippingDatasource.isEmpty()) { geoOptions.put("pathToShp", clippingDatasource); }
        // Target Wkt
        String targetWkt = targetWktTextArea.getText();
        if(!targetWkt.isEmpty()) { geoOptions.put("targetWkt", targetWkt); }
        // Target Cell Size
        String targetCellSize = targetCellSizeTextField.getText();
        if(!targetCellSize.isEmpty()) { geoOptions.put("targetCellSize", targetCellSize); }
        // Resampling Method
        geoOptions.put("resamplingMethod", resamplingPanel.getSelected().toString());

        /* Getting Destination */
        String destination = destinationSelectionPanel.getDestinationTextField().getText();

        /* Setting parts */
        Map<String, Set<String>> pathnameParts = DssUtil.getPathnameParts(selectedVariables);
        List<String> partAList = new ArrayList<>(pathnameParts.get("aParts"));
        List<String> partBList = new ArrayList<>(pathnameParts.get("bParts"));
        List<String> partCList = new ArrayList<>(pathnameParts.get("cParts"));
        List<String> partFList = new ArrayList<>(pathnameParts.get("fParts"));
        String partA = (partAList.size() == 1) ? partAList.get(0) : "*";
        String partB = (partBList.size() == 1) ? partBList.get(0) : "*";
        String partC = (partCList.size() == 1) ? partCList.get(0) : "*";
        String partF = (partFList.size() == 1) ? partFList.get(0) : "*";

        Map<String, String> writeOptions = new HashMap<>();
        String dssFieldA = destinationSelectionPanel.getFieldA().getText();
        String dssFieldB = destinationSelectionPanel.getFieldB().getText();
        String dssFieldC = destinationSelectionPanel.getFieldC().getText();
        String dssFieldF = destinationSelectionPanel.getFieldF().getText();

        if (destination.toLowerCase().endsWith(".dss")) {
            writeOptions.put("partA", (dssFieldA.isEmpty()) ? partA : dssFieldA);
            writeOptions.put("partB", (dssFieldB.isEmpty()) ? partB : dssFieldB);
            writeOptions.put("partC", (dssFieldC.isEmpty()) ? partC : dssFieldC);
            writeOptions.put("partF", (dssFieldF.isEmpty()) ? partF : dssFieldF);
        }

        if (destination.toLowerCase().endsWith(".nc")) {
            writeOptions.put("isOverwrite", String.valueOf(destinationSelectionPanel.isOverwrite()));
        }

        String unitsString = destinationSelectionPanel.getUnitsString();
        if (!unitsString.isEmpty())
            writeOptions.put("units", unitsString);

        String dataType = destinationSelectionPanel.getDataType();
        if (dataType != null && !dataType.isEmpty())
            writeOptions.put("dataType", dataType);

        BatchImporter importer = BatchImporter.builder()
                .inFiles(inFilesList)
                .variables(selectedVariables)
                .geoOptions(geoOptions)
                .destination(destination)
                .writeOptions(writeOptions)
                .build();

        importer.addPropertyChangeListener(evt -> {
            if (VortexProperty.STATUS.equals(evt.getPropertyName())) {
                String value = String.valueOf(evt.getNewValue());
                String message = TextProperties.getInstance().getProperty(value);
                processingLabel.setText(message);
            }

            if (VortexProperty.PROGRESS.equals(evt.getPropertyName())) {
                if (!(evt.getNewValue() instanceof Integer)) return;
                int progressValue = (int) evt.getNewValue();
                progressBar.setIndeterminate(false);
                progressBar.setStringPainted(true);
                progressBar.setValue(progressValue);
                progressBar.setString(progressValue + "%");
            }

            if (VortexProperty.ERROR.equals(evt.getPropertyName())) {
                String errorMessage = String.valueOf(evt.getNewValue());
                JOptionPane.showMessageDialog(this, errorMessage,
                        "Error: Failed to write", JOptionPane.ERROR_MESSAGE);
                setImportStatusMessageLabel(false);
            }

            if (VortexProperty.COMPLETE.equals(evt.getPropertyName())) {
                setImportStatusMessageLabel(true);
            }
        });

        importer.process();
    }

    private boolean unknownStepError() {
        logger.log(Level.SEVERE, "Unknown Step in Wizard");
        return false;
    }

    private void sortDssVariables(Set<String> variableList) {
        DefaultListModel<String> defaultListModel = getDefaultListModel(addFilesList);
        if(defaultListModel == null) { return; }
        List<String> fileList = Collections.list(defaultListModel.elements());

        Set<String> extensions = fileList.stream()
                .map(file -> file.substring(file.length() - 3))
                .collect(Collectors.toSet());

        if(extensions.size() == 1 && extensions.iterator().next().equals("dss")) {
            Util.sortDssVariables(variableList);
        } // If: Only one DSS file. Then sort variables by part
    }

    private JPanel stepOnePanel() {
        JPanel stepOnePanel = new JPanel(new BorderLayout());
        stepOnePanel.setBorder(BorderFactory.createEmptyBorder(5,10,5,10));

        /* addFilesLabel */
        JLabel addFilesLabel = new JLabel(TextProperties.getInstance().getProperty("ImportMetWizAddFiles"));
        addFilesLabel.setBorder(BorderFactory.createEmptyBorder(0,0,5,0));
        stepOnePanel.add(addFilesLabel, BorderLayout.NORTH);

        /* JList */
        DefaultListModel<String> addFilesListModel = new DefaultListModel<>();
        addFilesList = new JList<>(addFilesListModel);
        addFilesList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if(e.getKeyCode() == KeyEvent.VK_DELETE) {
                    addFilesListModel.remove(addFilesList.getSelectedIndex());
                    addFilesList.revalidate();
                    addFilesList.repaint();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(addFilesList);
        stepOnePanel.add(scrollPane, BorderLayout.CENTER);

        /* Browse Button */
        JPanel browsePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0,0));
        String uniqueId = this.getClass().getName() + ".addFiles";
        FileBrowseButton browseButton = new FileBrowseButton(uniqueId, "");
        browseButton.setIcon(IconResources.loadIcon("images/Open16.gif"));
        browseButton.setPreferredSize(new Dimension(22,22));
        browsePanel.add(Box.createRigidArea(new Dimension(8,0)));
        browsePanel.add(browseButton);
        stepOnePanel.add(browsePanel, BorderLayout.EAST);
        browseButton.addActionListener(arg0 -> addFilesBrowseAction(browseButton));

        return stepOnePanel;
    }

    private JPanel stepTwoPanel() {
        JPanel stepTwoPanel = new JPanel(new BorderLayout());
        stepTwoPanel.setBorder(BorderFactory.createEmptyBorder(5,10,5,10));

        /* selectVariablesLabel */
        JLabel selectVariablesLabel = new JLabel(TextProperties.getInstance().getProperty("ImportMetWizSelectVariables"));
        selectVariablesLabel.setBorder(BorderFactory.createEmptyBorder(0,0,5,0));
        stepTwoPanel.add(selectVariablesLabel, BorderLayout.NORTH);

        /* Left Variables List */
        DefaultListModel<String> leftVariablesListModel = new DefaultListModel<>();
        leftVariablesList = new JList<>(leftVariablesListModel);
        leftVariablesList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(e.getClickCount() == 2) { updateVariablesAction(VariableChooserAction.ADD); }
            }
        });
        JScrollPane leftScrollPanel = new JScrollPane();
        leftScrollPanel.setViewportView(leftVariablesList);

        /* Right Variables List */
        DefaultListModel<String> rightVariablesListModel = new DefaultListModel<>();
        rightVariablesList = new JList<>(rightVariablesListModel);
        rightVariablesList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(e.getClickCount() == 2) { updateVariablesAction(VariableChooserAction.REMOVE); }
            }
        });
        JScrollPane rightScrollPanel = new JScrollPane();
        rightScrollPanel.setViewportView(rightVariablesList);

        /* Transfer Buttons Panel */
        JPanel transferButtonsPanel = new JPanel();
        transferButtonsPanel.setLayout(new BoxLayout(transferButtonsPanel, BoxLayout.Y_AXIS));
        JButton addVariableButton = new JButton(IconResources.loadIcon("images/right-arrow-24.png"));
        addVariableButton.setPreferredSize(new Dimension(22,22));
        addVariableButton.setMaximumSize(new Dimension(22,22));
        addVariableButton.addActionListener(evt -> updateVariablesAction(VariableChooserAction.ADD));
        JButton removeVariableButton = new JButton(IconResources.loadIcon("images/left-arrow-24.png"));
        removeVariableButton.setPreferredSize(new Dimension(22,22));
        removeVariableButton.setMaximumSize(new Dimension(22,22));
        removeVariableButton.addActionListener(evt -> updateVariablesAction(VariableChooserAction.REMOVE));
        transferButtonsPanel.add(addVariableButton);
        transferButtonsPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        transferButtonsPanel.add(removeVariableButton);

        /* Adding all components to Step Two's main content */
        JPanel mainContentPanel = new JPanel();
        mainContentPanel.setLayout(new BoxLayout(mainContentPanel, BoxLayout.X_AXIS));
        mainContentPanel.add(leftScrollPanel);
        mainContentPanel.add(Box.createRigidArea(new Dimension(4,0)));
        mainContentPanel.add(transferButtonsPanel);
        mainContentPanel.add(Box.createRigidArea(new Dimension(4,0)));
        mainContentPanel.add(rightScrollPanel);
        stepTwoPanel.add(mainContentPanel, BorderLayout.CENTER);

        /* Setting Preferred Sizes of Components */
        int mainContentWidth = mainContentPanel.getPreferredSize().width;
        int scrollPanelWidth = mainContentWidth * 45 / 100;
        leftScrollPanel.setPreferredSize(new Dimension(scrollPanelWidth, 0));
        rightScrollPanel.setPreferredSize(new Dimension(scrollPanelWidth, 0));

        return stepTwoPanel;
    }

    private JPanel stepThreePanel() {
        GridBagLayout gridBagLayout = new GridBagLayout();
        gridBagLayout.columnWidths = new int[]{this.getPreferredSize().width, 0};
        gridBagLayout.rowHeights = new int[]{50, 100, 50, 50, 0};
        gridBagLayout.columnWeights = new double[]{1.0, Double.MIN_VALUE};
        gridBagLayout.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};

        JPanel stepThreePanel = new JPanel(gridBagLayout);
        stepThreePanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,7));

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(0, 0, 5, 0);
        gridBagConstraints.gridx = 0;

        /* Data Source Section Panel */
        gridBagConstraints.gridy = 0;
        stepThreePanel.add(dataSourceSectionPanel(), gridBagConstraints);

        /* Target Wkt Section Panel */
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weighty = 1;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        stepThreePanel.add(targetWktSectionPanel(), gridBagConstraints);

        /* Target Cell Size Section Panel */
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weighty = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        stepThreePanel.add(targetCellSizeSectionPanel(), gridBagConstraints);

        /* Resampling Method Section Panel */
        gridBagConstraints.gridy = 3;
        resamplingPanel = new ResamplingMethodSelectionPanel();
        stepThreePanel.add(resamplingPanel, gridBagConstraints);

        return stepThreePanel;
    }

    private JPanel stepFourPanel() {
        destinationSelectionPanel = new DestinationSelectionPanel(this);
        return destinationSelectionPanel;
    }

    private JPanel stepFivePanel() {
        JPanel stepFivePanel = new JPanel(new GridBagLayout());

        JPanel insidePanel = new JPanel();
        insidePanel.setLayout(new BoxLayout(insidePanel, BoxLayout.Y_AXIS));

        processingLabel = new JLabel(TextProperties.getInstance().getProperty("ImportMetWizProcessing_L"));
        JPanel processingPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        processingPanel.add(processingLabel);
        insidePanel.add(processingPanel);

        progressBar = new JProgressBar(0, 100);
        progressBar.setIndeterminate(true);
        progressBar.setStringPainted(false);
        JPanel progressPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        progressPanel.add(progressBar);
        insidePanel.add(progressPanel);

        stepFivePanel.add(insidePanel);

        return stepFivePanel;
    }

    private JPanel stepSixPanel() {
        JPanel stepSixPanel = new JPanel(new GridBagLayout());
        importStatusMessageLabel = new JLabel();
        stepSixPanel.add(importStatusMessageLabel);
        return stepSixPanel;
    }

    private void setImportStatusMessageLabel(boolean isImportSuccessful) {
        String messageKey = isImportSuccessful ? "ImportMetWizComplete_L" : "ImportMetWizFailed_L";
        String message = TextProperties.getInstance().getProperty(messageKey);
        importStatusMessageLabel.setText(message);
    }

    private JPanel dataSourceSectionPanel() {
        /* Clipping DataSource section (of stepThreePanel) */
        JLabel dataSourceLabel = new JLabel(TextProperties.getInstance().getProperty("ImportMetWizClippingDatasourceL"));
        JPanel dataSourceLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        dataSourceLabelPanel.add(dataSourceLabel);

        JPanel dataSourceTextFieldPanel = new JPanel();
        dataSourceTextFieldPanel.setLayout(new BoxLayout(dataSourceTextFieldPanel, BoxLayout.X_AXIS));

        dataSourceTextFieldPanel.add(Box.createRigidArea(new Dimension(4,0)));

        dataSourceTextField = new JTextField();
        dataSourceTextField.setFont(addFilesList.getFont());
        dataSourceTextField.setColumns(0);
        dataSourceTextField.setBorder(null);
        JScrollPane layerPanel = new JScrollPane(dataSourceTextField);
        dataSourceTextFieldPanel.add(layerPanel);

        dataSourceTextFieldPanel.add(Box.createRigidArea(new Dimension(8,0)));

        String uniqueId = this.getClass().getName() + ".dataSource";
        FileBrowseButton dataSourceBrowseButton = new FileBrowseButton(uniqueId, "");
        dataSourceBrowseButton.setIcon(IconResources.loadIcon("images/Open16.gif"));
        dataSourceBrowseButton.setPreferredSize(new Dimension(22,22));
        dataSourceBrowseButton.addActionListener(evt -> dataSourceBrowseAction(dataSourceBrowseButton));
        dataSourceTextFieldPanel.add(dataSourceBrowseButton);

        JPanel dataSourceSectionPanel = new JPanel();
        dataSourceSectionPanel.setLayout(new BoxLayout(dataSourceSectionPanel, BoxLayout.Y_AXIS));
        dataSourceSectionPanel.add(dataSourceLabelPanel);
        dataSourceSectionPanel.add(dataSourceTextFieldPanel);

        return dataSourceSectionPanel;
    }

    private JPanel targetWktSectionPanel() {
        /* Target Wkt section (of stepThreePanel) */
        JLabel targetWktLabel = new JLabel(TextProperties.getInstance().getProperty("ImportMetWizTargetWktL"));
        JPanel targetWktLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        targetWktLabelPanel.add(targetWktLabel);

        JPanel targetWktTextAreaPanel = new JPanel();
        targetWktTextAreaPanel.setLayout(new BoxLayout(targetWktTextAreaPanel, BoxLayout.X_AXIS));
        targetWktTextAreaPanel.add(Box.createRigidArea(new Dimension(4,0)));

        targetWktTextArea = new JTextArea();
        targetWktTextArea.setLineWrap(true);
        targetWktTextArea.setColumns(0);
        targetWktTextArea.setRows(5);
        targetWktTextArea.setBorder(null);
        targetWktTextArea.setFont(addFilesList.getFont());
        JScrollPane targetWktTextAreaScrollPanel = new JScrollPane(targetWktTextArea);
        targetWktTextAreaPanel.add(targetWktTextAreaScrollPanel);

        targetWktTextAreaPanel.add(Box.createRigidArea(new Dimension(8,0)));

        JPanel targetWktButtonsPanel = new JPanel();
        targetWktButtonsPanel.setLayout(new BoxLayout(targetWktButtonsPanel, BoxLayout.Y_AXIS));

        String uniqueId = this.getClass().getName() + ".targetWkt";
        FileBrowseButton targetWktBrowseButton = new FileBrowseButton(uniqueId, "");
        targetWktBrowseButton.setIcon(IconResources.loadIcon("images/Open16.gif"));
        targetWktBrowseButton.setPreferredSize(new Dimension(22,22));
        targetWktBrowseButton.addActionListener(evt -> targetWktBrowseAction(targetWktBrowseButton));
        JButton targetWktProjectionButton = new JButton(IconResources.loadIcon("images/geography-16.png"));
        targetWktProjectionButton.setPreferredSize(new Dimension(22,22));
        targetWktProjectionButton.addActionListener(evt -> targetWktProjectionAction());
        targetWktButtonsPanel.add(targetWktBrowseButton);
        targetWktButtonsPanel.add(Box.createRigidArea(new Dimension(0,5)));
        targetWktButtonsPanel.add(targetWktProjectionButton);
        targetWktButtonsPanel.add(Box.createVerticalGlue());

        targetWktTextAreaPanel.add(targetWktButtonsPanel);

        JPanel targetWktSectionPanel = new JPanel(new BorderLayout());
        targetWktSectionPanel.add(targetWktLabelPanel, BorderLayout.NORTH);
        targetWktSectionPanel.add(targetWktTextAreaPanel, BorderLayout.CENTER);

        return targetWktSectionPanel;
    }

    private JPanel targetCellSizeSectionPanel() {
        /* Target Cell Size section (of stepThreePanel) */
        JLabel targetCellSizeLabel = new JLabel(TextProperties.getInstance().getProperty("ImportMetWizTargetCellSizeL"));
        JPanel targetCellSizeLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        targetCellSizeLabelPanel.add(targetCellSizeLabel);

        JPanel targetCellSizeTextFieldPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        targetCellSizeTextFieldPanel.setLayout(new BoxLayout(targetCellSizeTextFieldPanel, BoxLayout.X_AXIS));
        targetCellSizeTextFieldPanel.add(Box.createRigidArea(new Dimension(4,0)));

        targetCellSizeTextField = new JTextField();
        targetCellSizeTextField.setColumns(0);
        targetCellSizeTextField.setBorder(null);
        JScrollPane layerPanel = new JScrollPane(targetCellSizeTextField);
        targetCellSizeTextFieldPanel.add(layerPanel);
        targetCellSizeTextFieldPanel.add(Box.createRigidArea(new Dimension(8,0)));

        JButton selectCellSizeButton = new JButton(IconResources.loadIcon("images/grid-16.png"));
        selectCellSizeButton.setPreferredSize(new Dimension(22,22));
        selectCellSizeButton.addActionListener(evt -> targetCellSizeSelectionAction());
        targetCellSizeTextFieldPanel.add(selectCellSizeButton);

        JPanel targetCellSizeSectionPanel = new JPanel();
        targetCellSizeSectionPanel.setLayout(new BoxLayout(targetCellSizeSectionPanel, BoxLayout.Y_AXIS));
        targetCellSizeSectionPanel.add(targetCellSizeLabelPanel);
        targetCellSizeSectionPanel.add(targetCellSizeTextFieldPanel);

        return targetCellSizeSectionPanel;
    }

    /* Actions */
    private enum VariableChooserAction {
        ADD, REMOVE
    }

    private void updateVariablesAction(VariableChooserAction action) {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        SwingWorker<Map<String, List<String>>, Void> worker = new SwingWorker<>() {
            @Override
            protected Map<String, List<String>> doInBackground() {
                DefaultListModel<String> defaultLeftModel = getDefaultListModel(leftVariablesList);
                DefaultListModel<String> defaultRightModel = getDefaultListModel(rightVariablesList);

                if (defaultLeftModel == null || defaultRightModel == null) {
                    return Collections.emptyMap();
                }

                /* MUST be a LinkedHashSet to preserve order */
                Set<String> availableVariableSet = new LinkedHashSet<>(Collections.list(defaultLeftModel.elements()));
                Set<String> selectedVariableSet = new LinkedHashSet<>(Collections.list(defaultRightModel.elements()));

                if (action == VariableChooserAction.ADD) {
                    List<String> selectedVariables = leftVariablesList.getSelectedValuesList();
                    selectedVariables.forEach(availableVariableSet::remove);
                    selectedVariableSet.addAll(selectedVariables);
                } else if (action == VariableChooserAction.REMOVE) {
                    List<String> selectedVariables = rightVariablesList.getSelectedValuesList();
                    selectedVariables.forEach(selectedVariableSet::remove);
                    availableVariableSet.addAll(selectedVariables);
                }

                return generateSortedLists(availableVariableSet, selectedVariableSet);
            }

            @Override
            protected void done() {
                DefaultListModel<String> defaultLeftModel = getDefaultListModel(leftVariablesList);
                DefaultListModel<String> defaultRightModel = getDefaultListModel(rightVariablesList);
                if (defaultLeftModel == null || defaultRightModel == null) return;

                try {
                    Map<String, List<String>> lists = get();
                    if (lists.isEmpty()) return;

                    defaultLeftModel.clear();
                    defaultRightModel.clear();

                    defaultLeftModel.addAll(lists.getOrDefault("available", Collections.emptyList()));
                    defaultRightModel.addAll(lists.getOrDefault("selected", Collections.emptyList()));
                } catch (InterruptedException | ExecutionException e) {
                    logger.warning(e.getMessage());
                    Thread.currentThread().interrupt();
                } finally {
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        };
        worker.execute();
    }

    private Map<String, List<String>> generateSortedLists(Set<String> available, Set<String> selected) {

        sortDssVariables(available);
        sortDssVariables(selected);

        Map<String, List<String>> resultMap = new HashMap<>();
        resultMap.put("available", new ArrayList<>(available));
        resultMap.put("selected", new ArrayList<>(selected));

        return resultMap;
    }

    private void addFilesBrowseAction(FileBrowseButton fileBrowseButton) {
        JFileChooser fileChooser = new JFileChooser(fileBrowseButton.getPersistedBrowseLocation());
        fileChooser.setMultiSelectionEnabled(true);

        // Configuring fileChooser dialog
        fileChooser.setAcceptAllFileFilterUsed(false);
        FileNameExtensionFilterEnhanced recognizedFilter = new FileNameExtensionFilterEnhanced(
                "All recognized files", ".nc", ".nc4", ".hdf", ".hdf5", ".h5",
                ".grib", ".gb2", ".grb2", ".grib2", ".grb",
                ".asc", ".bil", "*bil.zip", ".dss", ".tif", ".tiff", ".dat", ".tar", "bil.zip");
        fileChooser.addChoosableFileFilter(recognizedFilter);
        FileNameExtensionFilterEnhanced ncFilter = new FileNameExtensionFilterEnhanced(
                "netCDF datasets", ".nc", ".nc4");
        fileChooser.addChoosableFileFilter(ncFilter);
        FileNameExtensionFilterEnhanced hdfFilter = new FileNameExtensionFilterEnhanced(
                "HDF datasets", ".hdf", ".hdf5");
        fileChooser.addChoosableFileFilter(hdfFilter);
        FileNameExtensionFilterEnhanced gribFilter = new FileNameExtensionFilterEnhanced(
                "GRIB datasets", ".grib", ".gb2", ".grb2", ".grib2", ".grb");
        fileChooser.addChoosableFileFilter(gribFilter);

        FileNameExtensionFilterEnhanced ascFilter = new FileNameExtensionFilterEnhanced(
                "ASC datasets", ".asc");
        fileChooser.addChoosableFileFilter(ascFilter);

        FileNameExtensionFilterEnhanced tifFilter = new FileNameExtensionFilterEnhanced(
                "TIF datasets", ".tif");
        fileChooser.addChoosableFileFilter(tifFilter);

        FileNameExtensionFilterEnhanced bilFilter = new FileNameExtensionFilterEnhanced(
                "BIL datasets", ".bil", "bil.zip");
        fileChooser.addChoosableFileFilter(bilFilter);

        FileNameExtensionFilterEnhanced tarFilter = new FileNameExtensionFilterEnhanced(
                "SNODAS datasets", ".tar", ".dat");
        fileChooser.addChoosableFileFilter(tarFilter);

        FileNameExtensionFilterEnhanced dssFilter = new FileNameExtensionFilterEnhanced(
                "DSS datasets", ".dss");
        fileChooser.addChoosableFileFilter(dssFilter);

        FileNameExtensionFilterEnhanced allFilesFilter = new FileNameExtensionFilterEnhanced(
                "All files", "");
        fileChooser.addChoosableFileFilter(allFilesFilter);

        // Pop up fileChooser dialog
        int userChoice = fileChooser.showOpenDialog(this);

        // Deal with user's choice
        if(userChoice == JFileChooser.APPROVE_OPTION) {
            File[] selectedFiles = fileChooser.getSelectedFiles();
            ListModel<String> listModel = addFilesList.getModel();
            if(listModel instanceof DefaultListModel) {
                DefaultListModel<String> defaultListModel = (DefaultListModel<String>) listModel;
                List<String> elementList = Collections.list(defaultListModel.elements());
                for(File file : selectedFiles) {
                    if(!elementList.contains(file.getAbsolutePath()))
                        defaultListModel.addElement(file.getAbsolutePath());
                }
                fileBrowseButton.setPersistedBrowseLocation(selectedFiles[0]);
            }
        } // If: User selected OK -> Add to 'AddFiles' List
    }

    private void dataSourceBrowseAction(FileBrowseButton fileBrowseButton) {
        JFileChooser fileChooser = new JFileChooser(fileBrowseButton.getPersistedBrowseLocation());

        // Configuring fileChooser dialog
        fileChooser.setAcceptAllFileFilterUsed(false);
        FileNameExtensionFilterEnhanced acceptableExtension = new FileNameExtensionFilterEnhanced("Shapefiles (*.shp)", ".shp");
        fileChooser.addChoosableFileFilter(acceptableExtension);

        // Pop up fileChooser dialog
        int userChoice = fileChooser.showOpenDialog(this);

        // Deal with user's choice
        if(userChoice == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            dataSourceTextField.setText(selectedFile.getAbsolutePath());
            fileBrowseButton.setPersistedBrowseLocation(selectedFile);
        } // If: User selected OK -> Add to 'AddFiles' List
    }

    private void targetWktBrowseAction(FileBrowseButton fileBrowseButton) {
        JFileChooser fileChooser = new JFileChooser(fileBrowseButton.getPersistedBrowseLocation());

        // Configuring fileChooser dialog
        fileChooser.setAcceptAllFileFilterUsed(false);
        FileNameExtensionFilterEnhanced acceptableExtension = new FileNameExtensionFilterEnhanced("Projection Files (*.prj)", ".prj");
        fileChooser.setFileFilter(acceptableExtension);

        // Pop up fileChooser dialog
        int userChoice = fileChooser.showOpenDialog(this);

        // Deal with user's choice
        if(userChoice == JFileChooser.APPROVE_OPTION) {
            try {
                File selectedFile = fileChooser.getSelectedFile();
                String wkt = Files.readString(Paths.get(selectedFile.getAbsolutePath()));
                targetWktTextArea.setText(wkt);
                fileBrowseButton.setPersistedBrowseLocation(selectedFile);
            } catch (IOException e) { logger.log(Level.WARNING, e.toString()); }
        } // If: User selected OK -> Add to 'AddFiles' List
    }

    private void targetWktProjectionAction() {
        ProjectionsComboBox projectionComboBox = new ProjectionsComboBox();
        projectionComboBox.setSelectedItem("SHG");

        JDialog projectionDialog = new JDialog();
        projectionDialog.setSize(240, 100);
        projectionDialog.setIconImage(IconResources.loadImage("images/vortex_black.png"));
        projectionDialog.setLayout(new BorderLayout());
        projectionDialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        projectionDialog.setLocationRelativeTo(this);

        JPanel contentPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        contentPanel.add(new JLabel(TextProperties.getInstance().getProperty("ImportMetWizSelectProjection")));
        contentPanel.add(projectionComboBox);
        projectionDialog.add(BorderLayout.CENTER, contentPanel);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton projectionOkButton = new JButton(TextProperties.getInstance().getProperty("ImportMetWizOk"));
        projectionOkButton.addActionListener(evt -> {
            targetWktTextArea.setText(projectionComboBox.getWkt());
            projectionDialog.setVisible(false);
            projectionDialog.dispose();
        });
        JButton projectionCancelButton = new JButton(TextProperties.getInstance().getProperty("ImportMetWizCancel"));
        projectionCancelButton.addActionListener(evt -> { projectionDialog.setVisible(false); projectionDialog.dispose();});
        buttonPanel.add(projectionOkButton);
        buttonPanel.add(projectionCancelButton);
        projectionDialog.add(buttonPanel, BorderLayout.SOUTH);

        projectionDialog.setVisible(true);
    }

    private void targetCellSizeSelectionAction() {
        CellSizesComboBox cellSizeComboBox = new CellSizesComboBox();
        cellSizeComboBox.setSelectedItem("2000");

        JDialog cellSizeDialog = new JDialog();
        cellSizeDialog.setSize(240, 100);
        cellSizeDialog.setIconImage(IconResources.loadImage("images/vortex_black.png"));
        cellSizeDialog.setLayout(new BorderLayout());
        cellSizeDialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        cellSizeDialog.setLocationRelativeTo(this);

        JPanel contentPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        contentPanel.add(new JLabel(TextProperties.getInstance().getProperty("ImportMetWizSelectCellSize")));
        contentPanel.add(cellSizeComboBox);
        cellSizeDialog.add(BorderLayout.CENTER, contentPanel);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton projectionOkButton = new JButton(TextProperties.getInstance().getProperty("ImportMetWizOk"));
        projectionOkButton.addActionListener(evt -> {
            String chosenCellSize = String.valueOf(cellSizeComboBox.getSelectedItem());
            targetCellSizeTextField.setText(chosenCellSize);
            cellSizeDialog.setVisible(false);
            cellSizeDialog.dispose();
        });
        JButton projectionCancelButton = new JButton(TextProperties.getInstance().getProperty("ImportMetWizCancel"));
        projectionCancelButton.addActionListener(evt -> { cellSizeDialog.setVisible(false); cellSizeDialog.dispose();});
        buttonPanel.add(projectionOkButton);
        buttonPanel.add(projectionCancelButton);
        cellSizeDialog.add(buttonPanel, BorderLayout.SOUTH);

        cellSizeDialog.setVisible(true);
    }

    private DefaultListModel<String> getDefaultListModel(JList<String> list) {
        ListModel<String> listModel = list.getModel();
        if(!(listModel instanceof DefaultListModel)) {
            logger.log(Level.SEVERE, list.getName() + " may have not been initialized");
            return null;
        } // If: listModel is not a DefaultListModel -- should not be happening

        return (DefaultListModel<String>) listModel;
    }

    private void closeAction() {
        ImportMetWizard.this.setVisible(false);
        ImportMetWizard.this.dispose();
        boolean isSuccessful = importStatusMessageLabel.getText().equals(TextProperties.getInstance().getProperty("ImportMetWizComplete_L"));
        if (isSuccessful) {
            String savedFile = destinationSelectionPanel.getDestinationTextField().getText();
            FileSaveUtil.showFileLocation(ImportMetWizard.this, Path.of(savedFile));
        }
    }

    /* Add main for quick UI Testing */
    static public void main(String[] args) {
        FlatLightLaf.setup();
        ImportMetWizard metWizard = new ImportMetWizard(null);
        metWizard.buildAndShowUI();
    }

}
