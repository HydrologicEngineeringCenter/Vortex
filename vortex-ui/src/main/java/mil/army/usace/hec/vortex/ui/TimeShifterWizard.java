package mil.army.usace.hec.vortex.ui;

import mil.army.usace.hec.vortex.math.ShiftTimeUnit;
import mil.army.usace.hec.vortex.math.TimeShifter;
import mil.army.usace.hec.vortex.ui.util.FileSaveUtil;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.List;

public class TimeShifterWizard extends ProcessingWizard {
    private SourceFileSelectionPanel sourceFileSelectionPanel;
    private DestinationSelectionPanel destinationSelectionPanel;

    private JTextField sourceFileTextField;

    private JCheckBox startTimeCheckBox;
    private JTextField startTimeShiftTextField;
    private ShiftTimeUnitComboBox startTimeShiftUnitComboBox;

    private JCheckBox endTimeCheckBox;
    private JTextField endTimeShiftTextField;
    private ShiftTimeUnitComboBox endTimeShiftUnitComboBox;

    private JList<String> chosenSourceGridsList;

    public TimeShifterWizard(Frame frame) {
        super(frame);
    }

    @Override
    protected String getTitlePropertyKey() {
        return "TimeShifterWiz_Title";
    }

    @Override
    protected List<JPanel> createStepPanels() {
        return List.of(
                stepOnePanel(),
                stepTwoPanel(),
                stepThreePanel(),
                createProgressPanel(),
                createProgressPanel()
        );
    }

    @Override
    protected int getLastInteractiveStep() {
        return 2;
    }

    @Override
    protected boolean validateStep(int stepIndex) {
        return switch (stepIndex) {
            case 0 -> sourceFileSelectionPanel.validateInput();
            case 1 -> validateShiftConfig();
            case 2 -> validateDestination();
            default -> true;
        };
    }

    @Override
    protected void submitStep(int stepIndex) {
        if (stepIndex == 2) {
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
    }

    @Override
    protected void clearWizardState() {
        sourceFileSelectionPanel.clear();
        startTimeShiftTextField.setText("");
        destinationSelectionPanel.getDestinationTextField().setText("");
        destinationSelectionPanel.getFieldA().setText("");
        destinationSelectionPanel.getFieldB().setText("");
        destinationSelectionPanel.getFieldF().setText("");
    }

    @Override
    protected void showSaveResult() {
        String savedFile = destinationSelectionPanel.getDestinationTextField().getText();
        FileSaveUtil.showFileLocation(this, Path.of(savedFile));
    }

    private JPanel stepOnePanel() {
        sourceFileSelectionPanel = new SourceFileSelectionPanel(TimeShifterWizard.class.getName());
        sourceFileTextField = sourceFileSelectionPanel.getSourceFileTextField();
        chosenSourceGridsList = sourceFileSelectionPanel.getChosenSourceGridsList();
        return sourceFileSelectionPanel;
    }

    private JPanel stepTwoPanel() {
        JPanel setIntervalPanel = stepTwoIntervalPanel();

        GridBagLayout gridBagLayout = new GridBagLayout();
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

    private boolean validateShiftConfig() {
        if (!startTimeCheckBox.isSelected() && !endTimeCheckBox.isSelected()) {
            String message = Text.format("TimeShifterWiz_SelectTimeError");
            JOptionPane.showMessageDialog(this, message, Text.format("Error_Title"), JOptionPane.ERROR_MESSAGE);
            return false;
        }

        if (startTimeCheckBox.isSelected()) {
            String intervalText = startTimeShiftTextField.getText();
            if (intervalText.isEmpty()) {
                JOptionPane.showMessageDialog(this, Text.format("TimeShifterWiz_ShiftValueRequired"), Text.format("Error_Title"), JOptionPane.ERROR_MESSAGE);
                return false;
            }
            double interval;
            try {
                interval = Double.parseDouble(intervalText);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, Text.format("TimeShifterWiz_ParseStartInterval"), Text.format("Error_Title"), JOptionPane.ERROR_MESSAGE);
                return false;
            }
            ShiftTimeUnit timeUnit = startTimeShiftUnitComboBox.getSelected();
            if (!validateInterval(interval, timeUnit)) {
                JOptionPane.showMessageDialog(this, Text.format("TimeShifterWiz_StartShiftNotEvenSeconds"),
                        Text.format("Error_Title"), JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }

        if (endTimeCheckBox.isSelected()) {
            String intervalText = endTimeShiftTextField.getText();
            if (intervalText.isEmpty()) {
                JOptionPane.showMessageDialog(this, Text.format("TimeShifterWiz_ShiftValueRequired"), Text.format("Error_Title"), JOptionPane.ERROR_MESSAGE);
                return false;
            }
            double interval;
            try {
                interval = Double.parseDouble(intervalText);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, Text.format("TimeShifterWiz_ParseEndInterval"), Text.format("Error_Title"), JOptionPane.ERROR_MESSAGE);
                return false;
            }
            ShiftTimeUnit timeUnit = endTimeShiftUnitComboBox.getSelected();
            if (!validateInterval(interval, timeUnit)) {
                JOptionPane.showMessageDialog(this, Text.format("TimeShifterWiz_EndShiftNotEvenSeconds"),
                        Text.format("Error_Title"), JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }

        return true;
    }

    private JPanel stepTwoIntervalPanel() {
        startTimeCheckBox = new JCheckBox(Text.format("TimeShifterWiz_ShiftStart_L"));
        startTimeCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        startTimeCheckBox.setSelected(true);

        JPanel startTimeCheckBoxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        startTimeCheckBoxPanel.add(startTimeCheckBox);

        JPanel startTimeShiftPanel = startTimeShiftPanel();
        startTimeCheckBox.addActionListener(e -> startTimeShiftPanel.setVisible(startTimeCheckBox.isSelected()));

        endTimeCheckBox = new JCheckBox(Text.format("TimeShifterWiz_EndStart_L"));
        endTimeCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        endTimeCheckBox.setSelected(true);

        JPanel endTimeCheckBoxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        endTimeCheckBoxPanel.add(endTimeCheckBox);

        JPanel endTimeShiftPanel = endTimeShiftPanel();
        endTimeCheckBox.addActionListener(e -> endTimeShiftPanel.setVisible(endTimeCheckBox.isSelected()));

        JPanel intervalPanel = new JPanel();
        intervalPanel.setLayout(new BoxLayout(intervalPanel, BoxLayout.Y_AXIS));
        intervalPanel.add(startTimeCheckBoxPanel);
        intervalPanel.add(startTimeShiftPanel);
        intervalPanel.add(endTimeCheckBoxPanel);
        intervalPanel.add(endTimeShiftPanel);

        return intervalPanel;
    }

    private JPanel startTimeShiftPanel() {
        JLabel startTimeShiftLabel = new JLabel(Text.format("TimeShifterWiz_StartTimeShift_L"));
        startTimeShiftTextField = new JTextField(25);
        startTimeShiftUnitComboBox = new ShiftTimeUnitComboBox();
        startTimeShiftUnitComboBox.setEnabled(true);

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(startTimeShiftLabel);
        panel.add(Box.createRigidArea(new Dimension(2,0)));
        panel.add(startTimeShiftTextField);
        panel.add(Box.createRigidArea(new Dimension(2,0)));
        panel.add(startTimeShiftUnitComboBox);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));

        return panel;
    }

    private JPanel endTimeShiftPanel() {
        JLabel endTimeShiftLabel = new JLabel(Text.format("TimeShifterWiz_EndTimeShift_L"));
        endTimeShiftTextField = new JTextField(25);
        endTimeShiftUnitComboBox = new ShiftTimeUnitComboBox();
        endTimeShiftUnitComboBox.setEnabled(true);

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(endTimeShiftLabel);
        panel.add(Box.createRigidArea(new Dimension(2,0)));
        panel.add(endTimeShiftTextField);
        panel.add(Box.createRigidArea(new Dimension(2,0)));
        panel.add(endTimeShiftUnitComboBox);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));

        return panel;
    }

