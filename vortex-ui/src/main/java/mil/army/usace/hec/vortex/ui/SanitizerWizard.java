package mil.army.usace.hec.vortex.ui;

import mil.army.usace.hec.vortex.VortexProperty;
import mil.army.usace.hec.vortex.math.BatchSanitizer;
import mil.army.usace.hec.vortex.ui.util.FileSaveUtil;
import mil.army.usace.hec.vortex.util.DssUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SanitizerWizard extends VortexWizard {
    private static final Logger logger = Logger.getLogger(SanitizerWizard.class.getName());

    private final Frame frame;
    private SourceFileSelectionPanel sourceFileSelectionPanel;
    private DestinationSelectionPanel destinationSelectionPanel;

    private Container contentCards;
    private CardLayout cardLayout;
    private JButton backButton, nextButton, cancelButton;
    private int cardNumber;

    private JTextField sourceFileTextField;
    private JCheckBox lowerThresholdCheckBox;
    private JCheckBox upperThresholdCheckBox;
    private JTextField lowerThresholdTextField;
    private JTextField lowerReplacementTextField;
    private JTextField upperThresholdTextField;
    private JTextField upperReplacementTextField;
    private JList<String> chosenSourceGridsList;

    private final ProgressMessagePanel progressMessagePanel = new ProgressMessagePanel();

    public SanitizerWizard(Frame frame) {
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
        this.setTitle(TextProperties.getInstance().getProperty("SanitizerWiz_Title"));
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
    }

    private void initializeButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        /* Back Button */
        backButton = new JButton(TextProperties.getInstance().getProperty("SanitizerWiz_Back"));
        backButton.setToolTipText(TextProperties.getInstance().getProperty("SanitizerWiz_Back_TT"));
        backButton.setEnabled(false);
        backButton.addActionListener(evt -> backAction());

        /* Next Button */
        nextButton = new JButton(TextProperties.getInstance().getProperty("SanitizerWiz_Next"));
        nextButton.setToolTipText(TextProperties.getInstance().getProperty("SanitizerWiz_Next_TT"));
        nextButton.addActionListener(evt -> {
            if(nextButton.getText().equals(TextProperties.getInstance().getProperty("SanitizerWiz_Restart"))) { restartAction(); }
            else if(nextButton.getText().equals(TextProperties.getInstance().getProperty("SanitizerWiz_Next"))) { nextAction(); }
        });

        /* Cancel Button */
        cancelButton = new JButton(TextProperties.getInstance().getProperty("SanitizerWiz_Cancel"));
        cancelButton.setToolTipText(TextProperties.getInstance().getProperty("SanitizerWiz_Cancel_TT"));
        cancelButton.addActionListener(evt -> closeAction());

        /* Adding Buttons to NavigationPanel */
        buttonPanel.add(backButton);
        buttonPanel.add(nextButton);
        buttonPanel.add(cancelButton);

        /* Add buttonPanel to SanitizerWizard */
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
            nextButton.setText(TextProperties.getInstance().getProperty("SanitizerWiz_Restart"));
            nextButton.setToolTipText(TextProperties.getInstance().getProperty("SanitizerWiz_Restart_TT"));
            nextButton.setEnabled(true);
            cancelButton.setText(TextProperties.getInstance().getProperty("SanitizerWiz_Close"));
            cancelButton.setToolTipText(TextProperties.getInstance().getProperty("SanitizerWiz_Close_TT"));
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
        nextButton.setText(TextProperties.getInstance().getProperty("SanitizerWiz_Next"));
        nextButton.setToolTipText(TextProperties.getInstance().getProperty("SanitizerWiz_Next_TT"));

        cancelButton.setText(TextProperties.getInstance().getProperty("SanitizerWiz_Cancel"));
        cancelButton.setToolTipText(TextProperties.getInstance().getProperty("SanitizerWiz_Cancel_TT"));

        /* Clearing Step One Panel */
        sourceFileSelectionPanel.clear();

        /* Clearing Step Two Panel */
        lowerThresholdTextField.setText("");
        lowerReplacementTextField.setText("");
        upperThresholdTextField.setText("");
        upperReplacementTextField.setText("");

        /* Clearing Step Three Panel */
        destinationSelectionPanel.getDestinationTextField().setText("");
        destinationSelectionPanel.getFieldA().setText("");
        destinationSelectionPanel.getFieldB().setText("");
        destinationSelectionPanel.getFieldF().setText("");

        /* Clearing Step Four Panel */
        progressMessagePanel.clear();
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
        sourceFileSelectionPanel = new SourceFileSelectionPanel(CalculatorWizard.class.getName());
        sourceFileTextField = sourceFileSelectionPanel.getSourceFileTextField();
        chosenSourceGridsList = sourceFileSelectionPanel.getChosenSourceGridsList();
        return sourceFileSelectionPanel;
    }

    private boolean validateStepOne() {
        return sourceFileSelectionPanel.validateInput();
    }

    private void submitStepOne() {}

    private JPanel stepTwoPanel() {
        /* selectSanitizeFilePanel and selectSanitizeGridsPanel*/
        JPanel selectBelowThresholdPanel = stepTwoSanitizeValuesBelowPanel();
        JPanel selectAboveThresholdPanel = stepTwoSanitizeValuesAbovePanel();

        /* Setting GridBagLayout for stepTwoPanel */
        GridBagLayout gridBagLayout = new GridBagLayout();

        /* Adding Panels to stepTwoPanel */
        JPanel stepTwoPanel = new JPanel(gridBagLayout);
        stepTwoPanel.setBorder(BorderFactory.createEmptyBorder(5,9,5,8));

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(0, 0, 5, 0);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.weightx = 1;
        gridBagConstraints.weighty = 0;

        gridBagConstraints.gridy = 0;
        stepTwoPanel.add(selectBelowThresholdPanel, gridBagConstraints);

        gridBagConstraints.gridy = 1;
        stepTwoPanel.add(selectAboveThresholdPanel, gridBagConstraints);

        gridBagConstraints.gridy = 2;
        gridBagConstraints.weighty = 1;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        stepTwoPanel.add(new JPanel(), gridBagConstraints);

        return stepTwoPanel;
    }

    private boolean validateStepTwo() {
        //Is the lower threshold box checked
        if (lowerThresholdCheckBox.isSelected()) {
            try {
                Double.parseDouble(lowerThresholdTextField.getText ());
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Could not parse lower threshold value.",
                       "Error: Parse Exception", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            try {
                Double.parseDouble(lowerReplacementTextField.getText());
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Could not parse lower replacement value.",
                        "Error: Parse Exception", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }

        //Is the upper threshold box checked
        if (upperThresholdCheckBox.isSelected()) {
            try {
                Double.parseDouble(upperThresholdTextField.getText());
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Could not parse upper threshold value.",
                        "Error: Parse Exception", JOptionPane.ERROR_MESSAGE);
                return false;
            }

            try {
                Double.parseDouble(upperReplacementTextField.getText());
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Could not parse upper replacement value.",
                        "Error: Parse Exception", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        return true;
    }

    private void submitStepTwo() {}

    private JPanel stepTwoSanitizeValuesBelowPanel() {
        // create valuesBelowThresholdCheckbox
        lowerThresholdCheckBox = new JCheckBox(TextProperties.getInstance().getProperty("SanitizerWiz_LowerThresholdCheckbox_L"),false);

        /* create Replace values panel */
        JLabel replaceLowerValuesLabel = new JLabel(TextProperties.getInstance().getProperty("SanitizerWiz_LowerThreshold_L"));
        replaceLowerValuesLabel.setBorder(new EmptyBorder(0,15,0,0));
        lowerThresholdTextField = new JTextField(16);

        JPanel replaceLowerValuesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        replaceLowerValuesPanel.add(replaceLowerValuesLabel);
        replaceLowerValuesPanel.add(Box.createRigidArea(new Dimension(15,0)));
        replaceLowerValuesPanel.add(lowerThresholdTextField);

        /* create Replacement value panel */
        JLabel replacementValueLabel = new JLabel(TextProperties.getInstance().getProperty("SanitizerWiz_LowerReplacement_L"));
        replacementValueLabel.setBorder(new EmptyBorder(0,15,0,0));
        lowerReplacementTextField = new JTextField(16);

        JPanel replacementLowerValuePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        replacementLowerValuePanel.add(replacementValueLabel);
        replacementLowerValuePanel.add(Box.createRigidArea(new Dimension(68,0)));
        replacementLowerValuePanel.add(lowerReplacementTextField);

        /* Adding text boxes together */
        JPanel lowerTextFieldsPanel = new JPanel();
        lowerTextFieldsPanel.setLayout(new BoxLayout(lowerTextFieldsPanel, BoxLayout.Y_AXIS));
        lowerTextFieldsPanel.add(replaceLowerValuesPanel);
        lowerTextFieldsPanel.add(replacementLowerValuePanel);
        lowerTextFieldsPanel.setBorder(BorderFactory.createEmptyBorder(0, -5, 0, 0));
        lowerTextFieldsPanel.setVisible(false);

        /* Getting the status of the checkbox */
        lowerThresholdCheckBox.addActionListener(e -> lowerTextFieldsPanel.setVisible(lowerThresholdCheckBox.isSelected()));

        /* Adding everything together */
        JPanel valuesLowerPanel = new JPanel(new BorderLayout());
        valuesLowerPanel.add(lowerThresholdCheckBox, BorderLayout.NORTH);
        valuesLowerPanel.add(lowerTextFieldsPanel, BorderLayout.CENTER);

        return valuesLowerPanel;
    }

    private JPanel stepTwoSanitizeValuesAbovePanel() {
        // create valuesAboveThresholdCheckbox
        upperThresholdCheckBox = new JCheckBox(TextProperties.getInstance().getProperty("SanitizerWiz_UpperThresholdCheckbox_L"));

        /* create Replace values panel */
        JLabel replaceUpperValuesLabel = new JLabel(TextProperties.getInstance().getProperty("SanitizerWiz_UpperThreshold_L"));
        replaceUpperValuesLabel.setBorder(new EmptyBorder(0,15,0,0));
        upperThresholdTextField = new JTextField(16);

        JPanel replaceUpperValuesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        replaceUpperValuesPanel.add(replaceUpperValuesLabel);
        replaceUpperValuesPanel.add(Box.createRigidArea(new Dimension(15,0)));
        replaceUpperValuesPanel.add(upperThresholdTextField);

        /* create Replacement value panel */
        JLabel replacementValueLabel = new JLabel(TextProperties.getInstance().getProperty("SanitizerWiz_UpperReplacement_L"));
        replacementValueLabel.setBorder(new EmptyBorder(0,15,0,0));
        upperReplacementTextField = new JTextField(16);

        JPanel replacementUpperValuePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        replacementUpperValuePanel.add(replacementValueLabel);
        replacementUpperValuePanel.add(Box.createRigidArea(new Dimension(86,0)));
        replacementUpperValuePanel.add(upperReplacementTextField);

        /* Adding text boxes together */
        JPanel upperTextFieldsPanel = new JPanel();
        upperTextFieldsPanel.setLayout(new BoxLayout(upperTextFieldsPanel, BoxLayout.Y_AXIS));
        upperTextFieldsPanel.add(replaceUpperValuesPanel);
        upperTextFieldsPanel.add(replacementUpperValuePanel);
        upperTextFieldsPanel.setBorder(BorderFactory.createEmptyBorder(0, -5, 0, 0));
        upperTextFieldsPanel.setVisible(false);

        /* Getting the status of the checkbox */
        upperThresholdCheckBox.addActionListener(e -> upperTextFieldsPanel.setVisible(upperThresholdCheckBox.isSelected()));

        /* Adding everything together */
        JPanel valuesUpperPanel = new JPanel(new BorderLayout());
        valuesUpperPanel.add(upperThresholdCheckBox, BorderLayout.NORTH);
        valuesUpperPanel.add(upperTextFieldsPanel, BorderLayout.CENTER);

        return valuesUpperPanel;
    }

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
                sanitizerTask();
                return null;
            }

            @Override
            protected void done() {
                nextAction();
            }
        };

        task.execute();
    }

    private void sanitizerTask() {
        String pathToSource = sourceFileTextField.getText();
        List<String> chosenSourceGrids = getItemsInList(chosenSourceGridsList);
        if (chosenSourceGrids == null) return;
        Set<String> sourceGrids = new HashSet<>(chosenSourceGrids);

        double minimumThreshold;
        float minimumReplacementValue;
        if (lowerThresholdCheckBox.isSelected()) {
            minimumThreshold = Double.parseDouble(lowerThresholdTextField.getText());
            minimumReplacementValue = Float.parseFloat(lowerReplacementTextField.getText());
            if (lowerThresholdTextField == null || lowerReplacementTextField == null) return;
        } else {
            minimumThreshold = Double.NaN;
            minimumReplacementValue = Float.NaN;
        }

        double maximumThreshold;
        float maximumReplacementValue;
        if (upperThresholdCheckBox.isSelected()) {
            maximumThreshold = Double.parseDouble(upperThresholdTextField.getText());
            maximumReplacementValue = Float.parseFloat(upperReplacementTextField.getText());
            if (upperThresholdTextField == null || upperReplacementTextField == null) return;
        } else {
            maximumThreshold = Double.NaN;
            maximumReplacementValue = Float.NaN;
        }

        String destination = destinationSelectionPanel.getDestinationTextField().getText();

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
        String partC = (partCList.size() == 1) ? partCList.get(0) : "PRECIPITATION";
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

        String unitsString = destinationSelectionPanel.getUnitsString();
        if (!unitsString.isEmpty())
            writeOptions.put("units", unitsString);

        String dataType = destinationSelectionPanel.getDataType();
        if (dataType != null && !dataType.isEmpty())
            writeOptions.put("dataType", dataType);

        if (lowerThresholdCheckBox.isSelected()) {
            writeOptions.put("minThreshold", lowerThresholdTextField.getText());
            writeOptions.put("minDataValue", lowerReplacementTextField.getText());
        }

        if (upperThresholdCheckBox.isSelected()) {
            writeOptions.put("maxThreshold", upperThresholdTextField.getText());
            writeOptions.put("maxDataValue", upperReplacementTextField.getText());
        }

        BatchSanitizer batchSanitizer = BatchSanitizer.builder()
                .pathToInput(pathToSource)
                .variables(new ArrayList<>(sourceGrids))
                .minimumThreshold(minimumThreshold)
                .minimumReplacementValue(minimumReplacementValue)
                .maximumThreshold(maximumThreshold)
                .maximumReplacementValue(maximumReplacementValue)
                .destination(destinationSelectionPanel.getDestinationTextField().getText())
                .writeOptions(writeOptions)
                .build();

        batchSanitizer.addPropertyChangeListener(evt -> {
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
                batchSanitizer.run();
                return null;
            }
        };

        task.execute();
    }

    private JPanel stepFourPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(progressMessagePanel, BorderLayout.CENTER);
        return panel;
    }

    private boolean validateStepFour() { return true; }

    private void submitStepFour() {}

    private JPanel stepFivePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(progressMessagePanel, BorderLayout.CENTER);
        return panel;
    }

    private List<String> getItemsInList(JList<String> list) {
        DefaultListModel<String> defaultRightModel = Util.getDefaultListModel(list);
        if(defaultRightModel == null) { return null; }
        return Collections.list(defaultRightModel.elements());
    }

    private void closeAction() {
        SanitizerWizard.this.setVisible(false);
        SanitizerWizard.this.dispose();
        String savedFile = destinationSelectionPanel.getDestinationTextField().getText();
        FileSaveUtil.showFileLocation(SanitizerWizard.this, Path.of(savedFile));
    }

    /* Add main for quick UI Testing */
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) { e.printStackTrace(); }

        SanitizerWizard sanitizerWizard = new SanitizerWizard(null);
        sanitizerWizard.buildAndShowUI();
    }
}