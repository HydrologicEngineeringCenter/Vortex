package mil.army.usace.hec.vortex.ui;

import mil.army.usace.hec.vortex.geo.BatchSubsetter;
import mil.army.usace.hec.vortex.ui.util.FileSaveUtil;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

public class ClipperWizard extends ProcessingWizard {
    private SourceFileSelectionPanel sourceFileSelectionPanel;
    private DestinationSelectionPanel destinationSelectionPanel;

    private JTextField dataSourceTextField;
    private JTextField sourceFileTextField;
    private JList<String> chosenSourceGridsList;

    public ClipperWizard(Frame frame) {
        super(frame);
    }

    @Override
    protected String getTitlePropertyKey() {
        return "ClipperWiz_Title";
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
                    clipperTask();
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
        dataSourceTextField.setText("");
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
        sourceFileSelectionPanel = new SourceFileSelectionPanel(ClipperWizard.class.getName());
        sourceFileTextField = sourceFileSelectionPanel.getSourceFileTextField();
        chosenSourceGridsList = sourceFileSelectionPanel.getChosenSourceGridsList();
        return sourceFileSelectionPanel;
    }

    private JPanel stepTwoPanel() {
        GridBagLayout gridBagLayout = new GridBagLayout();
        gridBagLayout.columnWidths = new int[]{this.getPreferredSize().width, 0};
        gridBagLayout.rowHeights = new int[]{50, 100, 50, 50, 0};
        gridBagLayout.columnWeights = new double[]{1.0, Double.MIN_VALUE};
        gridBagLayout.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};

        JPanel stepTwoPanel = new JPanel(gridBagLayout);
        stepTwoPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,7));

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(0, 0, 5, 0);
        gridBagConstraints.gridx = 0;

        gridBagConstraints.gridy = 0;
        stepTwoPanel.add(dataSourceSectionPanel(), gridBagConstraints);

        return stepTwoPanel;
    }

    private JPanel dataSourceSectionPanel() {
        JLabel dataSourceLabel = new JLabel(TextProperties.getInstance().getProperty("ClipperWizClippingDatasourceL"));
        JPanel dataSourceLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        dataSourceLabelPanel.add(dataSourceLabel);

        JPanel dataSourceTextFieldPanel = new JPanel();
        dataSourceTextFieldPanel.setLayout(new BoxLayout(dataSourceTextFieldPanel, BoxLayout.X_AXIS));
        dataSourceTextFieldPanel.add(Box.createRigidArea(new Dimension(4,0)));

        dataSourceTextField = new JTextField();
        dataSourceTextFieldPanel.add(dataSourceTextField);
        dataSourceTextFieldPanel.add(Box.createRigidArea(new Dimension(8,0)));

        FileBrowseButton dataSourceBrowseButton = new FileBrowseButton(this.getClass().getName(), "");
        dataSourceBrowseButton.setIcon(IconResources.loadIcon("images/Open16.gif"));
        dataSourceBrowseButton.setPreferredSize(new Dimension(22,22));
        dataSourceBrowseButton.addActionListener(evt -> dataSourceBrowseAction(dataSourceBrowseButton));
        dataSourceTextFieldPanel.add(dataSourceBrowseButton);

        JPanel dataSourceSectionPanel = new JPanel();
        dataSourceSectionPanel.setLayout(new BoxLayout(dataSourceSectionPanel, BoxLayout.Y_AXIS));
        dataSourceSectionPanel.add(dataSourceLabelPanel);
        dataSourceSectionPanel.add(dataSourceTextFieldPanel);

        return dataSourceSectionPanel;
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

    private void clipperTask() {
        String pathToSource = sourceFileTextField.getText();
        List<String> sourceGrids = getItemsInList(chosenSourceGridsList);
        if (sourceGrids == null) return;

        String clippingDatasource = dataSourceTextField.getText();

        List<String> chosenSourceList = getItemsInList(chosenSourceGridsList);
        if(chosenSourceList == null) return;
        Map<String, String> writeOptions = DssWriteOptionsBuilder.buildWriteOptions(chosenSourceList, destinationSelectionPanel);

        BatchSubsetter batchSubsetter = BatchSubsetter.builder()
                .pathToInput(pathToSource)
                .variables(sourceGrids)
                .setEnvelopeDataSource(clippingDatasource)
                .destination(destinationSelectionPanel.getDestinationTextField().getText())
                .writeOptions(writeOptions)
                .build();

        batchSubsetter.addPropertyChangeListener(createProgressListener());

        SwingWorker<Void, Void> task = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                batchSubsetter.run();
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

    private void dataSourceBrowseAction(FileBrowseButton fileBrowseButton) {
        JFileChooser fileChooser = new JFileChooser(fileBrowseButton.getPersistedBrowseLocation());
        fileChooser.setAcceptAllFileFilterUsed(false);
        FileNameExtensionFilter acceptableExtension = new FileNameExtensionFilter("Shapefiles (*.shp)", "shp");
        fileChooser.addChoosableFileFilter(acceptableExtension);

        int userChoice = fileChooser.showOpenDialog(this);

        if(userChoice == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            dataSourceTextField.setText(selectedFile.getAbsolutePath());
            fileBrowseButton.setPersistedBrowseLocation(selectedFile);
        }
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) { e.printStackTrace(); }

        ClipperWizard clipperWizard = new ClipperWizard(null);
        clipperWizard.buildAndShowUI();
    }
}
