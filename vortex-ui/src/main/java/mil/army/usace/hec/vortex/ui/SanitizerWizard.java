package mil.army.usace.hec.vortex.ui;

import mil.army.usace.hec.vortex.math.BatchSanitizer;
import mil.army.usace.hec.vortex.ui.util.FileSaveUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

public class SanitizerWizard extends ProcessingWizard {
    private SourceFileSelectionPanel sourceFileSelectionPanel;
    private DestinationSelectionPanel destinationSelectionPanel;

    private JTextField sourceFileTextField;
    private JCheckBox lowerThresholdCheckBox;
    private JCheckBox upperThresholdCheckBox;
    private JTextField lowerThresholdTextField;
    private JTextField lowerReplacementTextField;
    private JTextField upperThresholdTextField;
    private JTextField upperReplacementTextField;
    private JList<String> chosenSourceGridsList;

    public SanitizerWizard(Frame frame) {
        super(frame);
    }

    @Override
    protected String getTitlePropertyKey() {
        return "SanitizerWiz_Title";
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
            case 1 -> validateThresholds();
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
    }

    @Override
    protected void clearWizardState() {
        sourceFileSelectionPanel.clear();
        lowerThresholdTextField.setText("");
        lowerReplacementTextField.setText("");
        upperThresholdTextField.setText("");
        upperReplacementTextField.setText("");
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
        sourceFileSelectionPanel = new SourceFileSelectionPanel(CalculatorWizard.class.getName());
        sourceFileTextField = sourceFileSelectionPanel.getSourceFileTextField();
        chosenSourceGridsList = sourceFileSelectionPanel.getChosenSourceGridsList();
        return sourceFileSelectionPanel;
    }

    private JPanel stepTwoPanel() {
        JPanel selectBelowThresholdPanel = stepTwoSanitizeValuesBelowPanel();
        JPanel selectAboveThresholdPanel = stepTwoSanitizeValuesAbovePanel();

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
        stepTwoPanel.add(selectBelowThresholdPanel, gridBagConstraints);

        gridBagConstraints.gridy = 1;
        stepTwoPanel.add(selectAboveThresholdPanel, gridBagConstraints);

        gridBagConstraints.gridy = 2;
        gridBagConstraints.weighty = 1;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        stepTwoPanel.add(new JPanel(), gridBagConstraints);

        return stepTwoPanel;
    }

