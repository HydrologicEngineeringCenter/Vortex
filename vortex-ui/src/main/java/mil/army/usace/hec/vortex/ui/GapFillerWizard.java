package mil.army.usace.hec.vortex.ui;

import mil.army.usace.hec.vortex.math.BatchGapFiller;
import mil.army.usace.hec.vortex.math.GapFillMethod;
import mil.army.usace.hec.vortex.ui.util.FileSaveUtil;
import mil.army.usace.hec.vortex.util.DssUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A wizard for filling gaps in time series data using various interpolation methods.
 * Supports both spatial and temporal gap filling techniques.
 */
public class GapFillerWizard extends VortexWizard {
    private static final Logger LOGGER = Logger.getLogger(GapFillerWizard.class.getName());

    // UI Constants
    private static final int ROW_HEIGHT = (int) new JTextField().getPreferredSize().getHeight();
    private static final int PAD = 2;
    private static final Dimension PANEL_DIMENSION = new Dimension(Integer.MAX_VALUE, (ROW_HEIGHT + 2 * PAD) * 2);

    // Text constants from properties
    private static final String FOCAL_MEAN_LABEL = TextProperties.INSTANCE.getProperty("GapFillerWiz_FocalMean_L");
    private static final String LINEAR_INTERP_LABEL = TextProperties.INSTANCE.getProperty("GapFillerWiz_LinearInterp_L");
    private static final String INSERT_TIME_STEPS_LABEL = TextProperties.INSTANCE.getProperty("GapFillerWiz_TimeSteps_L");
    private static final String TITLE = TextProperties.INSTANCE.getProperty("GapFillerWiz_Title");
    private static final String BACK_BUTTON = TextProperties.INSTANCE.getProperty("GapFillerWiz_Back");
    private static final String NEXT_BUTTON = TextProperties.INSTANCE.getProperty("GapFillerWiz_Next");
    private static final String CANCEL_BUTTON = TextProperties.INSTANCE.getProperty("GapFillerWiz_Cancel");
    private static final String RESTART_BUTTON = TextProperties.INSTANCE.getProperty("GapFillerWiz_Restart");
    private static final String CLOSE_BUTTON = TextProperties.INSTANCE.getProperty("GapFillerWiz_Close");

    // UI Components
    private final Frame frame;
    private SourceFileSelectionPanel sourceFileSelectionPanel;
    private DestinationSelectionPanel destinationSelectionPanel;
    private final ProgressMessagePanel progressMessagePanel = new ProgressMessagePanel();

    private Container contentCards;
    private CardLayout cardLayout;
    private JButton backButton;
    private JButton nextButton;
    private JButton cancelButton;
    private int cardNumber;

    private JTextField sourceFileTextField;
    private JList<String> chosenSourceGridsList;
    private ButtonGroup methodButtonGroup;
    private JCheckBox insertTimeStepsCheckBox;

