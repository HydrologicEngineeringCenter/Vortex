package mil.army.usace.hec.vortex.ui;

import mil.army.usace.hec.vortex.io.DataReader;
import mil.army.usace.hec.vortex.math.Normalizer;
import mil.army.usace.hec.vortex.util.DssUtil;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.OutputStream;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.*;
import java.util.logging.*;

public class NormalizerWizard extends JFrame {
    private final Frame frame;
    
    private Container contentCards;
    private CardLayout cardLayout;
    private JButton backButton, nextButton, cancelButton;
    private int cardNumber;

    private JTextField sourceFileTextField, normalFileTextField, selectDestinationTextField;
    private JTextField dssFieldA;
    private JTextField dssFieldB;
    private JTextField dssFieldC;
    private JTextField dssFieldF;
    private InputHintTextField startDateTextField, startTimeTextField;
    private InputHintTextField endDateTextField, endTimeTextField;
    private InputHintTextField normalIntervalTextField;
    private JList<String> availableSourceGridsList, chosenSourceGridsList;
    private JList<String> availableNormalGridsList, chosenNormalGridsList;
    private JPanel dssPartsSectionPanel;
    private JComboBox<String> timeUnitComboBox;
    private JProgressBar progressBar;

    private ZonedDateTime startDateTime, endDateTime;

    private static final Logger logger = Logger.getLogger(NormalizerWizard.class.getName());
    
    public NormalizerWizard(Frame frame) {
        this.frame = frame;

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    public void buildAndShowUI() {
        /* Setting Wizard's names and layout */
        this.setTitle(TextProperties.getInstance().getProperty("NormalizerWiz_Title"));
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
        contentCards.add("Step Six", stepSixPanel());
    }

    private void initializeButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        /* Back Button */
        backButton = new JButton(TextProperties.getInstance().getProperty("NormalizerWiz_Back"));
        backButton.setToolTipText(TextProperties.getInstance().getProperty("NormalizerWiz_Back_TT"));
        backButton.setEnabled(false);
        backButton.addActionListener(evt -> backAction());

        /* Next Button */
        nextButton = new JButton(TextProperties.getInstance().getProperty("NormalizerWiz_Next"));
        nextButton.setToolTipText(TextProperties.getInstance().getProperty("NormalizerWiz_Next_TT"));
        nextButton.addActionListener(evt -> {
            if(nextButton.getText().equals(TextProperties.getInstance().getProperty("NormalizerWiz_Restart"))) { restartAction(); }
            else if(nextButton.getText().equals(TextProperties.getInstance().getProperty("NormalizerWiz_Next"))) { nextAction(); }
        });

        /* Cancel Button */
        cancelButton = new JButton(TextProperties.getInstance().getProperty("NormalizerWiz_Cancel"));
        cancelButton.setToolTipText(TextProperties.getInstance().getProperty("NormalizerWiz_Cancel_TT"));
        cancelButton.addActionListener(evt -> {
            this.setVisible(false);
            this.dispose();
        });

        /* Adding Buttons to NavigationPanel */
        buttonPanel.add(backButton);
        buttonPanel.add(nextButton);
        buttonPanel.add(cancelButton);

        /* Add buttonPanel to NormalizerWizard */
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

        cardLayout.next(contentCards);
    }

    private void backAction() {
        cardNumber--;
        if(cardNumber == 0) {
            backButton.setEnabled(false);
        }
        cardLayout.previous(contentCards);
    }

    private void normalizerEndUI() {
        backButton.setVisible(false);
        nextButton.setText(TextProperties.getInstance().getProperty("NormalizerWiz_Restart"));
        nextButton.setToolTipText(TextProperties.getInstance().getProperty("NormalizerWiz_Restart_TT"));
        nextButton.setEnabled(true);
        cancelButton.setText(TextProperties.getInstance().getProperty("NormalizerWiz_Close"));
        cancelButton.setToolTipText(TextProperties.getInstance().getProperty("NormalizerWiz_Close_TT"));
    }

