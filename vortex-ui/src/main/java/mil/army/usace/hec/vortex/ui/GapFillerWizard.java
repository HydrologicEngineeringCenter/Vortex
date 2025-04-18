package mil.army.usace.hec.vortex.ui;

import mil.army.usace.hec.vortex.math.BatchGapFiller;
import mil.army.usace.hec.vortex.math.GapFillMethod;
import mil.army.usace.hec.vortex.ui.util.FileSaveUtil;
import mil.army.usace.hec.vortex.util.DssUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GapFillerWizard extends VortexWizard {
    private static final Logger LOGGER = Logger.getLogger(GapFillerWizard.class.getName());

    private static final int ROW_HEIGHT = (int) new JTextField().getPreferredSize().getHeight();
    private static final int PAD = 2;

    private static final String FOCAL_MEAN_LABEL = TextProperties.getInstance().getProperty("GapFillerWiz_FocalMean_L");
    private static final String LINEAR_INTERP_LABEL = TextProperties.getInstance().getProperty("GapFillerWiz_LinearInterp_L");
    private static final String INSERT_TIME_STEPS_LABEL = TextProperties.getInstance().getProperty("GapFillerWiz_TimeSteps_L");

    private final Frame frame;
    private SourceFileSelectionPanel sourceFileSelectionPanel;
    private DestinationSelectionPanel destinationSelectionPanel;

    private Container contentCards;
    private CardLayout cardLayout;
    private JButton backButton;
    private JButton nextButton;
    private JButton cancelButton;
    private int cardNumber;

    private JTextField sourceFileTextField;
    private JList<String> chosenSourceGridsList;
    private JProgressBar progressBar;

    private ButtonGroup buttonGroup;

    private JCheckBox insertTimeStepsCheckBox;

    public GapFillerWizard(Frame frame) {
        super();
        this.frame = frame;
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                closeAction();
            }
        });
    }

    public void buildAndShowUI() {
        /* Setting Wizard's names and layout */
        setTitle(TextProperties.getInstance().getProperty("GapFillerWiz_Title"));
        setIconImage(IconResources.loadImage("images/vortex_black.png"));
        setMinimumSize(new Dimension(600, 400));
        setLocation(getPersistedLocation());
        if (frame != null) setLocationRelativeTo(frame);
        setSize(getPersistedSize());
        setLayout(new BorderLayout());

        /* Initializing Card Container */
        initializeContentCards();

        /* Initializing Button Panel (Back, Next, Cancel) */
        initializeButtonPanel();

        /* Add contentCards to wizard, and then show wizard */
        add(contentCards, BorderLayout.CENTER);
        setVisible(true);
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
        backButton = new JButton(TextProperties.getInstance().getProperty("GapFillerWiz_Back"));
        backButton.setToolTipText(TextProperties.getInstance().getProperty("GapFillerWiz_Back_TT"));
        backButton.setEnabled(false);
        backButton.addActionListener(evt -> backAction());

        /* Next Button */
        nextButton = new JButton(TextProperties.getInstance().getProperty("GapFillerWiz_Next"));
        nextButton.setToolTipText(TextProperties.getInstance().getProperty("GapFillerWiz_Next_TT"));
        nextButton.addActionListener(evt -> {
            if (nextButton.getText().equals(TextProperties.getInstance().getProperty("GapFillerWiz_Restart"))) {
                restartAction();
            } else if (nextButton.getText().equals(TextProperties.getInstance().getProperty("GapFillerWiz_Next"))) {
                nextAction();
            }
        });

        /* Cancel Button */
        cancelButton = new JButton(TextProperties.getInstance().getProperty("GapFillerWiz_Cancel"));
        cancelButton.setToolTipText(TextProperties.getInstance().getProperty("GapFillerWiz_Cancel_TT"));
        cancelButton.addActionListener(evt -> closeAction());

        /* Adding Buttons to NavigationPanel */
        buttonPanel.add(backButton);
        buttonPanel.add(nextButton);
        buttonPanel.add(cancelButton);

        /* Add buttonPanel to GapFillerWizard */
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void nextAction() {
        if (!validateCurrentStep()) return;
        submitCurrentStep();
        cardNumber++;
        backButton.setEnabled(true);

        if (cardNumber == 4) {
            backButton.setEnabled(false);
            nextButton.setEnabled(false);
        } // If: Step Four (Processing...) Then disable Back and Next button

        if (cardNumber == 5) {
            backButton.setVisible(false);
            nextButton.setText(TextProperties.getInstance().getProperty("GapFillerWiz_Restart"));
            nextButton.setToolTipText(TextProperties.getInstance().getProperty("GapFillerWiz_Restart_TT"));
            nextButton.setEnabled(true);
            cancelButton.setText(TextProperties.getInstance().getProperty("GapFillerWiz_Close"));
            cancelButton.setToolTipText(TextProperties.getInstance().getProperty("GapFillerWiz_Close_TT"));
        } // If: Step Five (Change Cancel to Close)

        cardLayout.next(contentCards);
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
        nextButton.setText(TextProperties.getInstance().getProperty("GapFillerWiz_Next"));
        nextButton.setToolTipText(TextProperties.getInstance().getProperty("GapFillerWiz_Next_TT"));

        cancelButton.setText(TextProperties.getInstance().getProperty("GapFillerWiz_Cancel"));
        cancelButton.setToolTipText(TextProperties.getInstance().getProperty("GapFillerWiz_Cancel_TT"));

        /* Clearing Step One Panel */
        sourceFileSelectionPanel.clear();

        /* Clearing Step Two Panel */
        buttonGroup.clearSelection();

        /* Clearing Step Three Panel */
        insertTimeStepsCheckBox.setSelected(false);

        /* Clearing Step Four Panel */
        destinationSelectionPanel.getDestinationTextField().setText("");
        destinationSelectionPanel.getFieldA().setText("");
        destinationSelectionPanel.getFieldB().setText("");
        destinationSelectionPanel.getFieldF().setText("");

        /* Clearing Step Five Panel */
        progressBar.setIndeterminate(true);
        progressBar.setStringPainted(false);
        progressBar.setValue(0);
        progressBar.setString("0%");
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
            case 4:
                submitStepFive();
                break;
            case 5:
                submitStepSix();
                break;
            default:
                unknownStepError();
                break;
        }
    }

    private boolean unknownStepError() {
        LOGGER.log(Level.SEVERE, "Unknown Step in Wizard");
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

    private void submitStepOne() {
    }

    private JPanel stepTwoPanel() {
        JPanel selectInterpolationPanel = initInterpolationPanel();

        // Initialize spatial fill panel
        JPanel spatialFillPanel = new JPanel();
        spatialFillPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

        String spatialFillTitle = TextProperties.getInstance().getProperty("GapFillerWiz_SpatialFill_Title");
        String spatialFillTT = TextProperties.getInstance().getProperty("GapFillerWiz_SpatialFill_TT");

        spatialFillPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), spatialFillTitle));

        spatialFillPanel.setToolTipText(spatialFillTT);

        JRadioButton focalMeanButton = new JRadioButton(FOCAL_MEAN_LABEL);
        focalMeanButton.setToolTipText(spatialFillTT);

        spatialFillPanel.add(focalMeanButton);

        // Initialize temporal fill panel
        JPanel temporalFillPanel = new JPanel();
        temporalFillPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

        String temporalFillTitle = TextProperties.getInstance().getProperty("GapFillerWiz_TemporalFill_Title");
        String temporalFillTT = TextProperties.getInstance().getProperty("GapFillerWiz_TemporalFill_TT");

        temporalFillPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), temporalFillTitle));

        temporalFillPanel.setToolTipText(temporalFillTT);

        JRadioButton linearInterpButton = new JRadioButton(LINEAR_INTERP_LABEL);
        linearInterpButton.setToolTipText(temporalFillTT);

        temporalFillPanel.add(linearInterpButton);

        // Initialize button group for interpolation options
        buttonGroup = new ButtonGroup();
        buttonGroup.add(focalMeanButton);
        buttonGroup.add(linearInterpButton);

        // Add all panels
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(selectInterpolationPanel);
        panel.add(spatialFillPanel);
        panel.add(temporalFillPanel);

        return panel;
    }

    private static JPanel initInterpolationPanel() {
        String selectInterpolationText = "Select an Interpolation Method";
        JLabel selectInterpolationLabel = new JLabel(selectInterpolationText);
        selectInterpolationLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        selectInterpolationLabel.setBorder(new EmptyBorder(PAD, PAD, PAD, PAD));

        String selectInterpolationDescText = "Interpolations will be performed on gridded records that already exist.";
        JLabel selectInterpolationDescLabel = new JLabel(selectInterpolationDescText);
        selectInterpolationDescLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        selectInterpolationDescLabel.setBorder(new EmptyBorder(PAD, 4 * PAD, PAD, PAD));

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        Dimension dimension = new Dimension(Integer.MAX_VALUE, (ROW_HEIGHT + 2 * PAD) * 2);

        panel.setMinimumSize(dimension);
        panel.setMaximumSize(dimension);
        panel.setPreferredSize(dimension);

        panel.add(selectInterpolationLabel);
        panel.add(selectInterpolationDescLabel);
        return panel;
    }

    private boolean validateStepTwo() {
        //Is the lower threshold box checked
        if (getSelectedButtonText(buttonGroup) == null) {
            JOptionPane.showMessageDialog(this, "One gap fill method must be selected.",
                    "Error: Invalid Selection", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    private void submitStepTwo() {
    }

    private JPanel stepThreePanel() {
        String selectAddMissingText = "Add records for missing time steps";
        JLabel selectAddMissingLabel = new JLabel(selectAddMissingText);
        selectAddMissingLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        selectAddMissingLabel.setBorder(new EmptyBorder(PAD, PAD, PAD, PAD));

        String selectAddMissingDescText = "If selected, gridded records will be inserted at missing time-steps.";
        JLabel selectAddMissingDescLabel = new JLabel(selectAddMissingDescText);
        selectAddMissingDescLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        selectAddMissingDescLabel.setBorder(new EmptyBorder(PAD, 4 * PAD, PAD, PAD));

        // Initialize time steps panel
        String timeStepsTT = TextProperties.getInstance().getProperty("GapFillerWiz_TimeSteps_TT");

        insertTimeStepsCheckBox = new JCheckBox(INSERT_TIME_STEPS_LABEL);
        insertTimeStepsCheckBox.setToolTipText(timeStepsTT);
        insertTimeStepsCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setToolTipText(timeStepsTT);

        panel.add(selectAddMissingLabel);
        panel.add(selectAddMissingDescLabel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(insertTimeStepsCheckBox);

        return panel;
    }

    private boolean validateStepThree() {
        return true;
    }

    private void submitStepThree() {
        // No-op
    }

    private JPanel stepFourPanel() {
        destinationSelectionPanel = new DestinationSelectionPanel(this);
        return destinationSelectionPanel;
    }

    private boolean validateStepFour() {
        String destinationFile = destinationSelectionPanel.getDestinationTextField().getText();
        if (destinationFile == null || destinationFile.isEmpty()) {
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
                GapFillerWizard.this.process();
                return null;
            }

            @Override
            protected void done() {
                nextAction();
            }
        };

        task.execute();
    }

    private void process() {
        String pathToSource = sourceFileTextField.getText();
        List<String> chosenSourceGrids = getItemsInList(chosenSourceGridsList);
        if (chosenSourceGrids == null) return;
        Set<String> sourceGrids = new HashSet<>(chosenSourceGrids);

        String destination = destinationSelectionPanel.getDestinationTextField().getText();

        /* Setting parts */
        List<String> chosenSourceList = getItemsInList(chosenSourceGridsList);
        if (chosenSourceList == null) return;
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

        String selection = getSelectedButtonText(buttonGroup);
        GapFillMethod method = fromString(selection);

        BatchGapFiller batchGapFiller = BatchGapFiller.builder()
                .source(pathToSource)
                .variables(new ArrayList<>(sourceGrids))
                .method(method)
                .destination(destination)
                .writeOptions(writeOptions)
                .build();

        batchGapFiller.addPropertyChangeListener(evt -> {
            if (evt.getPropertyName().equalsIgnoreCase("progress")) {
                if (!(evt.getNewValue() instanceof Integer)) return;
                int progressValue = (int) evt.getNewValue();
                progressBar.setIndeterminate(false);
                progressBar.setStringPainted(true);
                progressBar.setValue(progressValue);
                progressBar.setString(progressValue + "%");
            }
        });

        batchGapFiller.run();

        if (insertTimeStepsCheckBox.isSelected()) {
            BatchGapFiller timeStepFiller = BatchGapFiller.builder()
                    .source(destination)
                    .variables(List.copyOf(sourceGrids))
                    .method(GapFillMethod.TIME_STEP)
                    .destination(destination)
                    .writeOptions(writeOptions)
                    .build();

            timeStepFiller.run();
        }
    }

    private JPanel stepFivePanel() {
        JPanel stepFourPanel = new JPanel(new GridBagLayout());

        JPanel insidePanel = new JPanel();
        insidePanel.setLayout(new BoxLayout(insidePanel, BoxLayout.Y_AXIS));

        JLabel processingLabel = new JLabel(TextProperties.getInstance().getProperty("GapFillerWiz_Processing_L"));
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

    private boolean validateStepFive() {
        return true;
    }

    private void submitStepFive() {
    }

    private JPanel stepSixPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        JLabel completeLabel = new JLabel(TextProperties.getInstance().getProperty("GapFillerWiz_Complete_L"));
        panel.add(completeLabel);
        return panel;
    }

    private void submitStepSix() {
    }

    private List<String> getItemsInList(JList<String> list) {
        DefaultListModel<String> defaultRightModel = Util.getDefaultListModel(list);
        if (defaultRightModel == null) {
            return Collections.emptyList();
        }
        return Collections.list(defaultRightModel.elements());
    }

    private void closeAction() {
        setVisible(false);
        dispose();
        String savedFile = destinationSelectionPanel.getDestinationTextField().getText();
        FileSaveUtil.showFileLocation(GapFillerWizard.this, Path.of(savedFile));
    }

    private static String getSelectedButtonText(ButtonGroup group) {
        for (Enumeration<AbstractButton> buttons = group.getElements(); buttons.hasMoreElements(); ) {
            AbstractButton button = buttons.nextElement();
            if (button.isSelected()) {
                return button.getText();
            }
        }
        return null; // No button selected
    }

    private static GapFillMethod fromString(String str) {
        if (FOCAL_MEAN_LABEL.equals(str))
            return GapFillMethod.FOCAL_MEAN;
        if (LINEAR_INTERP_LABEL.equals(str))
            return GapFillMethod.LINEAR_INTERPOLATION;

        return GapFillMethod.UNDEFINED;
    }

    /* Add main for quick UI Testing */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e, e::getMessage);
        }

        GapFillerWizard wizard = new GapFillerWizard(null);
        wizard.buildAndShowUI();
    }
}