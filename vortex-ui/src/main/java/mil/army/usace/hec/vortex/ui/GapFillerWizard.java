package mil.army.usace.hec.vortex.ui;

import mil.army.usace.hec.vortex.VortexProperty;
import mil.army.usace.hec.vortex.math.BatchGapFiller;
import mil.army.usace.hec.vortex.math.GapFillMethod;
import mil.army.usace.hec.vortex.ui.util.FileSaveUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GapFillerWizard extends ProcessingWizard {
    private static final Logger LOGGER = Logger.getLogger(GapFillerWizard.class.getName());

    private static final int PAD = 2;
    private static final int BORDER_PAD = 5;
    private static final int ROW_HEIGHT = (int) new JTextField().getPreferredSize().getHeight();
    private static final Dimension PANEL_DIMENSION = new Dimension(Integer.MAX_VALUE, (ROW_HEIGHT + 2 * PAD) * 2);

    private static final String FOCAL_MEAN_LABEL = TextProperties.INSTANCE.getProperty("GapFillerWiz_FocalMean_L");
    private static final String LINEAR_INTERP_LABEL = TextProperties.INSTANCE.getProperty("GapFillerWiz_LinearInterp_L");
    private static final String INSERT_TIME_STEPS_LABEL = TextProperties.INSTANCE.getProperty("GapFillerWiz_TimeSteps_L");

    private SourceFileSelectionPanel sourceFileSelectionPanel;
    private DestinationSelectionPanel destinationSelectionPanel;

    private JTextField sourceFileTextField;
    private JList<String> chosenSourceGridsList;
    private ButtonGroup methodButtonGroup;
    private JCheckBox insertTimeStepsCheckBox;

    public GapFillerWizard(Frame frame) {
        super(frame);
    }

    @Override
    protected String getTitlePropertyKey() {
        return "GapFillerWiz_Title";
    }

    @Override
    protected List<JPanel> createStepPanels() {
        return List.of(
                stepOnePanel(),
                stepTwoPanel(),
                stepThreePanel(),
                stepFourPanel(),
                createProgressPanel(),
                createProgressPanel()
        );
    }

    @Override
    protected int getLastInteractiveStep() {
        return 3;
    }

    @Override
    protected boolean validateStep(int stepIndex) {
        return switch (stepIndex) {
            case 0 -> sourceFileSelectionPanel.validateInput();
            case 1 -> validateMethod();
            case 3 -> validateDestination();
            default -> true;
        };
    }

    @Override
    protected void submitStep(int stepIndex) {
        if (stepIndex == 3) {
            SwingWorker<Void, Void> task = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    gapFillTask();
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        nextAction();
                    } catch (InterruptedException | ExecutionException e) {
                        if (e instanceof InterruptedException) {
                            Thread.currentThread().interrupt();
                        }
                        LOGGER.log(Level.SEVERE, "Error during processing", e);
                        String title = TextProperties.INSTANCE.getProperty("GapFillerWiz_ProcessingError_Title");
                        String message = TextProperties.INSTANCE.getProperty("GapFillerWiz_ProcessingError_Message");
                        SwingUtilities.invokeLater(() ->
                                JOptionPane.showMessageDialog(GapFillerWizard.this, message + " " + e.getMessage(), title, JOptionPane.ERROR_MESSAGE)
                        );
                    }
                }
            };
            task.execute();
        }
    }

    @Override
    protected void clearWizardState() {
        sourceFileSelectionPanel.clear();
        methodButtonGroup.clearSelection();
        insertTimeStepsCheckBox.setSelected(false);
        destinationSelectionPanel.getDestinationTextField().setText("");
        destinationSelectionPanel.getFieldA().setText("");
        destinationSelectionPanel.getFieldB().setText("");
        destinationSelectionPanel.getFieldF().setText("");
    }

    @Override
    protected void showSaveResult() {
        String savedFile = destinationSelectionPanel.getDestinationTextField().getText();
        if (savedFile != null && !savedFile.isEmpty()) {
            FileSaveUtil.showFileLocation(this, Path.of(savedFile));
        }
    }

    private JPanel stepOnePanel() {
        sourceFileSelectionPanel = new SourceFileSelectionPanel(CalculatorWizard.class.getName());
        sourceFileTextField = sourceFileSelectionPanel.getSourceFileTextField();
        chosenSourceGridsList = sourceFileSelectionPanel.getChosenSourceGridsList();
        return sourceFileSelectionPanel;
    }

    private JPanel stepTwoPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(createInterpolationExplanationPanel());
        panel.add(createSpatialFillPanel());
        panel.add(createTemporalFillPanel());
        panel.setBorder(BorderFactory.createEmptyBorder(BORDER_PAD, BORDER_PAD, BORDER_PAD, BORDER_PAD));
        return panel;
    }

    private JPanel createInterpolationExplanationPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel selectInterpolationLabel = new JLabel(TextProperties.INSTANCE.getProperty("GapFillerWiz_SpatialFill_Select_L"));
        selectInterpolationLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        selectInterpolationLabel.setBorder(new EmptyBorder(PAD, PAD, PAD, PAD));

        JLabel selectInterpolationDescLabel = new JLabel(TextProperties.INSTANCE.getProperty("GapFillerWiz_SpatialFill_Select_Desc"));
        selectInterpolationDescLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        selectInterpolationDescLabel.setBorder(new EmptyBorder(PAD, 4 * PAD, PAD, PAD));

        panel.setMinimumSize(PANEL_DIMENSION);
        panel.setMaximumSize(PANEL_DIMENSION);
        panel.setPreferredSize(PANEL_DIMENSION);

        panel.add(selectInterpolationLabel);
        panel.add(selectInterpolationDescLabel);

        return panel;
    }

    private JPanel createSpatialFillPanel() {
        JPanel spatialFillPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        String spatialFillTitle = TextProperties.INSTANCE.getProperty("GapFillerWiz_SpatialFill_Title");
        String spatialFillTT = TextProperties.INSTANCE.getProperty("GapFillerWiz_SpatialFill_TT");
        spatialFillPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), spatialFillTitle));
        spatialFillPanel.setToolTipText(spatialFillTT);

        JRadioButton focalMeanButton = new JRadioButton(FOCAL_MEAN_LABEL);
        focalMeanButton.setToolTipText(spatialFillTT);

        if (methodButtonGroup == null) {
            methodButtonGroup = new ButtonGroup();
        }
        methodButtonGroup.add(focalMeanButton);
        spatialFillPanel.add(focalMeanButton);

        return spatialFillPanel;
    }

    private JPanel createTemporalFillPanel() {
        JPanel temporalFillPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        String temporalFillTitle = TextProperties.INSTANCE.getProperty("GapFillerWiz_TemporalFill_Title");
        String temporalFillTT = TextProperties.INSTANCE.getProperty("GapFillerWiz_TemporalFill_TT");
        temporalFillPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), temporalFillTitle));
        temporalFillPanel.setToolTipText(temporalFillTT);

        JRadioButton linearInterpButton = new JRadioButton(LINEAR_INTERP_LABEL);
        linearInterpButton.setToolTipText(temporalFillTT);
        methodButtonGroup.add(linearInterpButton);
        temporalFillPanel.add(linearInterpButton);

        return temporalFillPanel;
    }

    private boolean validateMethod() {
        if (getSelectedButtonText(methodButtonGroup) == null) {
            String title = TextProperties.INSTANCE.getProperty("GapFillerWiz_NoMethodSelected_Title");
            String message = TextProperties.INSTANCE.getProperty("GapFillerWiz_NoMethodSelected_Error");
            JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    private JPanel stepThreePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        panel.add(createTimeStepExplanationPanel());
        panel.add(Box.createVerticalStrut(10));

        String timeStepsTT = TextProperties.INSTANCE.getProperty("GapFillerWiz_TimeSteps_TT");
        insertTimeStepsCheckBox = new JCheckBox(INSERT_TIME_STEPS_LABEL);
        insertTimeStepsCheckBox.setToolTipText(timeStepsTT);
        insertTimeStepsCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(insertTimeStepsCheckBox);
        panel.setToolTipText(timeStepsTT);
        panel.setBorder(BorderFactory.createEmptyBorder(BORDER_PAD, BORDER_PAD, BORDER_PAD, BORDER_PAD));

        return panel;
    }

    private JPanel createTimeStepExplanationPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel selectAddMissingLabel = new JLabel(TextProperties.INSTANCE.getProperty("GapFillerWiz_TimeSteps_Option_L"));
        selectAddMissingLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        selectAddMissingLabel.setBorder(new EmptyBorder(PAD, PAD, PAD, PAD));

        JLabel selectAddMissingDescLabel = new JLabel(TextProperties.INSTANCE.getProperty("GapFillerWiz_TimeSteps_Option_Desc"));
        selectAddMissingDescLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        selectAddMissingDescLabel.setBorder(new EmptyBorder(PAD, 4 * PAD, PAD, PAD));

        panel.add(selectAddMissingLabel);
        panel.add(selectAddMissingDescLabel);

        return panel;
    }

    private JPanel stepFourPanel() {
        destinationSelectionPanel = new DestinationSelectionPanel(this);
        return destinationSelectionPanel;
    }

    private boolean validateDestination() {
        String destinationFile = destinationSelectionPanel.getDestinationTextField().getText();
        if (destinationFile == null || destinationFile.isEmpty()) {
            String title = TextProperties.INSTANCE.getProperty("GapFillerWiz_NoDestination_Title");
            String message = TextProperties.INSTANCE.getProperty("GapFillerWiz_NoDestination_Error");
            JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    private void gapFillTask() {
        try {
            String pathToSource = sourceFileTextField.getText();
            List<String> chosenSourceGrids = getItemsInList(chosenSourceGridsList);
            if (chosenSourceGrids == null || chosenSourceGrids.isEmpty()) {
                progressMessagePanel.write("No source grids selected");
                return;
            }

            Set<String> sourceGrids = new HashSet<>(chosenSourceGrids);
            String destination = destinationSelectionPanel.getDestinationTextField().getText();

            Map<String, String> writeOptions = DssWriteOptionsBuilder.buildWriteOptions(chosenSourceGrids, destinationSelectionPanel);

            String selection = getSelectedButtonText(methodButtonGroup);
            GapFillMethod method = fromString(selection);

            List<Runnable> runnables = createGapFillers(pathToSource, sourceGrids, method, destination, writeOptions);
            executeGapFillers(runnables);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during processing", e);
            progressMessagePanel.write("ERROR: " + e.getMessage());
        }
    }

    private List<Runnable> createGapFillers(String source, Set<String> variables,
                                            GapFillMethod method, String destination,
                                            Map<String, String> writeOptions) {
        List<Runnable> runnables = new ArrayList<>();

        BatchGapFiller batchGapFiller = BatchGapFiller.builder()
                .source(source)
                .variables(new ArrayList<>(variables))
                .method(method)
                .destination(destination)
                .writeOptions(writeOptions)
                .build();

        batchGapFiller.addPropertyChangeListener(this::handlePropertyChange);
        runnables.add(batchGapFiller);

        if (insertTimeStepsCheckBox.isSelected()) {
            BatchGapFiller timeStepFiller = BatchGapFiller.builder()
                    .source(destination)
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

    private void handlePropertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equalsIgnoreCase(VortexProperty.PROGRESS.toString())) {
            if (evt.getNewValue() instanceof Integer progressValue) {
                progressMessagePanel.setValue(progressValue);
            }
        } else {
            Object obj = evt.getNewValue();
            if (obj == null) return;
            progressMessagePanel.write(String.valueOf(obj));
        }
    }

    private void executeGapFillers(List<Runnable> runnables) {
        SwingWorker<Void, Void> task = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                runnables.forEach(Runnable::run);
                return null;
            }

            @Override
            protected void done() {
                setButtonsForRestartOrClose();
                progressMessagePanel.setValue(100);
            }
        };
        task.execute();
    }

    private static String getSelectedButtonText(ButtonGroup group) {
        for (Enumeration<AbstractButton> buttons = group.getElements(); buttons.hasMoreElements(); ) {
            AbstractButton button = buttons.nextElement();
            if (button.isSelected()) {
                return button.getText();
            }
        }
        return null;
    }

    private static GapFillMethod fromString(String str) {
        if (FOCAL_MEAN_LABEL.equals(str)) return GapFillMethod.FOCAL_MEAN;
        if (LINEAR_INTERP_LABEL.equals(str)) return GapFillMethod.LINEAR_INTERPOLATION;
        return GapFillMethod.UNDEFINED;
    }

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