    private void restartAction() {
        cardNumber = 0;
        cardLayout.first(contentCards);

        /* Resetting Buttons */
        backButton.setVisible(true);
        backButton.setEnabled(false);

        nextButton.setEnabled(true);
        nextButton.setText(TextProperties.getInstance().getProperty("NormalizerWiz_Next"));
        nextButton.setToolTipText(TextProperties.getInstance().getProperty("NormalizerWiz_Next_TT"));

        cancelButton.setText(TextProperties.getInstance().getProperty("NormalizerWiz_Cancel"));
        cancelButton.setToolTipText(TextProperties.getInstance().getProperty("NormalizerWiz_Cancel_TT"));

        /* Clearing Step One Panel */
        sourceFileTextField.setText("");
        Objects.requireNonNull(getDefaultListModel(availableSourceGridsList)).clear();
        Objects.requireNonNull(getDefaultListModel(chosenSourceGridsList)).clear();

        /* Clearing Step Two Panel */
        normalFileTextField.setText("");
        Objects.requireNonNull(getDefaultListModel(availableNormalGridsList)).clear();
        Objects.requireNonNull(getDefaultListModel(chosenNormalGridsList)).clear();

        /* Clearing Step Three Panel */
        startDateTextField.resetTextField();
        startTimeTextField.resetTextField();
        endDateTextField.resetTextField();
        endTimeTextField.resetTextField();
        normalIntervalTextField.resetTextField();
        timeUnitComboBox.setSelectedItem(TextProperties.getInstance().getProperty("NormalizerWiz_TimeUnit_Days"));

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
        JLabel selectSourceFileLabel = new JLabel(TextProperties.getInstance().getProperty("NormalizerWiz_SelectSourceFile_L"));
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
        JLabel selectSourceGridsLabel = new JLabel(TextProperties.getInstance().getProperty("NormalizerWiz_SelectSourceGrids_L"));
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
        /* selectNormalFilePanel and selectNormalGridsPanel*/
        JPanel selectNormalFilePanel = stepTwoNormalFilePanel();
        JPanel selectNormalGridsPanel = stepTwoNormalGridsPanel();

        /* Setting GridBagLayout for stepTwoPanel */
        GridBagLayout gridBagLayout = new GridBagLayout();
        gridBagLayout.columnWidths = new int[]{this.getPreferredSize().width, 0};
        gridBagLayout.rowHeights = new int[]{50, 100};
        gridBagLayout.columnWeights = new double[]{1.0, Double.MIN_VALUE};
        gridBagLayout.rowWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};

        /* Adding Panels to stepTwoPanel */
        JPanel stepTwoPanel = new JPanel(gridBagLayout);
        stepTwoPanel.setBorder(BorderFactory.createEmptyBorder(5,9,5,8));

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(0, 0, 5, 0);
        gridBagConstraints.gridx = 0;

        gridBagConstraints.gridy = 0;
        stepTwoPanel.add(selectNormalFilePanel, gridBagConstraints);

        gridBagConstraints.gridy = 1;
        gridBagConstraints.weighty = 1;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        stepTwoPanel.add(selectNormalGridsPanel, gridBagConstraints);