    private JPanel stepThreePanel() {
        destinationSelectionPanel = new DestinationSelectionPanel(this);
        return destinationSelectionPanel;
    }

    private boolean validateDestination() {
        String destinationFile = destinationSelectionPanel.getDestinationTextField().getText();
        if(destinationFile == null || destinationFile.isEmpty() ) {
            JOptionPane.showMessageDialog(this, Text.format("Error_DestinationRequired"),
                    Text.format("Error_MissingField_Title"), JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    private void timeShifterTask() {
        String pathToSource = sourceFileTextField.getText();
        List<String> chosenSourceGrids = getItemsInList(chosenSourceGridsList);
        if (chosenSourceGrids == null) return;
        Set<String> sourceGrids = new HashSet<>(chosenSourceGrids);

        Duration startTimeShift = getStartTimeShift();
        Duration endTimeShift = getEndTimeShift();

        List<String> chosenSourceList = getItemsInList(chosenSourceGridsList);
        if(chosenSourceList == null) return;
        Map<String, String> writeOptions = DssWriteOptionsBuilder.buildWriteOptions(chosenSourceList, destinationSelectionPanel);

        TimeShifter shift = TimeShifter.builder()
                .pathToFile(pathToSource)
                .grids(sourceGrids)
                .shiftStart(startTimeShift)
                .shiftEnd(endTimeShift)
                .destination(destinationSelectionPanel.getDestinationTextField().getText())
                .writeOptions(writeOptions)
                .build();

        shift.addPropertyChangeListener(createProgressListener());

        SwingWorker<Void, Void> task = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                shift.run();
                return null;
            }

            @Override
            protected void done() {
                setButtonsForRestartOrClose();
            }
        };

        task.execute();
    }

    private Duration getStartTimeShift() {
        if (!startTimeCheckBox.isSelected()) return Duration.ZERO;
        try {
            double value = Float.parseFloat(startTimeShiftTextField.getText());
            ShiftTimeUnit timeUnit = startTimeShiftUnitComboBox.getSelected();
            long seconds = (long) (value * timeUnit.toSeconds());
            return Duration.ofSeconds(seconds);
        } catch (Exception e) {
            return Duration.ZERO;
        }
    }

    private Duration getEndTimeShift() {
        if (!endTimeCheckBox.isSelected()) return Duration.ZERO;
        try {
            double value = Float.parseFloat(endTimeShiftTextField.getText());
            ShiftTimeUnit timeUnit = endTimeShiftUnitComboBox.getSelected();
            long seconds = (long) (value * timeUnit.toSeconds());
            return Duration.ofSeconds(seconds);
        } catch (Exception e) {
            return Duration.ZERO;
        }
    }

    private boolean validateInterval(double value, ShiftTimeUnit timeUnit) {
        int toSeconds = timeUnit.toSeconds();
        double doubleValue = value * toSeconds;
        long longValue = (long) doubleValue;
        return Double.compare(longValue, doubleValue) == 0;
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) { e.printStackTrace(); }

        TimeShifterWizard sanitizerWizard = new TimeShifterWizard(null);
        sanitizerWizard.buildAndShowUI();
    }
}
