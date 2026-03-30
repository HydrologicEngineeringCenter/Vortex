package mil.army.usace.hec.vortex.ui;

import mil.army.usace.hec.vortex.VortexProperty;
import mil.army.usace.hec.vortex.math.BatchTimeStepResampler;
import mil.army.usace.hec.vortex.math.TimeStep;
import mil.army.usace.hec.vortex.ui.util.FileSaveUtil;
import mil.army.usace.hec.vortex.util.DssUtil;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TimeStepResamplerWizard extends VortexWizard {
    private static final Logger logger = Logger.getLogger(TimeStepResamplerWizard.class.getName());

    private final Frame frame;
    private SourceFileSelectionPanel sourceFileSelectionPanel;
    private DestinationSelectionPanel destinationSelectionPanel;

    private Container contentCards;
    private CardLayout cardLayout;
    private JButton backButton, nextButton, cancelButton;
    private int cardNumber;

    private JTextField sourceFileTextField;
    private JList<String> chosenSourceGridsList;

    private TimeStepComboBox timeStepComboBox;

    private final ProgressMessagePanel progressMessagePanel = new ProgressMessagePanel();

    public TimeStepResamplerWizard(Frame frame) {
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

    @Override
    public void buildAndShowUI() {
        /* Setting Wizard's names and layout */
        this.setTitle(TextProperties.getInstance().getProperty("TimeStepResamplerWiz_Title"));
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
        backButton = new JButton(TextProperties.getInstance().getProperty("TimeStepResamplerWiz_Back"));
        backButton.setToolTipText(TextProperties.getInstance().getProperty("TimeStepResamplerWiz_Back_TT"));
        backButton.setEnabled(false);
        backButton.addActionListener(evt -> backAction());

        /* Next Button */
        nextButton = new JButton(TextProperties.getInstance().getProperty("TimeStepResamplerWiz_Next"));
        nextButton.setToolTipText(TextProperties.getInstance().getProperty("TimeStepResamplerWiz_Next_TT"));
        nextButton.addActionListener(evt -> {
            if (nextButton.getText().equals(TextProperties.getInstance().getProperty("TimeStepResamplerWiz_Restart"))) {
                restartAction();
            } else if (nextButton.getText().equals(TextProperties.getInstance().getProperty("TimeStepResamplerWiz_Next"))) {
                nextAction();
            }
        });

        /* Cancel Button */
        cancelButton = new JButton(TextProperties.getInstance().getProperty("TimeStepResamplerWiz_Cancel"));
        cancelButton.setToolTipText(TextProperties.getInstance().getProperty("TimeStepResamplerWiz_Cancel_TT"));
        cancelButton.addActionListener(evt -> closeAction());

        /* Adding Buttons to NavigationPanel */
        buttonPanel.add(backButton);
        buttonPanel.add(nextButton);
        buttonPanel.add(cancelButton);

        /* Add buttonPanel to TimeStepResamplerWizard */
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
        backButton.setEnabled(cardNumber > 0 && cardNumber < 3);
        nextButton.setEnabled(cardNumber < 3);
        cancelButton.setEnabled(cardNumber < 3);
    }

    private void setButtonsForRestartOrClose() {
        backButton.setVisible(false);
        nextButton.setText(TextProperties.getInstance().getProperty("TimeStepResamplerWiz_Restart"));
        nextButton.setToolTipText(TextProperties.getInstance().getProperty("TimeStepResamplerWiz_Restart_TT"));
        nextButton.setEnabled(true);
        cancelButton.setText(TextProperties.getInstance().getProperty("TimeStepResamplerWiz_Close"));
        cancelButton.setToolTipText(TextProperties.getInstance().getProperty("TimeStepResamplerWiz_Close_TT"));
        cancelButton.setEnabled(true);
    }

    private void backAction() {
        cardNumber--;
        if (cardNumber == 0) {
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
        nextButton.setText(TextProperties.getInstance().getProperty("TimeStepResamplerWiz_Next"));
        nextButton.setToolTipText(TextProperties.getInstance().getProperty("TimeStepResamplerWiz_Next_TT"));

        cancelButton.setText(TextProperties.getInstance().getProperty("TimeStepResamplerWiz_Cancel"));
        cancelButton.setToolTipText(TextProperties.getInstance().getProperty("TimeStepResamplerWiz_Cancel_TT"));

        /* Clearing Step One Panel */
        sourceFileSelectionPanel.clear();

        /* Clearing Step Two Panel */
        timeStepComboBox.setSelectedIndex(10);

        /* Clearing Step Three Panel */
        destinationSelectionPanel.getDestinationTextField().setText("");
        destinationSelectionPanel.getFieldA().setText("");
        destinationSelectionPanel.getFieldB().setText("");
        destinationSelectionPanel.getFieldF().setText("");

        /* Clearing Step Four Panel */
        progressMessagePanel.clear();
    }

    private boolean validateCurrentStep() {
        switch (cardNumber) {
            case 0:
                return validateStepOne();
            case 1:
                return validateStepTwo();
            case 2:
                return validateStepThree();
            case 3:
                return validateStepFour();
            default:
                return unknownStepError();
        }
    }

    private void submitCurrentStep() {
        switch (cardNumber) {
            case 0:
                submitStepOne();
                break;
            case 1:
                submitStepTwo();
                break;
            case 2:
                submitStepThree();
                break;
            case 3:
                submitStepFour();
                break;
            default:
                unknownStepError();
                break;
        }
    }

    private boolean unknownStepError() {
        logger.log(Level.SEVERE, "Unknown Step in Wizard");
        return false;
    }

    /* Step One: Source File Selection */
    private JPanel stepOnePanel() {
        sourceFileSelectionPanel = new SourceFileSelectionPanel(TimeStepResamplerWizard.class.getName());
        sourceFileTextField = sourceFileSelectionPanel.getSourceFileTextField();
        chosenSourceGridsList = sourceFileSelectionPanel.getChosenSourceGridsList();
        return sourceFileSelectionPanel;
    }

    private boolean validateStepOne() {
        return sourceFileSelectionPanel.validateInput();
    }

    private void submitStepOne() {
    }

    /* Step Two: Time Step Configuration */
    private JPanel stepTwoPanel() {
        JLabel instructionLabel = new JLabel(TextProperties.getInstance().getProperty("TimeStepResamplerWiz_Instruction_L"));
        JPanel instructionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        instructionPanel.add(instructionLabel);

        JLabel timeStepLabel = new JLabel(TextProperties.getInstance().getProperty("TimeStepResamplerWiz_TimeStep_L"));
        timeStepComboBox = new TimeStepComboBox();

        JPanel selectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        selectionPanel.add(timeStepLabel);
        selectionPanel.add(Box.createRigidArea(new Dimension(2, 0)));
        selectionPanel.add(timeStepComboBox);

        GridBagLayout gridBagLayout = new GridBagLayout();
        JPanel stepTwoPanel = new JPanel(gridBagLayout);
        stepTwoPanel.setBorder(BorderFactory.createEmptyBorder(5, 9, 5, 8));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(0, 0, 5, 0);
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.weighty = 0;

        gbc.gridy = 0;
        stepTwoPanel.add(instructionPanel, gbc);

        gbc.gridy = 1;
        stepTwoPanel.add(selectionPanel, gbc);

        gbc.gridy = 2;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        stepTwoPanel.add(new JPanel(), gbc);

        return stepTwoPanel;
    }

    private boolean validateStepTwo() {
        if (timeStepComboBox.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "A time step must be selected.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    private void submitStepTwo() {
    }

    /* Step Three: Destination Selection */
    private JPanel stepThreePanel() {
        destinationSelectionPanel = new DestinationSelectionPanel(this);
        return destinationSelectionPanel;
    }

    private boolean validateStepThree() {
        String destinationFile = destinationSelectionPanel.getDestinationTextField().getText();
        if (destinationFile == null || destinationFile.isEmpty()) {
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
                timeStepResamplerTask();
                return null;
            }

            @Override
            protected void done() {
                nextAction();
            }
        };

        task.execute();
    }

    private void timeStepResamplerTask() {
        String pathToSource = sourceFileTextField.getText();
        List<String> chosenSourceList = getItemsInList(chosenSourceGridsList);
        if (chosenSourceList == null) return;
        Set<String> sourceGrids = new HashSet<>(chosenSourceList);

        TimeStep selectedTimeStep = timeStepComboBox.getSelected();

        String destination = destinationSelectionPanel.getDestinationTextField().getText();

        /* Setting parts */
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

        BatchTimeStepResampler batchTimeStepResampler = BatchTimeStepResampler.builder()
                .pathToInput(pathToSource)
                .variables(sourceGrids)
                .timeStep(selectedTimeStep)
                .destination(destination)
                .writeOptions(writeOptions)
                .build();

        batchTimeStepResampler.addPropertyChangeListener(evt -> SwingUtilities.invokeLater(() -> {
            VortexProperty property = VortexProperty.parse(evt.getPropertyName());
            if (VortexProperty.PROGRESS == property) {
                if (!(evt.getNewValue() instanceof Integer)) return;
                int progressValue = (int) evt.getNewValue();
                progressMessagePanel.setValue(progressValue);
            } else {
                String value = String.valueOf(evt.getNewValue());
                progressMessagePanel.write(value);
            }
        }));

        SwingWorker<Void, Void> task = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                batchTimeStepResampler.run();
                return null;
            }

            @Override
            protected void done() {
                setButtonsForRestartOrClose();
            }
        };

        task.execute();
    }

    /* Step Four: Progress */
    private JPanel stepFourPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(progressMessagePanel, BorderLayout.CENTER);
        return panel;
    }

    private boolean validateStepFour() {
        return true;
    }

    private void submitStepFour() {
    }

    /* Step Five: Results */
    private JPanel stepFivePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(progressMessagePanel, BorderLayout.CENTER);
        return panel;
    }

    private List<String> getItemsInList(JList<String> list) {
        DefaultListModel<String> defaultRightModel = Util.getDefaultListModel(list);
        if (defaultRightModel == null) {
            return null;
        }
        return Collections.list(defaultRightModel.elements());
    }

    private void closeAction() {
        TimeStepResamplerWizard.this.setVisible(false);
        TimeStepResamplerWizard.this.dispose();
        String savedFile = destinationSelectionPanel.getDestinationTextField().getText();
        FileSaveUtil.showFileLocation(TimeStepResamplerWizard.this, Path.of(savedFile));
    }

    /* Add main for quick UI Testing */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                 UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        TimeStepResamplerWizard wizard = new TimeStepResamplerWizard(null);
        wizard.buildAndShowUI();
    }
}
