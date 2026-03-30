package mil.army.usace.hec.vortex.ui;

import mil.army.usace.hec.vortex.math.BatchTimeStepResampler;
import mil.army.usace.hec.vortex.math.TimeStep;
import mil.army.usace.hec.vortex.ui.util.FileSaveUtil;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

public class TimeStepResamplerWizard extends ProcessingWizard {
    private SourceFileSelectionPanel sourceFileSelectionPanel;
    private DestinationSelectionPanel destinationSelectionPanel;

    private JTextField sourceFileTextField;
    private JList<String> chosenSourceGridsList;
    private TimeStepComboBox timeStepComboBox;

    public TimeStepResamplerWizard(Frame frame) {
        super(frame);
    }

    @Override
    protected String getTitlePropertyKey() {
        return "TimeStepResamplerWiz_Title";
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
            case 1 -> validateTimeStep();
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
    }

    @Override
    protected void clearWizardState() {
        sourceFileSelectionPanel.clear();
        timeStepComboBox.setSelectedIndex(10);
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
        sourceFileSelectionPanel = new SourceFileSelectionPanel(TimeStepResamplerWizard.class.getName());
        sourceFileTextField = sourceFileSelectionPanel.getSourceFileTextField();
        chosenSourceGridsList = sourceFileSelectionPanel.getChosenSourceGridsList();
        return sourceFileSelectionPanel;
    }

    private JPanel stepTwoPanel() {
        JLabel instructionLabel = new JLabel(Text.format("TimeStepResamplerWiz_Instruction_L"));
        JPanel instructionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        instructionPanel.add(instructionLabel);

        JLabel timeStepLabel = new JLabel(Text.format("TimeStepResamplerWiz_TimeStep_L"));
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

    private boolean validateTimeStep() {
        if (timeStepComboBox.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, Text.format("TimeStepResamplerWiz_TimeStepRequired"),
                    Text.format("Error_Title"), JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    private JPanel stepThreePanel() {
        destinationSelectionPanel = new DestinationSelectionPanel(this);
        return destinationSelectionPanel;
    }

    private boolean validateDestination() {
        String destinationFile = destinationSelectionPanel.getDestinationTextField().getText();
        if (destinationFile == null || destinationFile.isEmpty()) {
            JOptionPane.showMessageDialog(this, Text.format("Error_DestinationRequired"),
                    Text.format("Error_MissingField_Title"), JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    private void timeStepResamplerTask() {
        String pathToSource = sourceFileTextField.getText();
        List<String> chosenSourceList = getItemsInList(chosenSourceGridsList);
        if (chosenSourceList == null) return;
        Set<String> sourceGrids = new HashSet<>(chosenSourceList);

        TimeStep selectedTimeStep = timeStepComboBox.getSelected();
        String destination = destinationSelectionPanel.getDestinationTextField().getText();

        Map<String, String> writeOptions = DssWriteOptionsBuilder.buildWriteOptions(chosenSourceList, destinationSelectionPanel);

        BatchTimeStepResampler batchTimeStepResampler = BatchTimeStepResampler.builder()
                .pathToInput(pathToSource)
                .variables(sourceGrids)
                .timeStep(selectedTimeStep)
                .destination(destination)
                .writeOptions(writeOptions)
                .build();

        batchTimeStepResampler.addPropertyChangeListener(evt -> SwingUtilities.invokeLater(() -> {
            var property = mil.army.usace.hec.vortex.VortexProperty.parse(evt.getPropertyName());
            if (mil.army.usace.hec.vortex.VortexProperty.PROGRESS == property) {
                if (!(evt.getNewValue() instanceof Integer)) return;
                progressMessagePanel.setValue((int) evt.getNewValue());
            } else {
                progressMessagePanel.write(String.valueOf(evt.getNewValue()));
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
