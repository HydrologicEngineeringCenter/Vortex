package mil.army.usace.hec.vortex.ui;

import mil.army.usace.hec.vortex.io.BatchImporter;
import mil.army.usace.hec.vortex.io.DataReader;
import mil.army.usace.hec.vortex.util.DssUtil;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ImportMetWizard extends JFrame {
    private final Frame frame;

    private Container contentCards;
    private CardLayout cardLayout;
    private JButton backButton, nextButton, cancelButton;
    private JProgressBar progressBar;
    private int cardNumber;

    private JList<String> addFilesList, leftVariablesList, rightVariablesList;
    private JTextField dataSourceTextField, targetCellSizeTextField, selectDestinationTextField;
    private JTextField dssFieldA;
    private JTextField dssFieldB;
    private JTextField dssFieldC;
    private JTextField dssFieldF;
    private JTextArea targetWktTextArea;
    private JComboBox<String> resamplingComboBox;
    private JPanel dssPartsSectionPanel;

    private static final Logger logger = Logger.getLogger(ImportMetWizard.class.getName());

    public ImportMetWizard (Frame frame) {
        this.frame = frame;
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    public void buildAndShowUI() {

        /* Setting Wizard's names and layout */
        this.setTitle(TextProperties.getInstance().getProperty("ImportMetWizNameTitle"));
        this.setIconImage(IconResources.loadImage("images/vortex_black.png"));
        this.setMinimumSize(new Dimension(600, 400));
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
        cancelButton.addActionListener(evt -> {
            this.setVisible(false);
            this.dispose();
        });

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
        resamplingComboBox.setSelectedItem(TextProperties.getInstance().getProperty("ImportMetWizBilinear"));

        /* Clearing Step Four Panel */
        selectDestinationTextField.setText("");
        dssFieldA.setText("");
        dssFieldB.setText("");
        dssFieldF.setText("");

        /* Clearing Step Five Panel */
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
        String destinationPath = selectDestinationTextField.getText();

        if(selectDestinationTextField == null || destinationPath.isEmpty()) {
            /* Popup Alert of Missing Inputs */
            JOptionPane.showMessageDialog(this, "Destination file is required.",
                    "Error: Missing Field", JOptionPane.ERROR_MESSAGE);
            return false;
        } // If: Missing Destination File

        if(!destinationPath.endsWith(".dss")) {
            JOptionPane.showMessageDialog(this, "DSS file is required.",
                    "Error: Unsupported File Type", JOptionPane.ERROR_MESSAGE);
            return false;
        } // If: Path doesn't end with .dss

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
        List<String> variables = fileList.stream()
                .map(DataReader::getVariables)
                .flatMap(Collection::stream).distinct().sorted().collect(Collectors.toList());
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
        geoOptions.put("resamplingMethod", String.valueOf(resamplingComboBox.getSelectedItem()));

        /* Getting Destination */
        String destination = selectDestinationTextField.getText();

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
        if (destination.toLowerCase().endsWith(".dss")) {
            writeOptions.put("partA", (dssFieldA.getText().isEmpty()) ? partA : dssFieldA.getText());
            writeOptions.put("partB", (dssFieldB.getText().isEmpty()) ? partB : dssFieldB.getText());
            writeOptions.put("partC", (dssFieldC.getText().isEmpty()) ? partC : dssFieldC.getText());
            writeOptions.put("partF", (dssFieldF.getText().isEmpty()) ? partF : dssFieldF.getText());
        }

        BatchImporter importer = BatchImporter.builder()
                .inFiles(inFilesList)
                .variables(selectedVariables)
                .geoOptions(geoOptions)
                .destination(destination)
                .writeOptions(writeOptions)
                .build();

        importer.addPropertyChangeListener(evt -> {
            if(evt.getPropertyName().equalsIgnoreCase("progress")) {
                if(!(evt.getNewValue() instanceof Integer)) return;
                int progressValue = (int) evt.getNewValue();
                progressBar.setIndeterminate(false);
                progressBar.setStringPainted(true);
                progressBar.setValue(progressValue);
                progressBar.setString(progressValue + "%");
            }
        });

        importer.process();
    }

    private boolean unknownStepError() {
        logger.log(Level.SEVERE, "Unknown Step in Wizard");
        return false;
    }

    private void sortDssVariables(List<String> variableList) {
        DefaultListModel<String> defaultListModel = getDefaultListModel(addFilesList);
        if(defaultListModel == null) { return; }
        List<String> fileList = Collections.list(defaultListModel.elements());

        Set<String> extensions = fileList.stream()
                .map(file -> file.substring(file.length() - 3))
                .collect(Collectors.toSet());

        if(extensions.size() == 1 && extensions.iterator().next().equals("dss")) {
            try {
                DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                        .parseCaseInsensitive()
                        .appendPattern("ddMMMuuuu:HHmm")
                        .toFormatter();
                // Sort based on D part
                variableList.sort(Comparator.comparing(s -> LocalDateTime.parse(s.split("/")[4], formatter)));
                // Sort based on A part
                variableList.sort(Comparator.comparing(s -> s.split("/")[1]));
                // Sort based on B part
                variableList.sort(Comparator.comparing(s -> s.split("/")[2]));
                // Sort based on C part
                variableList.sort(Comparator.comparing(s -> s.split("/")[3]));
                // Sort based on F part
                variableList.sort(Comparator.comparing(s -> s.split("/")[6]));
            } catch (DateTimeParseException e) {
                Collections.sort(variableList);
            }
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
        FileBrowseButton browseButton = new FileBrowseButton(this.getClass().getName(), "");
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
                if(e.getClickCount() == 2) { addSelectedVariables(); }
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
                if(e.getClickCount() == 2) { removeSelectedVariables(); }
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
        addVariableButton.addActionListener(evt -> addSelectedVariables());
        JButton removeVariableButton = new JButton(IconResources.loadIcon("images/left-arrow-24.png"));
        removeVariableButton.setPreferredSize(new Dimension(22,22));
        removeVariableButton.setMaximumSize(new Dimension(22,22));
        removeVariableButton.addActionListener(evt -> removeSelectedVariables());
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
        stepThreePanel.add(resamplingMethodSectionPanel(), gridBagConstraints);

        return stepThreePanel;
    }

    private JPanel stepFourPanel() {
        GridBagLayout gridBagLayout = new GridBagLayout();
        gridBagLayout.columnWidths = new int[]{this.getPreferredSize().width, 0};
        gridBagLayout.rowHeights = new int[] {50, 100, 0};
        gridBagLayout.columnWeights = new double[]{1.0, Double.MIN_VALUE};
        gridBagLayout.rowWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};

        JPanel stepFourPanel = new JPanel(gridBagLayout);
        stepFourPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,8));

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.anchor = GridBagConstraints.NORTH;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new Insets(0, 0, 5, 0);
        gridBagConstraints.gridx = 0;

        /* Select Destination Section Panel */
        gridBagConstraints.gridy = 0;
        stepFourPanel.add(selectDestinationSectionPanel(), gridBagConstraints);

        /* DSS Parts Panel (Only appears when selected destination is a DSS file */
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new Insets(10,0,0,0);
        dssPartsSectionPanel = dssPartsSectionPanel();
        dssPartsSectionPanel.setVisible(false);
        stepFourPanel.add(dssPartsSectionPanel, gridBagConstraints);

        return stepFourPanel;
    }

    private JPanel stepFivePanel() {
        JPanel stepFivePanel = new JPanel(new GridBagLayout());

        JPanel insidePanel = new JPanel();
        insidePanel.setLayout(new BoxLayout(insidePanel, BoxLayout.Y_AXIS));

        JLabel processingLabel = new JLabel(TextProperties.getInstance().getProperty("ImportMetWizProcessing_L"));
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
        JLabel completeLabel = new JLabel(TextProperties.getInstance().getProperty("ImportMetWizComplete_L"));
        stepSixPanel.add(completeLabel);
        return stepSixPanel;
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

        FileBrowseButton dataSourceBrowseButton = new FileBrowseButton(this.getClass().getName(), "");
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

        FileBrowseButton targetWktBrowseButton = new FileBrowseButton(this.getClass().getName(), "");
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

    private JPanel resamplingMethodSectionPanel() {
        /* Resampling Method section (of stepThreePanel) */
        JLabel resamplingMethodLabel = new JLabel(TextProperties.getInstance().getProperty("ImportMetWizResamplingMethodL"));
        JPanel resamplingMethodLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        resamplingMethodLabelPanel.add(resamplingMethodLabel);

        Vector<String> resamplingMethods = new Vector<>();
        resamplingMethods.add(TextProperties.getInstance().getProperty("ImportMetWizNearestNeighbor"));
        resamplingMethods.add(TextProperties.getInstance().getProperty("ImportMetWizAverage"));
        resamplingMethods.add(TextProperties.getInstance().getProperty("ImportMetWizBilinear"));
        resamplingComboBox = new JComboBox<>();
        resamplingComboBox.setModel(new DefaultComboBoxModel<>(new Vector<>(resamplingMethods)));
        resamplingComboBox.setSelectedItem(TextProperties.getInstance().getProperty("ImportMetWizBilinear"));
        JPanel resamplingMethodComboBoxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        resamplingMethodComboBoxPanel.add(resamplingComboBox);

        JPanel resamplingMethodSectionPanel = new JPanel();
        resamplingMethodSectionPanel.setLayout(new BoxLayout(resamplingMethodSectionPanel, BoxLayout.Y_AXIS));
        resamplingMethodSectionPanel.add(resamplingMethodLabelPanel);
        resamplingMethodSectionPanel.add(resamplingMethodComboBoxPanel);

        return resamplingMethodSectionPanel;
    }

    private JPanel selectDestinationSectionPanel() {
        /* Select Destination section (of stepFourPanel) */
        JLabel selectDestinationLabel = new JLabel(TextProperties.getInstance().getProperty("ImportMetWizSelectDestinationL"));
        JPanel selectDestinationLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        selectDestinationLabelPanel.add(selectDestinationLabel);

        JPanel selectDestinationTextFieldPanel = new JPanel();
        selectDestinationTextFieldPanel.setLayout(new BoxLayout(selectDestinationTextFieldPanel, BoxLayout.X_AXIS));

        selectDestinationTextFieldPanel.add(Box.createRigidArea(new Dimension(4,0)));

        selectDestinationTextField = new JTextField();
        selectDestinationTextField.setFont(addFilesList.getFont());
        selectDestinationTextField.setColumns(0);
        selectDestinationTextField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { textUpdated(); }
            public void removeUpdate(DocumentEvent e) { textUpdated(); }
            public void insertUpdate(DocumentEvent e) { textUpdated(); }
            void textUpdated() { dssPartsSectionPanel.setVisible(selectDestinationTextField.getText().endsWith(".dss")); }
        });
        selectDestinationTextFieldPanel.add(selectDestinationTextField);

        selectDestinationTextFieldPanel.add(Box.createRigidArea(new Dimension(4,0)));

        FileBrowseButton selectDestinationBrowseButton = new FileBrowseButton(this.getClass().getName(), "");
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

    private JPanel dssPartsSectionPanel() {
        GridBagLayout dssGridLayout = new GridBagLayout();
        JPanel dssPartsPanel = new JPanel(dssGridLayout);

        GridBagConstraints dssPartsConstraints = new GridBagConstraints();
        dssPartsConstraints.fill = GridBagConstraints.HORIZONTAL;

        dssFieldA = new JTextField();
        dssFieldA.setFont(addFilesList.getFont());
        dssFieldB = new JTextField();
        dssFieldB.setFont(addFilesList.getFont());
        dssFieldC = new JTextField();
        dssFieldC.setFont(addFilesList.getFont());
        dssFieldC.setText("*");
        dssFieldC.setEditable(false);
        JTextField dssFieldD = new JTextField();
        dssFieldD.setFont(addFilesList.getFont());
        dssFieldD.setText("*");
        dssFieldD.setEditable(false);
        JTextField dssFieldE = new JTextField();
        dssFieldE.setFont(addFilesList.getFont());
        dssFieldE.setText("*");
        dssFieldE.setEditable(false);
        dssFieldF = new JTextField();
        dssFieldF.setFont(addFilesList.getFont());

        dssPartsConstraints.gridy = 0;
        dssPartsConstraints.gridx = 0;
        dssPartsConstraints.weightx = 1;
        JPanel partAPanel = new JPanel();
        partAPanel.setLayout(new BoxLayout(partAPanel, BoxLayout.X_AXIS));
        partAPanel.add(Box.createRigidArea(new Dimension(4,0)));
        partAPanel.add(new JLabel(TextProperties.getInstance().getProperty("ImportMetWizPartA_L")));
        partAPanel.add(dssFieldA);
        dssPartsPanel.add(partAPanel, dssPartsConstraints);

        dssPartsConstraints.gridy = 0;
        dssPartsConstraints.gridx = 1;
        dssPartsConstraints.weightx = 1;
        JPanel partBPanel = new JPanel();
        partBPanel.setLayout(new BoxLayout(partBPanel, BoxLayout.X_AXIS));
        partBPanel.add(Box.createRigidArea(new Dimension(4,0)));
        partBPanel.add(new JLabel(TextProperties.getInstance().getProperty("ImportMetWizPartB_L")));
        partBPanel.add(dssFieldB);
        dssPartsPanel.add(partBPanel, dssPartsConstraints);

        dssPartsConstraints.gridy = 0;
        dssPartsConstraints.gridx = 2;
        dssPartsConstraints.weightx = 1;
        JPanel partCPanel = new JPanel();
        partCPanel.setLayout(new BoxLayout(partCPanel, BoxLayout.X_AXIS));
        partCPanel.add(Box.createRigidArea(new Dimension(4,0)));
        partCPanel.add(new JLabel(TextProperties.getInstance().getProperty("ImportMetWizPartC_L")));
        partCPanel.add(dssFieldC);
        dssPartsPanel.add(partCPanel, dssPartsConstraints);

        dssPartsConstraints.gridy = 1;
        dssPartsConstraints.gridx = 0;
        dssPartsConstraints.weightx = 1;
        dssPartsConstraints.insets = new Insets(10,0,0,0);
        JPanel partDPanel = new JPanel();
        partDPanel.setLayout(new BoxLayout(partDPanel, BoxLayout.X_AXIS));
        partDPanel.add(Box.createRigidArea(new Dimension(4,0)));
        partDPanel.add(new JLabel(TextProperties.getInstance().getProperty("ImportMetWizPartD_L")));
        partDPanel.add(dssFieldD);
        dssPartsPanel.add(partDPanel, dssPartsConstraints);

        dssPartsConstraints.gridy = 1;
        dssPartsConstraints.gridx = 1;
        dssPartsConstraints.weightx = 1;
        JPanel partEPanel = new JPanel();
        partEPanel.setLayout(new BoxLayout(partEPanel, BoxLayout.X_AXIS));
        partEPanel.add(Box.createRigidArea(new Dimension(4,0)));
        partEPanel.add(new JLabel(TextProperties.getInstance().getProperty("ImportMetWizPartE_L")));
        partEPanel.add(dssFieldE);
        dssPartsPanel.add(partEPanel, dssPartsConstraints);

        dssPartsConstraints.gridy = 1;
        dssPartsConstraints.gridx = 2;
        dssPartsConstraints.weightx = 1;
        JPanel partFPanel = new JPanel();
        partFPanel.setLayout(new BoxLayout(partFPanel, BoxLayout.X_AXIS));
        partFPanel.add(Box.createRigidArea(new Dimension(4,0)));
        partFPanel.add(new JLabel(TextProperties.getInstance().getProperty("ImportMetWizPartF_L")));
        partFPanel.add(Box.createRigidArea(new Dimension(1,0)));
        partFPanel.add(dssFieldF);
        dssPartsPanel.add(partFPanel, dssPartsConstraints);

        return dssPartsPanel;
    }

    private void addSelectedVariables() {
        List<String> selectedVariables = leftVariablesList.getSelectedValuesList();

        /* Adding to Right Variables List */
        DefaultListModel<String> defaultRightModel = getDefaultListModel(rightVariablesList);
        if(defaultRightModel == null) { return; }
        List<String> rightVariablesList = Collections.list(defaultRightModel.elements());
        rightVariablesList.addAll(selectedVariables);
        sortDssVariables(rightVariablesList);
        defaultRightModel.clear();
        defaultRightModel.addAll(rightVariablesList);

        /* Removing from Left Variables List */
        DefaultListModel<String> defaultLeftModel = getDefaultListModel(leftVariablesList);
        if(defaultLeftModel == null) { return; }
        selectedVariables.forEach(defaultLeftModel::removeElement);
    }

    private void removeSelectedVariables() {
        List<String> selectedVariables = rightVariablesList.getSelectedValuesList();

        /* Adding to Left Variables List */
        DefaultListModel<String> defaultLeftModel = getDefaultListModel(leftVariablesList);
        if(defaultLeftModel == null) { return; }
        List<String> leftVariablesList = Collections.list(defaultLeftModel.elements());
        leftVariablesList.addAll(selectedVariables);
        sortDssVariables(leftVariablesList);
        defaultLeftModel.clear();
        defaultLeftModel.addAll(leftVariablesList);

        /* Removing from Left Variables List */
        DefaultListModel<String> defaultRightModel = getDefaultListModel(rightVariablesList);
        if(defaultRightModel == null) { return; }
        selectedVariables.forEach(defaultRightModel::removeElement);
    }

    private void addFilesBrowseAction(FileBrowseButton fileBrowseButton) {
        JFileChooser fileChooser = new JFileChooser(fileBrowseButton.getPersistedBrowseLocation());
        fileChooser.setMultiSelectionEnabled(true);

        // Configuring fileChooser dialog
        fileChooser.setAcceptAllFileFilterUsed(false);
        FileNameExtensionFilter acceptableExtension = new FileNameExtensionFilter("All recognized files",
                "nc", "nc4", "hdf", "hdf5", "h5", "grib", "gb2", "grb2",
                "grib2", "grb", "asc", "bil", "bil.zip", "dss", "tif", "tiff");
        fileChooser.addChoosableFileFilter(acceptableExtension);

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
                fileBrowseButton.setPersistedBrowseLocation(new File(defaultListModel.get(0)));
            }
        } // If: User selected OK -> Add to 'AddFiles' List
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
            dataSourceTextField.setText(selectedFile.getAbsolutePath());
            fileBrowseButton.setPersistedBrowseLocation(selectedFile);
        } // If: User selected OK -> Add to 'AddFiles' List
    }

    private void targetWktBrowseAction(FileBrowseButton fileBrowseButton) {
        JFileChooser fileChooser = new JFileChooser(fileBrowseButton.getPersistedBrowseLocation());

        // Configuring fileChooser dialog
        fileChooser.setAcceptAllFileFilterUsed(false);
        FileNameExtensionFilter acceptableExtension = new FileNameExtensionFilter("Projection Files (*.prj)", "prj");
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

    private void selectDestinationBrowseAction(FileBrowseButton fileBrowseButton) {
        JFileChooser fileChooser = new JFileChooser(fileBrowseButton.getPersistedBrowseLocation());

        // Configuring fileChooser dialog
        fileChooser.setAcceptAllFileFilterUsed(true);
        FileNameExtensionFilter acceptableExtension = new FileNameExtensionFilter("DSS Files (*.dss)", "dss");
        fileChooser.setFileFilter(acceptableExtension);

        // Pop up fileChooser dialog
        int userChoice = fileChooser.showOpenDialog(this);

        // Deal with user's choice
        if(userChoice == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String selectedPath = selectedFile.getAbsolutePath();
            if(!selectedFile.getName().contains(".")) { selectedPath = selectedPath + ".dss"; }
            selectDestinationTextField.setText(selectedPath);
            File finalFile = new File(selectedPath);
            fileBrowseButton.setPersistedBrowseLocation(finalFile);
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

    /* Add main for quick UI Testing */
    static public void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) { e.printStackTrace(); }

        ImportMetWizard metWizard = new ImportMetWizard(null);
        metWizard.buildAndShowUI();
    }

}