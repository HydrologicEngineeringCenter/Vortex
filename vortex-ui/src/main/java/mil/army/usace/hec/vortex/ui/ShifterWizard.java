package mil.army.usace.hec.vortex.ui;

import mil.army.usace.hec.vortex.math.ShiftTimeUnit;
import mil.army.usace.hec.vortex.math.TimeShiftMethod;
import mil.army.usace.hec.vortex.math.Shifter;
import mil.army.usace.hec.vortex.ui.util.FileSaveUtil;
import mil.army.usace.hec.vortex.util.DssUtil;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ShifterWizard extends VortexWizard {
    private final Frame frame;
    private SourceFileSelectionPanel sourceFileSelectionPanel;
    private DestinationSelectionPanel destinationSelectionPanel;

    private Container contentCards;
    private CardLayout cardLayout;
    private JButton backButton, nextButton, cancelButton;
    private int cardNumber;

    private JTextField sourceFileTextField;
    private JCheckBox startTimeCheckBox;
    private JCheckBox endTimeCheckBox;
    private JTextField intervalTextField;
    private ShiftTimeUnitComboBox shiftTimeUnitComboBox;
    private JList<String> chosenSourceGridsList;
    private JProgressBar progressBar;

    private static final Logger logger = Logger.getLogger(ShifterWizard.class.getName());

    public ShifterWizard(Frame frame) {
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
        this.setTitle(TextProperties.getInstance().getProperty("Time-ShifterWiz_Title"));
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
        backButton = new JButton(TextProperties.getInstance().getProperty("Time-ShifterWiz_Back"));
        backButton.setToolTipText(TextProperties.getInstance().getProperty("Time-ShifterWiz_Back_TT"));
        backButton.setEnabled(false);
        backButton.addActionListener(evt -> backAction());

        /* Next Button */
        nextButton = new JButton(TextProperties.getInstance().getProperty("Time-ShifterWiz_Next"));
        nextButton.setToolTipText(TextProperties.getInstance().getProperty("Time-ShifterWiz_Next_TT"));
        nextButton.addActionListener(evt -> {
            if(nextButton.getText().equals(TextProperties.getInstance().getProperty("Time-ShifterWiz_Restart"))) { restartAction(); }
            else if(nextButton.getText().equals(TextProperties.getInstance().getProperty("Time-ShifterWiz_Next"))) { nextAction(); }
        });

        /* Cancel Button */
        cancelButton = new JButton(TextProperties.getInstance().getProperty("Time-ShifterWiz_Cancel"));
        cancelButton.setToolTipText(TextProperties.getInstance().getProperty("Time-ShifterWiz_Cancel_TT"));
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
            nextButton.setText(TextProperties.getInstance().getProperty("Time-ShifterWiz_Restart"));
            nextButton.setToolTipText(TextProperties.getInstance().getProperty("Time-ShifterWiz_Restart_TT"));
            nextButton.setEnabled(true);
            cancelButton.setText(TextProperties.getInstance().getProperty("Time-ShifterWiz_Close"));
            cancelButton.setToolTipText(TextProperties.getInstance().getProperty("Time-ShifterWiz_Close_TT"));
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
        nextButton.setText(TextProperties.getInstance().getProperty("Time-ShifterWiz_Next"));
        nextButton.setToolTipText(TextProperties.getInstance().getProperty("Time-ShifterWiz_Next_TT"));

        cancelButton.setText(TextProperties.getInstance().getProperty("Time-ShifterWiz_Cancel"));
        cancelButton.setToolTipText(TextProperties.getInstance().getProperty("Time-ShifterWiz_Cancel_TT"));

        /* Clearing Step One Panel */
        sourceFileSelectionPanel.clear();

        /* Clearing Step Two Panel */
        intervalTextField.setText("");

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
        sourceFileSelectionPanel = new SourceFileSelectionPanel(ShifterWizard.class.getName());
        sourceFileTextField = sourceFileSelectionPanel.getSourceFileTextField();
        chosenSourceGridsList = sourceFileSelectionPanel.getChosenSourceGridsList();
        return sourceFileSelectionPanel;
    }

    private boolean validateStepOne() {
        return sourceFileSelectionPanel.validateInput();
    }

    private void submitStepOne() {}

    private JPanel stepTwoPanel() {
        /* intervalPanel */
        JPanel setIntervalPanel = stepTwoIntervalPanel();

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
        stepTwoPanel.add(setIntervalPanel, gridBagConstraints);

        gridBagConstraints.gridy = 1;
        gridBagConstraints.weighty = 1;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        stepTwoPanel.add(new JPanel(), gridBagConstraints);

        return stepTwoPanel;
    }

    private boolean validateStepTwo() {
        /* Check that start time, end time, or both are selected */
        if (!startTimeCheckBox.isSelected() && !endTimeCheckBox.isSelected()) {
            String message = "Start time, end time, or both start time and end time must be selected.";
            JOptionPane.showMessageDialog(this, message,
                    "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        /* Check for at least one entry */
        String intervalText = intervalTextField.getText();
        if (intervalText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Shift value required.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        double interval;
        try {
            interval = Double.parseDouble(intervalText);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Could not parse interval.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        ShiftTimeUnit timeUnit = shiftTimeUnitComboBox.getSelected();
        if (!validateInterval(interval, timeUnit)) {
            String message = "Specified interval is cannot be converted to an even number of seconds.";
            JOptionPane.showMessageDialog(this, message,
                    "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        return true;
    }

    private void submitStepTwo() {}

    private JPanel stepTwoIntervalPanel() {
        JLabel shiftMethodLabel = new JLabel(TextProperties.getInstance().getProperty("Time-ShifterWiz_ShiftMethod_L"));
        JPanel shiftMethodPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        shiftMethodPanel.add(shiftMethodLabel);

        startTimeCheckBox = new JCheckBox(TextProperties.getInstance().getProperty("ShifterWiz_ShiftStart_L"));
        endTimeCheckBox = new JCheckBox(TextProperties.getInstance().getProperty("ShifterWiz_EndStart_L"));
        JPanel startTimeCheckBoxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        startTimeCheckBoxPanel.add(startTimeCheckBox);

        startTimeCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        endTimeCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel endTimeCheckBoxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        endTimeCheckBoxPanel.add(endTimeCheckBox);

        startTimeCheckBox.setSelected(true);
        endTimeCheckBox.setSelected(true);

        /* Interval Panel */
        JLabel shiftLabel = new JLabel(TextProperties.getInstance().getProperty("ShifterWiz_TimeShift_L"));
        intervalTextField = new JTextField(25);
        shiftTimeUnitComboBox = new ShiftTimeUnitComboBox();
        shiftTimeUnitComboBox.setEnabled(true);

        JPanel setIntervalPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        setIntervalPanel.add(shiftLabel);
        setIntervalPanel.add(Box.createRigidArea(new Dimension(2,0)));
        setIntervalPanel.add(intervalTextField);
        setIntervalPanel.add(Box.createRigidArea(new Dimension(2,0)));
        setIntervalPanel.add(shiftTimeUnitComboBox);

        /* Adding everything together */
        JPanel intervalPanel = new JPanel();
        intervalPanel.setLayout(new BoxLayout(intervalPanel, BoxLayout.Y_AXIS));
        intervalPanel.add(shiftMethodPanel);
        intervalPanel.add(startTimeCheckBoxPanel);
        intervalPanel.add(endTimeCheckBoxPanel);
        intervalPanel.add(setIntervalPanel);

        return intervalPanel;
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
                timeShifterTask();
                return null;
            }

            @Override
            protected void done() {
                nextAction();
            }
        };

        task.execute();
    }

    private void timeShifterTask() {
        String pathToSource = sourceFileTextField.getText();
        List<String> chosenSourceGrids = getItemsInList(chosenSourceGridsList);
        if (chosenSourceGrids == null) return;
        Set<String> sourceGrids = new HashSet<>(chosenSourceGrids);

        Set<TimeShiftMethod> methods = new HashSet<>();
        if (startTimeCheckBox.isSelected()) methods.add(TimeShiftMethod.START);
        if (endTimeCheckBox.isSelected()) methods.add(TimeShiftMethod.END);

        String intervalText = intervalTextField.getText();
        double value = Float.parseFloat(intervalText);

        ShiftTimeUnit timeUnit = shiftTimeUnitComboBox.getSelected();
        int toSeconds = timeUnit.toSeconds();

        // Cast is validated on step 2 of the wizard
        long seconds = (long) value * toSeconds;
        
        Duration interval = Duration.ofSeconds(seconds);

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

        Shifter shift = Shifter.builder()
                .pathToFile(pathToSource)
                .grids(sourceGrids)
                .methods(methods)
                .shift(interval)
                .destination(destinationSelectionPanel.getDestinationTextField().getText())
                .writeOptions(writeOptions)
                .build();

        shift.addPropertyChangeListener(evt -> {
            if (evt.getPropertyName().equalsIgnoreCase("progress")) {
                if (!(evt.getNewValue() instanceof Integer)) return;
                int progressValue = (int) evt.getNewValue();
                progressBar.setIndeterminate(false);
                progressBar.setStringPainted(true);
                progressBar.setValue(progressValue);
                progressBar.setString(progressValue + "%");
            }
        });

        shift.shift();
    }

    private JPanel stepFourPanel() {
        JPanel stepFourPanel = new JPanel(new GridBagLayout());

        JPanel insidePanel = new JPanel();
        insidePanel.setLayout(new BoxLayout(insidePanel, BoxLayout.Y_AXIS));

        JLabel processingLabel = new JLabel(TextProperties.getInstance().getProperty("Time-ShifterWiz_Processing_L"));
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
        JLabel completeLabel = new JLabel(TextProperties.getInstance().getProperty("Time-ShifterWiz_Complete_L"));
        stepFivePanel.add(completeLabel);
        return stepFivePanel;
    }

    private List<String> getItemsInList(JList<String> list) {
        DefaultListModel<String> defaultRightModel = Util.getDefaultListModel(list);
        if(defaultRightModel == null) { return null; }
        return Collections.list(defaultRightModel.elements());
    }

    private void closeAction() {
        ShifterWizard.this.setVisible(false);
        ShifterWizard.this.dispose();
        String savedFile = destinationSelectionPanel.getDestinationTextField().getText();
        FileSaveUtil.showFileLocation(ShifterWizard.this, Path.of(savedFile));
    }

    private boolean validateInterval(double value, ShiftTimeUnit timeUnit) {
        int toSeconds = timeUnit.toSeconds();
        double doubleValue = value * toSeconds;
        long longValue = (long) doubleValue;

        // Check if there is any remainder after casting
        return Double.compare(longValue, doubleValue) == 0;
    }

    /* Add main for quick UI Testing */
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) { e.printStackTrace(); }

        ShifterWizard sanitizerWizard = new ShifterWizard(null);
        sanitizerWizard.buildAndShowUI();
    }
}