        return stepTwoPanel;
    }

    private boolean validateStepTwo() {
        /* Popup Alert of Missing Inputs */
        if(normalFileTextField.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Input dataset is required.",
                    "Error: Missing Field", JOptionPane.ERROR_MESSAGE);

            return false;
        }

        /* Popup Alert of No Variables Selected */
        DefaultListModel<String> chosenGridsModel = getDefaultListModel(chosenNormalGridsList);
        if(chosenGridsModel == null || chosenGridsModel.isEmpty()) {
            JOptionPane.showMessageDialog(this, "At least one variable must be selected.",
                    "Error: No Variables Selected", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        return true;
    }

    private void submitStepTwo() {}

    private JPanel stepTwoNormalFilePanel() {
        /* selectNormalFileLabel */
        JLabel selectNormalFileLabel = new JLabel(TextProperties.getInstance().getProperty("NormalizerWiz_SelectNormalFile_L"));
        selectNormalFileLabel.setBorder(BorderFactory.createEmptyBorder(0,0,5,0));

        /* TextField and Browse Panel */
        JPanel normalFileTextFieldPanel = new JPanel();
        normalFileTextFieldPanel.setLayout(new BoxLayout(normalFileTextFieldPanel, BoxLayout.X_AXIS));

        normalFileTextField = new JTextField();
        normalFileTextField.setColumns(0);
        normalFileTextFieldPanel.add(normalFileTextField);

        normalFileTextFieldPanel.add(Box.createRigidArea(new Dimension(8,0)));

        FileBrowseButton normalFileBrowseButton = new FileBrowseButton(this.getClass().getName(), "");
        normalFileBrowseButton.setIcon(IconResources.loadIcon("images/Open16.gif"));
        normalFileBrowseButton.setPreferredSize(new Dimension(22,22));
        normalFileBrowseButton.addActionListener(evt -> normalFileBrowseAction(normalFileBrowseButton));
        normalFileTextFieldPanel.add(normalFileBrowseButton);

        /* Adding everything together */
        JPanel normalFilePanel = new JPanel(new BorderLayout());
        normalFilePanel.add(selectNormalFileLabel, BorderLayout.NORTH);
        normalFilePanel.add(normalFileTextFieldPanel, BorderLayout.CENTER);

        return normalFilePanel;
    }

    private JPanel stepTwoNormalGridsPanel() {
        /* selectNormalGridsLabel */
        JLabel selectNormalGridsLabel = new JLabel(TextProperties.getInstance().getProperty("NormalizerWiz_SelectNormalGrids_L"));
        selectNormalGridsLabel.setBorder(BorderFactory.createEmptyBorder(0,0,5,0));

        /* Available Normal Grids List */
        DefaultListModel<String> availableGridsListModel = new DefaultListModel<>();
        availableNormalGridsList = new JList<>(availableGridsListModel);
        availableNormalGridsList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(e.getClickCount() == 2) { stepTwoAddSelectedVariables(); }
            }
        });
        JScrollPane leftScrollPanel = new JScrollPane();
        leftScrollPanel.setViewportView(availableNormalGridsList);

        /* Chosen Source Grids List */
        DefaultListModel<String> chosenGridsListModel = new DefaultListModel<>();
        chosenNormalGridsList = new JList<>(chosenGridsListModel);
        chosenNormalGridsList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(e.getClickCount() == 2) { stepTwoRemoveSelectedVariables(); }
            }
        });
        JScrollPane rightScrollPanel = new JScrollPane();
        rightScrollPanel.setViewportView(chosenNormalGridsList);

        /* Transfer Buttons Panel */
        JPanel transferButtonsPanel = new JPanel();
        transferButtonsPanel.setLayout(new BoxLayout(transferButtonsPanel, BoxLayout.Y_AXIS));
        JButton addVariableButton = new JButton(IconResources.loadIcon("images/right-arrow-24.png"));
        addVariableButton.setPreferredSize(new Dimension(22,22));
        addVariableButton.setMaximumSize(new Dimension(22,22));
        addVariableButton.addActionListener(evt -> stepTwoAddSelectedVariables());
        JButton removeVariableButton = new JButton(IconResources.loadIcon("images/left-arrow-24.png"));
        removeVariableButton.setPreferredSize(new Dimension(22,22));
        removeVariableButton.setMaximumSize(new Dimension(22,22));
        removeVariableButton.addActionListener(evt -> stepTwoRemoveSelectedVariables());
        transferButtonsPanel.add(addVariableButton);
        transferButtonsPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        transferButtonsPanel.add(removeVariableButton);

        /* Adding grid lists and transfer buttons to normalGridsSelectionPanel */
        JPanel normalGridsSelectionPanel = new JPanel();
        normalGridsSelectionPanel.setLayout(new BoxLayout(normalGridsSelectionPanel, BoxLayout.X_AXIS));
        normalGridsSelectionPanel.add(leftScrollPanel);
        normalGridsSelectionPanel.add(Box.createRigidArea(new Dimension(4,0)));
        normalGridsSelectionPanel.add(transferButtonsPanel);
        normalGridsSelectionPanel.add(Box.createRigidArea(new Dimension(4,0)));
        normalGridsSelectionPanel.add(rightScrollPanel);

        /* Setting Preferred Sizes of Components */
        int mainContentWidth = normalGridsSelectionPanel.getPreferredSize().width;
        int scrollPanelWidth = mainContentWidth * 45 / 100;
        leftScrollPanel.setPreferredSize(new Dimension(scrollPanelWidth, 0));
        rightScrollPanel.setPreferredSize(new Dimension(scrollPanelWidth, 0));

        /* Adding everything together */
        JPanel normalGridsPanel = new JPanel(new BorderLayout());
        normalGridsPanel.add(selectNormalGridsLabel, BorderLayout.NORTH);
        normalGridsPanel.add(normalGridsSelectionPanel, BorderLayout.CENTER);

        return normalGridsPanel;
    }

    private void normalFileBrowseAction(FileBrowseButton fileBrowseButton) {
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
            normalFileTextField.setText(selectedFile.getAbsolutePath());
            fileBrowseButton.setPersistedBrowseLocation(selectedFile);

            /* Populate variables for available source grids list */
            Set<String> variables = DataReader.getVariables(selectedFile.toString());
            DefaultListModel<String> defaultListModel = getDefaultListModel(availableNormalGridsList);

            if(defaultListModel != null) {
                defaultListModel.clear();
                List<String> sortedDssVariables = sortDssVariables(new ArrayList<>(variables));
                defaultListModel.addAll(sortedDssVariables);
            }
        } // If: User selected OK -> Populate Available Normal Grids
    }

    private void stepTwoAddSelectedVariables() {
        List<String> selectedVariables = availableNormalGridsList.getSelectedValuesList();

        /* Adding to Right Variables List */
        DefaultListModel<String> defaultRightModel = getDefaultListModel(chosenNormalGridsList);
        if(defaultRightModel == null) { return; }
        List<String> rightVariablesList = Collections.list(defaultRightModel.elements());
        rightVariablesList.addAll(selectedVariables);
        defaultRightModel.clear();
        rightVariablesList = sortDssVariables(rightVariablesList);
        defaultRightModel.addAll(rightVariablesList);

        /* Removing from Left Variables List */
        DefaultListModel<String> defaultLeftModel = getDefaultListModel(availableNormalGridsList);
        if(defaultLeftModel == null) { return; }
        selectedVariables.forEach(defaultLeftModel::removeElement);
    }

    private void stepTwoRemoveSelectedVariables() {
        List<String> selectedVariables = chosenNormalGridsList.getSelectedValuesList();

        /* Adding to Left Variables List */
        DefaultListModel<String> defaultLeftModel = getDefaultListModel(availableNormalGridsList);
        if(defaultLeftModel == null) { return; }
        List<String> leftVariablesList = Collections.list(defaultLeftModel.elements());
        leftVariablesList.addAll(selectedVariables);
        defaultLeftModel.clear();
        leftVariablesList = sortDssVariables(leftVariablesList);
        defaultLeftModel.addAll(leftVariablesList);

        /* Removing from Left Variables List */
        DefaultListModel<String> defaultRightModel = getDefaultListModel(chosenNormalGridsList);
        if(defaultRightModel == null) { return; }
        selectedVariables.forEach(defaultRightModel::removeElement);
    }

    private JPanel stepThreePanel() {
        /* normalPeriodPanel and normalIntervalPanel*/
        JPanel normalPeriodPanel = stepThreeNormalPeriodPanel();
        JPanel normalIntervalPanel = stepThreeNormalIntervalPanel();

        /* Setting GridBagLayout for stepThreePanel */
        GridBagLayout gridBagLayout = new GridBagLayout();

        /* Adding Panels to stepThreePanel */
        JPanel stepThreePanel = new JPanel(gridBagLayout);
        stepThreePanel.setBorder(BorderFactory.createEmptyBorder(5,9,5,8));

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(0, 0, 5, 0);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.weightx = 1;
        gridBagConstraints.weighty = 0;

        gridBagConstraints.gridy = 0;
        stepThreePanel.add(normalPeriodPanel, gridBagConstraints);

        gridBagConstraints.gridy = 1;
        stepThreePanel.add(normalIntervalPanel, gridBagConstraints);

        gridBagConstraints.gridy = 2;
        gridBagConstraints.weighty = 1;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        stepThreePanel.add(new JPanel(), gridBagConstraints);

        return stepThreePanel;
    }

    private boolean validateStepThree() {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HHmm");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("M/d/yyyy");

        try {
            LocalTime startTime = LocalTime.parse(startTimeTextField.getText(), timeFormatter);
            LocalDate startD = LocalDate.parse(startDateTextField.getText(), dateFormatter);
            startDateTime = ZonedDateTime.of(LocalDateTime.of(startD, startTime), ZoneId.of("UTC"));
        } catch (DateTimeParseException e){
            JOptionPane.showMessageDialog(this, "Could not parse start date/time.",
                    "Error: Parse Exception", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        try {
            LocalTime endTime = LocalTime.parse(endTimeTextField.getText(), timeFormatter);
            LocalDate endD = LocalDate.parse(endDateTextField.getText(), dateFormatter);
            endDateTime = ZonedDateTime.of(LocalDateTime.of(endD, endTime), ZoneId.of("UTC"));
        } catch (DateTimeParseException e){
            JOptionPane.showMessageDialog(this, "Could not parse end date/time.",
                    "Error: Parse Exception", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        try {
            Integer.parseInt(normalIntervalTextField.getText());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Could not parse end interval.",
                    "Error: Parse Exception", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    private void submitStepThree() {}

    private JPanel stepThreeNormalPeriodPanel() {
        /* normalPeriodLabel */
        JLabel normalPeriodLabel = new JLabel(TextProperties.getInstance().getProperty("NormalizerWiz_SetNormalPeriod_L"));
        normalPeriodLabel.setBorder(BorderFactory.createEmptyBorder(0,0,5,0));

        /* normalPeriod - Start Panel */
        JLabel normalPeriodStartLabel = new JLabel(TextProperties.getInstance().getProperty("NormalizerWiz_NormalPeriodStart_L"));
        startDateTextField = new InputHintTextField(TextProperties.getInstance().getProperty("NormalizerWiz_NormalPeriodStartDate_Default"));
        startDateTextField.setColumns(16);
        startTimeTextField = new InputHintTextField(TextProperties.getInstance().getProperty("NormalizerWiz_NormalPeriodStartTime_Default"));
        startTimeTextField.setColumns(4);

        JPanel normalPeriodStartPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        normalPeriodStartPanel.add(normalPeriodStartLabel);
        normalPeriodStartPanel.add(Box.createRigidArea(new Dimension(15,0)));
        normalPeriodStartPanel.add(startDateTextField);
        normalPeriodStartPanel.add(startTimeTextField);

        /* normalPeriod - End Panel */
        JLabel normalPeriodEndLabel = new JLabel(TextProperties.getInstance().getProperty("NormalizerWiz_NormalPeriodEnd_L"));
        endDateTextField = new InputHintTextField(TextProperties.getInstance().getProperty("NormalizerWiz_NormalPeriodEndDate_Default"));
        endDateTextField.setColumns(16);
        endTimeTextField = new InputHintTextField(TextProperties.getInstance().getProperty("NormalizerWiz_NormalPeriodEndTime_Default"));
        endTimeTextField.setColumns(4);

        JPanel normalPeriodEndPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        normalPeriodEndPanel.add(normalPeriodEndLabel);
        normalPeriodEndPanel.add(Box.createRigidArea(new Dimension(21,0)));
        normalPeriodEndPanel.add(endDateTextField);
        normalPeriodEndPanel.add(endTimeTextField);

        /* Adding normalPeriod - Start & End together */
        JPanel normalPeriodStartEndPanel = new JPanel();
        normalPeriodStartEndPanel.setLayout(new BoxLayout(normalPeriodStartEndPanel, BoxLayout.Y_AXIS));
        normalPeriodStartEndPanel.add(normalPeriodStartPanel);
        normalPeriodStartEndPanel.add(normalPeriodEndPanel);
        normalPeriodStartEndPanel.setBorder(BorderFactory.createEmptyBorder(0, -5, 0, 0));

        /* Adding everything together */
        JPanel normalPeriodPanel = new JPanel(new BorderLayout());
        normalPeriodPanel.add(normalPeriodLabel, BorderLayout.NORTH);
        normalPeriodPanel.add(normalPeriodStartEndPanel, BorderLayout.CENTER);

        return normalPeriodPanel;
    }

    private JPanel stepThreeNormalIntervalPanel() {
        /* normalIntervalLabel */
        JLabel normalIntervalLabel = new JLabel(TextProperties.getInstance().getProperty("NormalizerWiz_SetNormalInterval_L"));
        normalIntervalLabel.setBorder(BorderFactory.createEmptyBorder(0,0,5,0));

        /* normalInterval - Interval Panel */
        JLabel normalIntervalIntervalLabel = new JLabel(TextProperties.getInstance().getProperty("NormalizerWiz_NormalIntervalInterval_L"));
        normalIntervalTextField = new InputHintTextField(TextProperties.getInstance().getProperty("NormalizerWiz_Interval_Default"));
        normalIntervalTextField.setColumns(10);

        JPanel normalIntervalIntervalPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        normalIntervalIntervalPanel.add(normalIntervalIntervalLabel);
        normalIntervalIntervalPanel.add(Box.createRigidArea(new Dimension(1,0)));
        normalIntervalIntervalPanel.add(normalIntervalTextField);

        /* normalInterval - Time Unit Panel */
        JLabel normalIntervalTimeUnitLabel = new JLabel(TextProperties.getInstance().getProperty("NormalizerWiz_NormalIntervalTimeUnitEnd_L"));

        Vector<String> timeUnitOptions = new Vector<>();
        timeUnitOptions.add(TextProperties.getInstance().getProperty("NormalizerWiz_TimeUnit_Days"));
        timeUnitOptions.add(TextProperties.getInstance().getProperty("NormalizerWiz_TimeUnit_Hours"));
        timeUnitOptions.add(TextProperties.getInstance().getProperty("NormalizerWiz_TimeUnit_Minutes"));
        timeUnitOptions.add(TextProperties.getInstance().getProperty("NormalizerWiz_TimeUnit_Seconds"));

        timeUnitComboBox = new JComboBox<>();
        timeUnitComboBox.setModel(new DefaultComboBoxModel<>(timeUnitOptions));
        timeUnitComboBox.setSelectedItem(TextProperties.getInstance().getProperty("NormalizerWiz_TimeUnit_Days"));

        JPanel normalIntervalTimeUnitPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        normalIntervalTimeUnitPanel.add(normalIntervalTimeUnitLabel);
        normalIntervalTimeUnitPanel.add(timeUnitComboBox);

        /* Adding normalInterval - Interval & Time Unit together */
        JPanel intervalTimeUnitPanel = new JPanel();
        intervalTimeUnitPanel.setLayout(new BoxLayout(intervalTimeUnitPanel, BoxLayout.Y_AXIS));
        intervalTimeUnitPanel.add(normalIntervalIntervalPanel);
        intervalTimeUnitPanel.add(normalIntervalTimeUnitPanel);
        intervalTimeUnitPanel.setBorder(BorderFactory.createEmptyBorder(0, -5, 0, 0));

        /* Adding everything together */
        JPanel normalIntervalPanel = new JPanel(new BorderLayout());
        normalIntervalPanel.add(normalIntervalLabel, BorderLayout.NORTH);
        normalIntervalPanel.add(intervalTimeUnitPanel, BorderLayout.CENTER);

        return normalIntervalPanel;
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
        gridBagConstraints.gridx = 0;

        /* Select Destination Section Panel */
        gridBagConstraints.gridy = 0;
        stepFourPanel.add(stepFourSelectDestinationPanel(), gridBagConstraints);

        /* DSS Parts Panel (Only appears when selected destination is a DSS file */
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new Insets(10,0,0,0);
        dssPartsSectionPanel = stepFourDssPartsSectionPanel();
        dssPartsSectionPanel.setVisible(false);
        stepFourPanel.add(dssPartsSectionPanel, gridBagConstraints);

        return stepFourPanel;
    }

    private JPanel stepFourSelectDestinationPanel() {
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

    private JPanel stepFourDssPartsSectionPanel() {
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

        return dssPartsPanel;
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
        }
    }

    private boolean validateStepFour() {
        if(selectDestinationTextField.getText() == null || selectDestinationTextField.getText().isEmpty() ) {
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
                normalizerTask();
                return null;
            }

            @Override
            protected void done() { normalizerEndUI(); }
        };

        task.execute();
    }

    private void normalizerTask() {
        String pathToSource = sourceFileTextField.getText();
        List<String> chosenSourceGrids = getItemsInList(chosenSourceGridsList);
        if(chosenSourceGrids == null) return;
        Set<String> sourceGrids = new HashSet<>(chosenSourceGrids);

        String pathToNormals = normalFileTextField.getText();
        List<String> chosenNormalGrids = getItemsInList(chosenNormalGridsList);
        if(chosenNormalGrids == null) return;
        Set<String> normalGrids = new HashSet<>(chosenNormalGrids);

        Object selectedTimeUnit = timeUnitComboBox.getSelectedItem();
        if(selectedTimeUnit == null) return;
        String intervalType = selectedTimeUnit.toString();
        String intervalAmount = normalIntervalTextField.getText();

        Duration interval;
        switch (intervalType) {
            case "Days":
                interval = Duration.ofDays(Integer.parseInt(intervalAmount));
                break;
            case "Hours":
                interval = Duration.ofHours(Integer.parseInt(intervalAmount));
                break;
            case "Seconds":
                interval = Duration.ofSeconds(Integer.parseInt(intervalAmount));
                break;
            default:
                interval = Duration.ofMinutes(Integer.parseInt(intervalAmount));
                break;
        }

        String destination = selectDestinationTextField.getText();

        /* Setting parts */
        List<String> chosenSourceList = getItemsInList(chosenSourceGridsList);
        if(chosenSourceList == null) return;
        Map<String, Set<String>> pathnameParts = DssUtil.getPathnameParts(chosenSourceList);
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

        List<Handler> handlers = handlersForNormalizer();

        Normalizer normalizer = Normalizer.builder()
                .startTime(startDateTime)
                .endTime(endDateTime)
                .interval(interval)
                .pathToSource(pathToSource)
                .sourceVariables(sourceGrids)
                .pathToNormals(pathToNormals)
                .normalsVariables(normalGrids)
                .destination(destination)
                .writeOptions(writeOptions)
                .handlers(handlers)
                .build();

        normalizer.addPropertyChangeListener(evt -> {
            if(evt.getPropertyName().equalsIgnoreCase("progress")) {
                if(!(evt.getNewValue() instanceof Integer)) return;
                int progressValue = (int) evt.getNewValue();
                progressBar.setIndeterminate(false);
                progressBar.setStringPainted(true);
                progressBar.setValue(progressValue);
                progressBar.setString(progressValue + "%");
            }
        });

        normalizer.normalize();
    }

    private List<Handler> handlersForNormalizer() {
        /* Handler to get Normalizer's Log Messages */
        SimpleFormatter formatter = new SimpleFormatter(){
            private static final String format = "%s %s %s\n";

            @Override
            public synchronized String format(LogRecord lr) {
                final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                        .withZone(ZoneId.systemDefault());

                return String.format(format,
                        formatter.format(Instant.now()),
                        lr.getLevel().getLocalizedName(),
                        lr.getMessage()
                );
            }
        };

        StreamHandler handler = new StreamHandler(new OutputStream() {
            @Override
            public void write(int b) {
                String s = String.valueOf((char)b);
            }
        }, formatter){

            // flush on each publish:
            @Override
            public void publish(LogRecord record) {
                super.publish(record);
                flush();
            }

        };
        List<Handler> handlers = new ArrayList<>();
        handlers.add(handler);

        return handlers;
    }

    private JPanel stepFivePanel() {
        JPanel stepFivePanel = new JPanel(new GridBagLayout());

        JPanel insidePanel = new JPanel();
        insidePanel.setLayout(new BoxLayout(insidePanel, BoxLayout.Y_AXIS));

        JLabel processingLabel = new JLabel(TextProperties.getInstance().getProperty("NormalizerWiz_Processing_L"));
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

    private boolean validateStepFive() { return true; }

    private void submitStepFive() {}

    private JPanel stepSixPanel() {
        JPanel stepSixPanel = new JPanel(new GridBagLayout());
        JLabel completeLabel = new JLabel(TextProperties.getInstance().getProperty("NormalizerWiz_Complete_L"));
        stepSixPanel.add(completeLabel);
        return stepSixPanel;
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

        NormalizerWizard normalizerWizard = new NormalizerWizard(null);
        normalizerWizard.buildAndShowUI();
    }
}