    private boolean validateThresholds() {
        if (lowerThresholdCheckBox.isSelected()) {
            try {
                Double.parseDouble(lowerThresholdTextField.getText());
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

    private JPanel stepTwoSanitizeValuesBelowPanel() {
        lowerThresholdCheckBox = new JCheckBox(TextProperties.getInstance().getProperty("SanitizerWiz_LowerThresholdCheckbox_L"),false);

        JLabel replaceLowerValuesLabel = new JLabel(TextProperties.getInstance().getProperty("SanitizerWiz_LowerThreshold_L"));
        replaceLowerValuesLabel.setBorder(new EmptyBorder(0,15,0,0));
        lowerThresholdTextField = new JTextField(16);

        JPanel replaceLowerValuesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        replaceLowerValuesPanel.add(replaceLowerValuesLabel);
        replaceLowerValuesPanel.add(Box.createRigidArea(new Dimension(15,0)));
        replaceLowerValuesPanel.add(lowerThresholdTextField);

        JLabel replacementValueLabel = new JLabel(TextProperties.getInstance().getProperty("SanitizerWiz_LowerReplacement_L"));
        replacementValueLabel.setBorder(new EmptyBorder(0,15,0,0));
        lowerReplacementTextField = new JTextField(16);

        JPanel replacementLowerValuePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        replacementLowerValuePanel.add(replacementValueLabel);
        replacementLowerValuePanel.add(Box.createRigidArea(new Dimension(68,0)));
        replacementLowerValuePanel.add(lowerReplacementTextField);

        JPanel lowerTextFieldsPanel = new JPanel();
        lowerTextFieldsPanel.setLayout(new BoxLayout(lowerTextFieldsPanel, BoxLayout.Y_AXIS));
        lowerTextFieldsPanel.add(replaceLowerValuesPanel);
        lowerTextFieldsPanel.add(replacementLowerValuePanel);
        lowerTextFieldsPanel.setBorder(BorderFactory.createEmptyBorder(0, -5, 0, 0));
        lowerTextFieldsPanel.setVisible(false);

        lowerThresholdCheckBox.addActionListener(e -> lowerTextFieldsPanel.setVisible(lowerThresholdCheckBox.isSelected()));

        JPanel valuesLowerPanel = new JPanel(new BorderLayout());
        valuesLowerPanel.add(lowerThresholdCheckBox, BorderLayout.NORTH);
        valuesLowerPanel.add(lowerTextFieldsPanel, BorderLayout.CENTER);

        return valuesLowerPanel;
    }

    private JPanel stepTwoSanitizeValuesAbovePanel() {
        upperThresholdCheckBox = new JCheckBox(TextProperties.getInstance().getProperty("SanitizerWiz_UpperThresholdCheckbox_L"));

        JLabel replaceUpperValuesLabel = new JLabel(TextProperties.getInstance().getProperty("SanitizerWiz_UpperThreshold_L"));
        replaceUpperValuesLabel.setBorder(new EmptyBorder(0,15,0,0));
        upperThresholdTextField = new JTextField(16);

        JPanel replaceUpperValuesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        replaceUpperValuesPanel.add(replaceUpperValuesLabel);
        replaceUpperValuesPanel.add(Box.createRigidArea(new Dimension(15,0)));
        replaceUpperValuesPanel.add(upperThresholdTextField);

        JLabel replacementValueLabel = new JLabel(TextProperties.getInstance().getProperty("SanitizerWiz_UpperReplacement_L"));
        replacementValueLabel.setBorder(new EmptyBorder(0,15,0,0));
        upperReplacementTextField = new JTextField(16);

        JPanel replacementUpperValuePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        replacementUpperValuePanel.add(replacementValueLabel);
        replacementUpperValuePanel.add(Box.createRigidArea(new Dimension(86,0)));
        replacementUpperValuePanel.add(upperReplacementTextField);

        JPanel upperTextFieldsPanel = new JPanel();
        upperTextFieldsPanel.setLayout(new BoxLayout(upperTextFieldsPanel, BoxLayout.Y_AXIS));
        upperTextFieldsPanel.add(replaceUpperValuesPanel);
        upperTextFieldsPanel.add(replacementUpperValuePanel);
        upperTextFieldsPanel.setBorder(BorderFactory.createEmptyBorder(0, -5, 0, 0));
        upperTextFieldsPanel.setVisible(false);

        upperThresholdCheckBox.addActionListener(e -> upperTextFieldsPanel.setVisible(upperThresholdCheckBox.isSelected()));

        JPanel valuesUpperPanel = new JPanel(new BorderLayout());
        valuesUpperPanel.add(upperThresholdCheckBox, BorderLayout.NORTH);
        valuesUpperPanel.add(upperTextFieldsPanel, BorderLayout.CENTER);

        return valuesUpperPanel;
    }

    private JPanel stepThreePanel() {
        destinationSelectionPanel = new DestinationSelectionPanel(this);
        return destinationSelectionPanel;
    }

    private boolean validateDestination() {
        String destinationFile = destinationSelectionPanel.getDestinationTextField().getText();
        if(destinationFile == null || destinationFile.isEmpty() ) {
            JOptionPane.showMessageDialog(this, "Destination file is required.",
                    "Error: Missing Field", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
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

        List<String> chosenSourceList = getItemsInList(chosenSourceGridsList);
        if(chosenSourceList == null) return;
        Map<String, String> writeOptions = DssWriteOptionsBuilder.buildWriteOptions(chosenSourceList, destinationSelectionPanel);

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

        batchSanitizer.addPropertyChangeListener(createProgressListener());

        SwingWorker<Void, Void> task = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                batchSanitizer.run();
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

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) { e.printStackTrace(); }

        SanitizerWizard sanitizerWizard = new SanitizerWizard(null);
        sanitizerWizard.buildAndShowUI();
    }
}