    /**
     * Creates a new GapFillerWizard with the specified parent frame.
     *
     * @param frame The parent frame, can be null
     */
    public GapFillerWizard(Frame frame) {
        super();
        this.frame = frame;
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeAction();
            }
        });
    }

    /**
     * Builds and displays the user interface for the wizard.
     */
    public void buildAndShowUI() {
        configureWizardFrame();
        initializeContentCards();
        initializeButtonPanel();
        add(contentCards, BorderLayout.CENTER);
        setVisible(true);
    }

    /**
     * Configures the wizard's frame settings.
     */
    private void configureWizardFrame() {
        setTitle(TITLE);
        setIconImage(IconResources.loadImage("images/vortex_black.png"));
        setMinimumSize(new Dimension(600, 400));
        setLocation(getPersistedLocation());
        if (frame != null) setLocationRelativeTo(frame);
        setSize(getPersistedSize());
        setLayout(new BorderLayout());
    }

    /**
     * Initializes the content cards container with all wizard steps.
     */
    private void initializeContentCards() {
        contentCards = new Container();
        cardLayout = new CardLayout();
        contentCards.setLayout(cardLayout);
        cardNumber = 0;

        contentCards.add("Step One", stepOnePanel());
        contentCards.add("Step Two", stepTwoPanel());
        contentCards.add("Step Three", stepThreePanel());
        contentCards.add("Step Four", stepFourPanel());
        contentCards.add("Step Five", stepFivePanel());
        contentCards.add("Step Six", stepSixPanel());
    }

    /**
     * Initializes the button panel with navigation buttons.
     */
    private void initializeButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        backButton = createButton(BACK_BUTTON,
                TextProperties.INSTANCE.getProperty("GapFillerWiz_Back_TT"),
                false, e -> backAction());

        nextButton = createButton(NEXT_BUTTON,
                TextProperties.INSTANCE.getProperty("GapFillerWiz_Next_TT"),
                true, e -> handleNextButtonClick());

        cancelButton = createButton(CANCEL_BUTTON,
                TextProperties.INSTANCE.getProperty("GapFillerWiz_Cancel_TT"),
                true, e -> closeAction());

        buttonPanel.add(backButton);
        buttonPanel.add(nextButton);
        buttonPanel.add(cancelButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * Creates a button with specified properties.
     *
     * @param text    The button text
     * @param tooltip The tooltip text
     * @param enabled Whether the button is initially enabled
     * @param action  The action to perform when clicked
     * @return The configured button
     */
    private JButton createButton(String text, String tooltip, boolean enabled, java.awt.event.ActionListener action) {
        JButton button = new JButton(text);
        button.setToolTipText(tooltip);
        button.setEnabled(enabled);
        button.addActionListener(action);
        return button;
    }

    /**
     * Handles the next button click based on current text.
     */
    private void handleNextButtonClick() {
        if (nextButton.getText().equals(RESTART_BUTTON)) {
            restartAction();
        } else if (nextButton.getText().equals(NEXT_BUTTON)) {
            nextAction();
        }
    }

    /**
     * Advances to the next step in the wizard.
     */
    private void nextAction() {
        if (!validateCurrentStep()) return;
        submitCurrentStep();
        cardNumber++;
        updateButtonStates();
        cardLayout.next(contentCards);
    }

    /**
     * Updates button states based on the current card number.
     */
    private void updateButtonStates() {
        backButton.setEnabled(cardNumber > 0);

        if (cardNumber == 4) {
            backButton.setEnabled(false);
            nextButton.setEnabled(false);
        } else if (cardNumber == 5) {
            backButton.setVisible(false);
            nextButton.setText(RESTART_BUTTON);
            nextButton.setToolTipText(TextProperties.INSTANCE.getProperty("GapFillerWiz_Restart_TT"));
            nextButton.setEnabled(true);
            cancelButton.setText(CLOSE_BUTTON);
            cancelButton.setToolTipText(TextProperties.INSTANCE.getProperty("GapFillerWiz_Close_TT"));
        }
    }

    /**
     * Goes back to the previous step in the wizard.
     */
    private void backAction() {
        cardNumber--;
        backButton.setEnabled(cardNumber > 0);
        cardLayout.previous(contentCards);
    }

    /**
     * Restarts the wizard, resetting all fields and returning to the first step.
     */
    private void restartAction() {
        cardNumber = 0;
        cardLayout.first(contentCards);

        // Reset UI components
        resetButtons();
        resetPanels();
    }

    /**
     * Resets all buttons to their initial state.
     */
    private void resetButtons() {
        backButton.setVisible(true);
        backButton.setEnabled(false);

        nextButton.setEnabled(true);
        nextButton.setText(NEXT_BUTTON);
        nextButton.setToolTipText(TextProperties.INSTANCE.getProperty("GapFillerWiz_Next_TT"));

        cancelButton.setText(CANCEL_BUTTON);
        cancelButton.setToolTipText(TextProperties.INSTANCE.getProperty("GapFillerWiz_Cancel_TT"));
    }

    /**
     * Resets all panels to their initial state.
     */
    private void resetPanels() {
        sourceFileSelectionPanel.clear();
        methodButtonGroup.clearSelection();
        insertTimeStepsCheckBox.setSelected(false);
        destinationSelectionPanel.getDestinationTextField().setText("");
        destinationSelectionPanel.getFieldA().setText("");
        destinationSelectionPanel.getFieldB().setText("");
        destinationSelectionPanel.getFieldF().setText("");
        progressMessagePanel.clear();
    }

    /**
     * Validates the current step based on the card number.
     *
     * @return true if the step is valid, false otherwise
     */
    private boolean validateCurrentStep() {
        return switch (cardNumber) {
            case 0 -> validateStepOne();
            case 1 -> validateStepTwo();
            case 2 -> validateStepThree();
            case 3 -> validateStepFour();
            case 4 -> true; // Processing step is always valid
            default -> {
                LOGGER.log(Level.WARNING, "Unknown validation step: {0}", cardNumber);
                yield false;
            }
        };
    }

    /**
     * Submits the current step based on the card number.
     */
    private void submitCurrentStep() {
        switch (cardNumber) {
            case 0, 1, 2, 5 -> { /* No specific actions needed */ }
            case 3 -> submitStepFour();
            case 4 -> { /* Processing step handled separately */ }
            default -> LOGGER.log(Level.WARNING, "Unknown submit step: {0}", cardNumber);
        }
    }

    /**
     * Creates the step one panel for source file selection.
     *
     * @return The configured panel
     */
    private JPanel stepOnePanel() {
        sourceFileSelectionPanel = new SourceFileSelectionPanel(CalculatorWizard.class.getName());
        sourceFileTextField = sourceFileSelectionPanel.getSourceFileTextField();
        chosenSourceGridsList = sourceFileSelectionPanel.getChosenSourceGridsList();
        return sourceFileSelectionPanel;
    }

    /**
     * Validates the input for step one.
     *
     * @return true if valid, false otherwise
     */
    private boolean validateStepOne() {
        return sourceFileSelectionPanel.validateInput();
    }

    /**
     * Creates the step two panel for interpolation method selection.
     *
     * @return The configured panel
     */
    private JPanel stepTwoPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Add explanation panel
        panel.add(createInterpolationExplanationPanel());

        // Add spatial fill panel
        panel.add(createSpatialFillPanel());

        // Add temporal fill panel
        panel.add(createTemporalFillPanel());

        return panel;
    }

    /**
     * Creates the interpolation explanation panel.
     *
     * @return The configured panel
     */
    private JPanel createInterpolationExplanationPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        String selectInterpolationText = TextProperties.INSTANCE.getProperty("GapFillerWiz_SpatialFill_Select_L");
        JLabel selectInterpolationLabel = new JLabel(selectInterpolationText);
        selectInterpolationLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        selectInterpolationLabel.setBorder(new EmptyBorder(PAD, PAD, PAD, PAD));

        String selectInterpolationDescText = TextProperties.INSTANCE.getProperty("GapFillerWiz_SpatialFill_Select_Desc");
        JLabel selectInterpolationDescLabel = new JLabel(selectInterpolationDescText);
        selectInterpolationDescLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        selectInterpolationDescLabel.setBorder(new EmptyBorder(PAD, 4 * PAD, PAD, PAD));

        panel.setMinimumSize(PANEL_DIMENSION);
        panel.setMaximumSize(PANEL_DIMENSION);
        panel.setPreferredSize(PANEL_DIMENSION);

        panel.add(selectInterpolationLabel);
        panel.add(selectInterpolationDescLabel);

        return panel;
    }

    /**
     * Creates the spatial fill panel.
     *
     * @return The configured panel
     */
    private JPanel createSpatialFillPanel() {
        JPanel spatialFillPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        String spatialFillTitle = TextProperties.INSTANCE.getProperty("GapFillerWiz_SpatialFill_Title");
        String spatialFillTT = TextProperties.INSTANCE.getProperty("GapFillerWiz_SpatialFill_TT");

        spatialFillPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), spatialFillTitle));
        spatialFillPanel.setToolTipText(spatialFillTT);

        JRadioButton focalMeanButton = new JRadioButton(FOCAL_MEAN_LABEL);
        focalMeanButton.setToolTipText(spatialFillTT);

        // Initialize the button group if it hasn't been created yet
        if (methodButtonGroup == null) {
            methodButtonGroup = new ButtonGroup();
        }
        methodButtonGroup.add(focalMeanButton);

        spatialFillPanel.add(focalMeanButton);

        return spatialFillPanel;
    }

    /**
     * Creates the temporal fill panel.
     *
     * @return The configured panel
     */
    private JPanel createTemporalFillPanel() {
        JPanel temporalFillPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        String temporalFillTitle = TextProperties.INSTANCE.getProperty("GapFillerWiz_TemporalFill_Title");
        String temporalFillTT = TextProperties.INSTANCE.getProperty("GapFillerWiz_TemporalFill_TT");

        temporalFillPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), temporalFillTitle));
        temporalFillPanel.setToolTipText(temporalFillTT);

        JRadioButton linearInterpButton = new JRadioButton(LINEAR_INTERP_LABEL);
        linearInterpButton.setToolTipText(temporalFillTT);

        // Add to the button group created in createSpatialFillPanel
        methodButtonGroup.add(linearInterpButton);

        temporalFillPanel.add(linearInterpButton);

        return temporalFillPanel;
    }

    /**
     * Validates the input for step two.
     *
     * @return true if valid, false otherwise
     */
    private boolean validateStepTwo() {
        if (getSelectedButtonText(methodButtonGroup) == null) {
            String title = TextProperties.INSTANCE.getProperty("GapFillerWiz_NoMethodSelected_Title");
            String message = TextProperties.INSTANCE.getProperty("GapFillerWiz_NoMethodSelected_Error");
            JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    /**
     * Creates the step three panel for time step options.
     *
     * @return The configured panel
     */
    private JPanel stepThreePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Add explanation labels
        panel.add(createTimeStepExplanationPanel());

        // Add checkbox for inserting time steps
        panel.add(Box.createVerticalStrut(10));

        String timeStepsTT = TextProperties.INSTANCE.getProperty("GapFillerWiz_TimeSteps_TT");
        insertTimeStepsCheckBox = new JCheckBox(INSERT_TIME_STEPS_LABEL);
        insertTimeStepsCheckBox.setToolTipText(timeStepsTT);
        insertTimeStepsCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(insertTimeStepsCheckBox);
        panel.setToolTipText(timeStepsTT);

        return panel;
    }

    /**
     * Creates the time step explanation panel.
     *
     * @return The configured panel
     */
    private JPanel createTimeStepExplanationPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        String selectAddMissingText = TextProperties.INSTANCE.getProperty("GapFillerWiz_TimeSteps_Option_L");
        JLabel selectAddMissingLabel = new JLabel(selectAddMissingText);
        selectAddMissingLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        selectAddMissingLabel.setBorder(new EmptyBorder(PAD, PAD, PAD, PAD));

        String selectAddMissingDescText = TextProperties.INSTANCE.getProperty("GapFillerWiz_TimeSteps_Option_Desc");
        JLabel selectAddMissingDescLabel = new JLabel(selectAddMissingDescText);
        selectAddMissingDescLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        selectAddMissingDescLabel.setBorder(new EmptyBorder(PAD, 4 * PAD, PAD, PAD));

        panel.add(selectAddMissingLabel);
        panel.add(selectAddMissingDescLabel);

        return panel;
    }

    /**
     * Validates the input for step three.
     *
     * @return true if valid, false otherwise
     */
    private boolean validateStepThree() {
        return true; // No validation needed for step three
    }

    /**
     * Creates the step four panel for destination selection.
     *
     * @return The configured panel
     */
    private JPanel stepFourPanel() {
        destinationSelectionPanel = new DestinationSelectionPanel(this);
        return destinationSelectionPanel;
    }

    /**
     * Validates the input for step four.
     *
     * @return true if valid, false otherwise
     */
    private boolean validateStepFour() {
        String destinationFile = destinationSelectionPanel.getDestinationTextField().getText();
        if (destinationFile == null || destinationFile.isEmpty()) {
            String title = TextProperties.INSTANCE.getProperty("GapFillerWiz_NoDestination_Title");
            String message = TextProperties.INSTANCE.getProperty("GapFillerWiz_NoDestination_Error");
            JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    /**
     * Submits step four and starts the processing.
     */
    private void submitStepFour() {
        SwingWorker<Void, Void> task = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                GapFillerWizard.this.process();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get(); // Check for exceptions
                    nextAction();
                } catch (InterruptedException | ExecutionException e) {
                    // Restore interrupt status only for InterruptedException
                    if (e instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }

                    // Common error handling
                    LOGGER.log(Level.SEVERE, "Error during processing", e);
                    String title = TextProperties.INSTANCE.getProperty("GapFillerWiz_ProcessingError_Title");
                    String message = TextProperties.INSTANCE.getProperty("GapFillerWiz_ProcessingError_Message");
                    showErrorDialog(title, message + " " + e.getMessage());
                }
            }
        };
        task.execute();
    }

    /**
     * Shows an error dialog with the specified title and message.
     *
     * @param title   The dialog title
     * @param message The error message
     */
    private void showErrorDialog(String title, String message) {
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE)
        );
    }

    /**
     * Processes the gap filling operation.
     */
    private void process() {
        try {
            String pathToSource = sourceFileTextField.getText();
            List<String> chosenSourceGrids = getItemsInList(chosenSourceGridsList);
            if (chosenSourceGrids.isEmpty()) {
                progressMessagePanel.write("No source grids selected");
                return;
            }

            Set<String> sourceGrids = new HashSet<>(chosenSourceGrids);
            String destination = destinationSelectionPanel.getDestinationTextField().getText();

            // Prepare DSS path parts and write options
            Map<String, String> writeOptions = prepareWriteOptions(chosenSourceGrids, destination);

            // Get selected gap fill method
            String selection = getSelectedButtonText(methodButtonGroup);
            GapFillMethod method = fromString(selection);

            // Create and execute batch gap fillers
            List<Runnable> runnables = createGapFillers(pathToSource, sourceGrids, method, destination, writeOptions);
            executeGapFillers(runnables);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during processing", e);
            progressMessagePanel.write("ERROR: " + e.getMessage());
        }
    }

    /**
     * Prepares write options for DSS files.
     *
     * @param chosenSourceGrids The list of selected source grids
     * @param destination       The destination file path
     * @return The map of write options
     */
    private Map<String, String> prepareWriteOptions(List<String> chosenSourceGrids, String destination) {
        Map<String, String> writeOptions = new HashMap<>();

        if (destination.toLowerCase().endsWith(".dss")) {
            Map<String, Set<String>> pathnameParts = DssUtil.getPathnameParts(chosenSourceGrids);

            List<String> partAList = new ArrayList<>(pathnameParts.get("aParts"));
            List<String> partBList = new ArrayList<>(pathnameParts.get("bParts"));
            List<String> partCList = new ArrayList<>(pathnameParts.get("cParts"));
            List<String> partFList = new ArrayList<>(pathnameParts.get("fParts"));

            String partA = (partAList.size() == 1) ? partAList.get(0) : "*";
            String partB = (partBList.size() == 1) ? partBList.get(0) : "*";
            String partC = (partCList.size() == 1) ? partCList.get(0) : "PRECIPITATION";
            String partF = (partFList.size() == 1) ? partFList.get(0) : "*";

            String dssFieldA = destinationSelectionPanel.getFieldA().getText();
            String dssFieldB = destinationSelectionPanel.getFieldB().getText();
            String dssFieldC = destinationSelectionPanel.getFieldC().getText();
            String dssFieldF = destinationSelectionPanel.getFieldF().getText();

            writeOptions.put("partA", (dssFieldA.isEmpty()) ? partA : dssFieldA);
            writeOptions.put("partB", (dssFieldB.isEmpty()) ? partB : dssFieldB);
            writeOptions.put("partC", (dssFieldC.isEmpty()) ? partC : dssFieldC);
            writeOptions.put("partF", (dssFieldF.isEmpty()) ? partF : dssFieldF);
        }

        String unitsString = destinationSelectionPanel.getUnitsString();
        if (!unitsString.isEmpty()) {
            writeOptions.put("units", unitsString);
        }

        String dataType = destinationSelectionPanel.getDataType();
        if (dataType != null && !dataType.isEmpty()) {
            writeOptions.put("dataType", dataType);
        }

        return writeOptions;
    }

    /**
     * Creates gap fillers based on user selections.
     *
     * @param source       The source file path
     * @param variables    The set of variables to process
     * @param method       The gap fill method to use
     * @param destination  The destination file path
     * @param writeOptions The write options for the output
     * @return A list of runnable gap fillers
     */
    private List<Runnable> createGapFillers(String source, Set<String> variables,
                                            GapFillMethod method, String destination,
                                            Map<String, String> writeOptions) {
        List<Runnable> runnables = new ArrayList<>();

        // Add primary gap filler
        BatchGapFiller batchGapFiller = BatchGapFiller.builder()
                .source(source)
                .variables(new ArrayList<>(variables))
                .method(method)
                .destination(destination)
                .writeOptions(writeOptions)
                .build();

        batchGapFiller.addPropertyChangeListener(this::handlePropertyChange);
        runnables.add(batchGapFiller);

        // Add time step filler if selected
        if (insertTimeStepsCheckBox.isSelected()) {
            BatchGapFiller timeStepFiller = BatchGapFiller.builder()
                    .source(source)
                    .variables(new ArrayList<>(variables))
                    .method(GapFillMethod.TIME_STEP)
                    .destination(destination)
                    .writeOptions(writeOptions)
                    .build();

            timeStepFiller.addPropertyChangeListener(this::handlePropertyChange);
            runnables.add(timeStepFiller);
        }

        return runnables;
    }

    /**
     * Handles property change events from gap fillers.
     *
     * @param evt The property change event
     */
    private void handlePropertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equalsIgnoreCase("progress")) {
            if (evt.getNewValue() instanceof Integer progressValue) {
                progressMessagePanel.setValue(progressValue);
            }
        } else {
            String value = String.valueOf(evt.getNewValue());
            progressMessagePanel.write(value);
        }
    }

    /**
     * Executes the list of gap fillers in a background thread.
     *
     * @param runnables The list of gap fillers to execute
     */
    private void executeGapFillers(List<Runnable> runnables) {
        SwingWorker<Void, Void> task = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                runnables.forEach(Runnable::run);
                return null;
            }

            @Override
            protected void done() {
                // Gap-fillers do not currently report progress. Manually set to 100 when done.
                progressMessagePanel.setValue(100);
            }
        };
        task.execute();
    }

    /**
     * Creates the step five panel for displaying progress.
     *
     * @return The configured panel
     */
    private JPanel stepFivePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(progressMessagePanel, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Creates the step six panel for displaying results.
     *
     * @return The configured panel
     */
    private JPanel stepSixPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(progressMessagePanel, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Gets all items in a JList as a List of Strings.
     *
     * @param list The JList to get items from
     * @return A list of the items, or an empty list if none
     */
    private List<String> getItemsInList(JList<String> list) {
        DefaultListModel<String> model = Util.getDefaultListModel(list);
        if (model == null) {
            return Collections.emptyList();
        }
        return Collections.list(model.elements());
    }

    /**
     * Closes the wizard and shows the saved file location.
     */
    private void closeAction() {
        setVisible(false);
        dispose();
        String savedFile = destinationSelectionPanel.getDestinationTextField().getText();
        if (savedFile != null && !savedFile.isEmpty()) {
            FileSaveUtil.showFileLocation(this, Path.of(savedFile));
        }
    }

    /**
     * Gets the text of the selected button in a button group.
     *
     * @param group The button group
     * @return The text of the selected button, or null if none is selected
     */
    private static String getSelectedButtonText(ButtonGroup group) {
        for (Enumeration<AbstractButton> buttons = group.getElements(); buttons.hasMoreElements(); ) {
            AbstractButton button = buttons.nextElement();
            if (button.isSelected()) {
                return button.getText();
            }
        }
        return null; // No button selected
    }

    /**
     * Converts a string label to its corresponding GapFillMethod.
     *
     * @param str The string label to convert
     * @return The corresponding GapFillMethod
     */
    private static GapFillMethod fromString(String str) {
        if (FOCAL_MEAN_LABEL.equals(str)) {
            return GapFillMethod.FOCAL_MEAN;
        }
        if (LINEAR_INTERP_LABEL.equals(str)) {
            return GapFillMethod.LINEAR_INTERPOLATION;
        }
        return GapFillMethod.UNDEFINED;
    }

    /**
     * Main method for quick UI testing.
     *
     * @param args Command line arguments (unused)
     */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to set look and feel", e);
        }

        SwingUtilities.invokeLater(() -> {
            GapFillerWizard wizard = new GapFillerWizard(null);
            wizard.buildAndShowUI();
        });
    }
}