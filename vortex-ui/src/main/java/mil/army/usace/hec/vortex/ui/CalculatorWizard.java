package mil.army.usace.hec.vortex.ui;

import mil.army.usace.hec.vortex.math.BatchCalculator;
import mil.army.usace.hec.vortex.util.DssUtil;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CalculatorWizard extends JFrame {
    private final Frame frame;
    private SourceFileSelectionPanel sourceFileSelectionPanel;
    private DestinationSelectionPanel destinationSelectionPanel;

    private Container contentCards;
    private CardLayout cardLayout;
    private JButton backButton, nextButton, cancelButton;
    private int cardNumber;

    private JTextField sourceFileTextField;
    private JTextField multiplyTextField;
    private JTextField divideTextField;
    private JTextField addTextField;
    private JTextField subtractTextField;
    private JList<String> chosenSourceGridsList;
    private JProgressBar progressBar;

    private static final Logger logger = Logger.getLogger(CalculatorWizard.class.getName());

    public CalculatorWizard(Frame frame) {
        this.frame = frame;
        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                CalculatorWizard.this.setVisible(false);
                CalculatorWizard.this.dispose();
            }
        });
    }

    public void buildAndShowUI() {
        /* Setting Wizard's names and layout */
        this.setTitle(TextProperties.getInstance().getProperty("CalculatorWiz_Title"));
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
        backButton = new JButton(TextProperties.getInstance().getProperty("CalculatorWiz_Back"));
        backButton.setToolTipText(TextProperties.getInstance().getProperty("CalculatorWiz_Back_TT"));
        backButton.setEnabled(false);
        backButton.addActionListener(evt -> backAction());

        /* Next Button */
        nextButton = new JButton(TextProperties.getInstance().getProperty("CalculatorWiz_Next"));
        nextButton.setToolTipText(TextProperties.getInstance().getProperty("CalculatorWiz_Next_TT"));
        nextButton.addActionListener(evt -> {
            if(nextButton.getText().equals(TextProperties.getInstance().getProperty("CalculatorWiz_Restart"))) { restartAction(); }
            else if(nextButton.getText().equals(TextProperties.getInstance().getProperty("CalculatorWiz_Next"))) { nextAction(); }
        });

        /* Cancel Button */
        cancelButton = new JButton(TextProperties.getInstance().getProperty("CalculatorWiz_Cancel"));
        cancelButton.setToolTipText(TextProperties.getInstance().getProperty("CalculatorWiz_Cancel_TT"));
        cancelButton.addActionListener(evt -> {
            this.setVisible(false);
            this.dispose();
        });

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
            nextButton.setText(TextProperties.getInstance().getProperty("CalculatorWiz_Restart"));
            nextButton.setToolTipText(TextProperties.getInstance().getProperty("CalculatorWiz_Restart_TT"));
            nextButton.setEnabled(true);
            cancelButton.setText(TextProperties.getInstance().getProperty("CalculatorWiz_Close"));
            cancelButton.setToolTipText(TextProperties.getInstance().getProperty("CalculatorWiz_Close_TT"));
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
        nextButton.setText(TextProperties.getInstance().getProperty("CalculatorWiz_Next"));
        nextButton.setToolTipText(TextProperties.getInstance().getProperty("CalculatorWiz_Next_TT"));

        cancelButton.setText(TextProperties.getInstance().getProperty("CalculatorWiz_Cancel"));
        cancelButton.setToolTipText(TextProperties.getInstance().getProperty("CalculatorWiz_Cancel_TT"));

        /* Clearing Step One Panel */
        sourceFileSelectionPanel.clear();

        /* Clearing Step Two Panel */
        multiplyTextField.setText("");
        divideTextField.setText("");
        addTextField.setText("");
        subtractTextField.setText("");

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
        /* constantPanel */
        JPanel setConstantPanel = stepTwoConstantPanel();

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
        stepTwoPanel.add(setConstantPanel, gridBagConstraints);

        gridBagConstraints.gridy = 1;
        gridBagConstraints.weighty = 1;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        stepTwoPanel.add(new JPanel(), gridBagConstraints);

        return stepTwoPanel;
    }

    private boolean validateStepTwo() {
        int count = 0;
        String multiplyText = multiplyTextField.getText();
        String divideText = divideTextField.getText();
        String addText = addTextField.getText();
        String subtractText = subtractTextField.getText();

        /* Check for at least one entry */
        if (multiplyText.isEmpty() && divideText.isEmpty() && addText.isEmpty() && subtractText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "One constant value entry required",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        /* Check for multiple entries */
        if (!multiplyText.isEmpty())
            count++;
        if (!divideText.isEmpty())
            count++;
        if (!addText.isEmpty())
            count++;
        if(!subtractText.isEmpty())
            count++;
        if (count != 1) {
            JOptionPane.showMessageDialog(this, "Only one constant value entry allowed",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    private void submitStepTwo() {}

    private JPanel stepTwoConstantPanel() {
        /* constantLabel */
        JLabel setConstantLabel = new JLabel(TextProperties.getInstance().getProperty("CalculatorWiz_SetConstant_L"));
        setConstantLabel.setBorder(BorderFactory.createEmptyBorder(0,0,5,0));

        /* Multiply Panel */
        JLabel multiplyLabel = new JLabel(TextProperties.getInstance().getProperty("CalculatorWiz_Multiply_L"));
        multiplyTextField = new JTextField(25);

        JPanel multiplyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        multiplyPanel.add(multiplyLabel);
        multiplyPanel.add(Box.createRigidArea(new Dimension(75,0)));
        multiplyPanel.add(multiplyTextField);

        /* Divide Panel */
        JLabel divideLabel = new JLabel(TextProperties.getInstance().getProperty("CalculatorWiz_Divide_L"));
        divideTextField = new JTextField(25);

        JPanel dividePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        dividePanel.add(divideLabel);
        dividePanel.add(Box.createRigidArea(new Dimension(82,0)));
        dividePanel.add(divideTextField);

        /* Add Panel */
        JLabel addLabel = new JLabel(TextProperties.getInstance().getProperty("CalculatorWiz_Add_L"));
        addTextField = new JTextField(25);

        JPanel addPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addPanel.add(addLabel);
        addPanel.add(Box.createRigidArea(new Dimension(92,0)));
        addPanel.add(addTextField);

        /* subtract Panel */
        JLabel subtractLabel = new JLabel(TextProperties.getInstance().getProperty("CalculatorWiz_Subtract_L"));
        subtractTextField = new JTextField(25);

        JPanel subtractPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        subtractPanel.add(subtractLabel);
        subtractPanel.add(Box.createRigidArea(new Dimension(70,0)));
        subtractPanel.add(subtractTextField);

        /* Adding constant panels together */
        JPanel setConstantPanel = new JPanel();
        setConstantPanel.setLayout(new BoxLayout(setConstantPanel, BoxLayout.Y_AXIS));
        setConstantPanel.add(multiplyPanel);
        setConstantPanel.add(dividePanel);
        setConstantPanel.add(addPanel);
        setConstantPanel.add(subtractPanel);
        setConstantPanel.setBorder(BorderFactory.createEmptyBorder(0, -5, 0, 0));

        /* Adding everything together */
        JPanel constantPanel = new JPanel(new BorderLayout());
        constantPanel.add(setConstantLabel, BorderLayout.NORTH);
        constantPanel.add(setConstantPanel, BorderLayout.CENTER);

        return constantPanel;
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
                calculatorTask();
                return null;
            }

            @Override
            protected void done() {
                nextAction();
            }
        };

        task.execute();
    }

    private void calculatorTask() {
        String pathToSource = sourceFileTextField.getText();
        List<String> chosenSourceGrids = getItemsInList(chosenSourceGridsList);
        if (chosenSourceGrids == null) return;
        Set<String> sourceGrids = new HashSet<>(chosenSourceGrids);

        String multiplyText = multiplyTextField.getText();
        String divideText = divideTextField.getText();
        String addText = addTextField.getText();
        String subtractText = subtractTextField.getText();

        float multiplyValue;
        if (multiplyText != null && !multiplyText.isEmpty()) {
            multiplyValue = Integer.parseInt(multiplyTextField.getText());
        } else {
            multiplyValue = Float.NaN;
        }

        float divideValue;
        if (divideText != null && !divideText.isEmpty()) {
            divideValue = Integer.parseInt(divideTextField.getText());
        } else {
            divideValue = Float.NaN;
        }

        float addValue;
        if (addText != null && !addText.isEmpty()) {
            addValue = Integer.parseInt(addTextField.getText());
        } else {
            addValue = Float.NaN;
        }

        float subtractValue;
        if (subtractText != null && !subtractText.isEmpty()) {
            subtractValue = Integer.parseInt(subtractTextField.getText());
        } else {
            subtractValue = Float.NaN;
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

        BatchCalculator batchCalculator = BatchCalculator.builder()
                .pathToInput(pathToSource)
                .variables(new ArrayList<>(sourceGrids))
                .multiplyValue(multiplyValue)
                .divideValue(divideValue)
                .addValue(addValue)
                .subtractValue(subtractValue)
                .destination(destinationSelectionPanel.getDestinationTextField().getText())
                .writeOptions(writeOptions)
                .build();

        batchCalculator.addPropertyChangeListener(evt -> {
            if (evt.getPropertyName().equalsIgnoreCase("progress")) {
                if (!(evt.getNewValue() instanceof Integer)) return;
                int progressValue = (int) evt.getNewValue();
                progressBar.setIndeterminate(false);
                progressBar.setStringPainted(true);
                progressBar.setValue(progressValue);
                progressBar.setString(progressValue + "%");
            }
        });

        batchCalculator.process();
    }

    private JPanel stepFourPanel() {
        JPanel stepFourPanel = new JPanel(new GridBagLayout());

        JPanel insidePanel = new JPanel();
        insidePanel.setLayout(new BoxLayout(insidePanel, BoxLayout.Y_AXIS));

        JLabel processingLabel = new JLabel(TextProperties.getInstance().getProperty("CalculatorWiz_Processing_L"));
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
        JLabel completeLabel = new JLabel(TextProperties.getInstance().getProperty("CalculatorWiz_Complete_L"));
        stepFivePanel.add(completeLabel);
        return stepFivePanel;
    }

    private List<String> getItemsInList(JList<String> list) {
        DefaultListModel<String> defaultRightModel = Util.getDefaultListModel(list);
        if(defaultRightModel == null) { return null; }
        return Collections.list(defaultRightModel.elements());
    }

    /* Add main for quick UI Testing */
    static public void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) { e.printStackTrace(); }

        CalculatorWizard sanitizerWizard = new CalculatorWizard(null);
        sanitizerWizard.buildAndShowUI();
    }
}