package mil.army.usace.hec.vortex.ui;

import mil.army.usace.hec.vortex.geo.ResamplingMethod;
import mil.army.usace.hec.vortex.math.BatchCalculator;
import mil.army.usace.hec.vortex.math.BatchGridCalculator;
import mil.army.usace.hec.vortex.math.Operation;
import mil.army.usace.hec.vortex.ui.util.FileSaveUtil;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class CalculatorWizard extends ProcessingWizard {
    private static final String ERROR_TITLE = "Error";

    private SourceFileSelectionPanel sourceFileSelectionPanel;
    private JPanel constantOrRasterSelectionPanel;
    private JPanel calculationSelectionPanel;
    private DestinationSelectionPanel destinationSelectionPanel;

    private JRadioButton constantRadioButton;
    private JTextField sourceFileTextField;
    private JTextField multiplyTextField;
    private JTextField divideTextField;
    private JTextField addTextField;
    private JTextField subtractTextField;
    private OperationComboBox operationComboBox;
    private JTextField rasterTextField;
    private ResamplingMethodSelectionPanel resamplingPanel;
    private JList<String> chosenSourceGridsList;

    public CalculatorWizard(Frame frame) {
        super(frame);
    }

    @Override
    protected String getTitlePropertyKey() {
        return "CalculatorWiz_Title";
    }

    @Override
    protected List<JPanel> createStepPanels() {
        initSourceFileSelectionPanel();
        initConstantOrRasterSelectionPanel();
        initCalculationSelectionPanel();
        initDestinationSelectionPanel();

        JPanel progressPanel = createProgressPanel();
        JPanel completedPanel = createProgressPanel();

        return List.of(
                sourceFileSelectionPanel,
                constantOrRasterSelectionPanel,
                calculationSelectionPanel,
                destinationSelectionPanel,
                progressPanel,
                completedPanel
        );
    }

    @Override
    protected int getLastInteractiveStep() {
        return 3;
    }

    @Override
    protected boolean validateStep(int stepIndex) {
        return switch (stepIndex) {
            case 0 -> validateStepOne();
            case 2 -> validateCalculationParameters();
            case 3 -> validateStepFour();
            default -> true;
        };
    }

    @Override
    protected void submitStep(int stepIndex) {
        switch (stepIndex) {
            case 1 -> submitStepTwo();
            case 3 -> submitStepFour();
            default -> { /* no-op */ }
        }
    }

    @Override
    protected void clearWizardState() {
        sourceFileSelectionPanel.clear();

        multiplyTextField.setText("");
        divideTextField.setText("");
        addTextField.setText("");
        subtractTextField.setText("");

        operationComboBox.refresh();
        rasterTextField.setText("");
        resamplingPanel.refresh();

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

    /* Step 1: Source File Selection */
    private void initSourceFileSelectionPanel() {
        sourceFileSelectionPanel = new SourceFileSelectionPanel(CalculatorWizard.class.getName());
        sourceFileTextField = sourceFileSelectionPanel.getSourceFileTextField();
        chosenSourceGridsList = sourceFileSelectionPanel.getChosenSourceGridsList();
    }

    private boolean validateStepOne() {
        return sourceFileSelectionPanel.validateInput();
    }

    /* Step 2: Constant or Raster Selection */
    private void initConstantOrRasterSelectionPanel() {
        JLabel calcTypeLabel = new JLabel(TextProperties.getInstance().getProperty("CalculatorWiz_Type_L"));
        calcTypeLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 0));

        constantRadioButton = new JRadioButton(TextProperties.getInstance().getProperty("CalculatorWiz_ConstantType_L"));
        JRadioButton rasterRadioButton = new JRadioButton(TextProperties.getInstance().getProperty("CalculatorWiz_RasterType_L"));

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(constantRadioButton);
        buttonGroup.add(rasterRadioButton);

        Box buttonBox = Box.createVerticalBox();
        buttonBox.add(constantRadioButton);
        buttonBox.add(rasterRadioButton);

        constantOrRasterSelectionPanel = new JPanel();
        constantOrRasterSelectionPanel.setLayout(new BoxLayout(constantOrRasterSelectionPanel, BoxLayout.Y_AXIS));
        constantOrRasterSelectionPanel.add(calcTypeLabel);
        constantOrRasterSelectionPanel.add(buttonBox);
    }

    private void submitStepTwo() {
        JPanel calculationPanel = constantRadioButton.isSelected() ? constantCalculationPanel() : rasterCalculationPanel();
        calculationSelectionPanel.removeAll();
        calculationSelectionPanel.add(calculationPanel, getCalculationSelectionPanelGbc(0));
        calculationSelectionPanel.add(new JPanel(), getCalculationSelectionPanelGbc(1));
    }

    /* Step 3: Calculation Selection */
    private void initCalculationSelectionPanel() {
        GridBagLayout gridBagLayout = new GridBagLayout();
        calculationSelectionPanel = new JPanel(gridBagLayout);
        calculationSelectionPanel.setBorder(BorderFactory.createEmptyBorder(5, 9, 5, 8));
    }

    private GridBagConstraints getCalculationSelectionPanelGbc(int y) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = y == 0 ? GridBagConstraints.HORIZONTAL : GridBagConstraints.BOTH;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(0, 0, 5, 0);
        constraints.gridx = 0;
        constraints.weightx = 1;
        constraints.gridy = y;
        constraints.weighty = y == 0 ? 0 : 1;
        return constraints;
    }

    private boolean validateCalculationParameters() {
        return constantRadioButton.isSelected() ? validateConstantParameters() : validateRasterParameters();
    }

    private boolean validateConstantParameters() {
        int count = 0;
        String multiplyText = multiplyTextField.getText();
        String divideText = divideTextField.getText();
        String addText = addTextField.getText();
        String subtractText = subtractTextField.getText();

        if (multiplyText.isEmpty() && divideText.isEmpty() && addText.isEmpty() && subtractText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "One constant value entry required",
                    ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
            return false;
        }

        if (!multiplyText.isEmpty()) count++;
        if (!divideText.isEmpty()) count++;
        if (!addText.isEmpty()) count++;
        if (!subtractText.isEmpty()) count++;
        if (count != 1) {
            JOptionPane.showMessageDialog(this, "Only one constant value entry allowed",
                    ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    private boolean validateRasterParameters() {
        Path pathToRaster = Path.of(rasterTextField.getText());
        if (Files.isRegularFile(pathToRaster) && Files.notExists(pathToRaster)) {
            JOptionPane.showMessageDialog(this, "Raster does not exist",
                    ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    private JPanel constantCalculationPanel() {
        JLabel setConstantLabel = new JLabel(TextProperties.getInstance().getProperty("CalculatorWiz_SetConstant_L"));
        setConstantLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));

        JLabel multiplyLabel = new JLabel(Operation.MULTIPLY.getDisplayString());
        multiplyTextField = new JTextField(25);

        JPanel multiplyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        multiplyPanel.add(multiplyLabel);
        multiplyPanel.add(Box.createRigidArea(new Dimension(75, 0)));
        multiplyPanel.add(multiplyTextField);

        JLabel divideLabel = new JLabel(Operation.DIVIDE.getDisplayString());
        divideTextField = new JTextField(25);

        JPanel dividePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        dividePanel.add(divideLabel);
        dividePanel.add(Box.createRigidArea(new Dimension(82, 0)));
        dividePanel.add(divideTextField);

        JLabel addLabel = new JLabel(Operation.ADD.getDisplayString());
        addTextField = new JTextField(25);

        JPanel addPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addPanel.add(addLabel);
        addPanel.add(Box.createRigidArea(new Dimension(92, 0)));
        addPanel.add(addTextField);

        JLabel subtractLabel = new JLabel(Operation.SUBTRACT.getDisplayString());
        subtractTextField = new JTextField(25);

        JPanel subtractPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        subtractPanel.add(subtractLabel);
        subtractPanel.add(Box.createRigidArea(new Dimension(70, 0)));
        subtractPanel.add(subtractTextField);

        JPanel setConstantPanel = new JPanel();
        setConstantPanel.setLayout(new BoxLayout(setConstantPanel, BoxLayout.Y_AXIS));
        setConstantPanel.add(multiplyPanel);
        setConstantPanel.add(dividePanel);
        setConstantPanel.add(addPanel);
        setConstantPanel.add(subtractPanel);
        setConstantPanel.setBorder(BorderFactory.createEmptyBorder(0, -5, 0, 0));

        JPanel constantPanel = new JPanel(new BorderLayout());
        constantPanel.add(setConstantLabel, BorderLayout.NORTH);
        constantPanel.add(setConstantPanel, BorderLayout.CENTER);

        return constantPanel;
    }

    private JPanel rasterCalculationPanel() {
        JPanel operationPanel = operationSelectionPanel();
        JPanel rasterSelectionPanel = rasterSelectionPanel();
        resamplingPanel = new ResamplingMethodSelectionPanel();

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(operationPanel);
        panel.add(rasterSelectionPanel);
        panel.add(resamplingPanel);

        return panel;
    }

    private JPanel operationSelectionPanel() {
        JLabel operationLabel = new JLabel(TextProperties.getInstance().getProperty("ImportMetWizResamplingMethodL"));
        JPanel operationLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        operationLabelPanel.add(operationLabel);

        operationComboBox = new OperationComboBox();
        JPanel operationComboBoxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        operationComboBoxPanel.add(operationComboBox);

        JPanel operationSelectionPanel = new JPanel();
        operationSelectionPanel.setLayout(new BoxLayout(operationSelectionPanel, BoxLayout.Y_AXIS));
        operationSelectionPanel.add(operationLabelPanel);
        operationSelectionPanel.add(operationComboBoxPanel);

        return operationSelectionPanel;
    }

    private JPanel rasterSelectionPanel() {
        JLabel label = new JLabel(TextProperties.getInstance().getProperty("CalculatorWiz_Raster_L"));
        JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        labelPanel.add(label);

        JPanel textFieldPanel = new JPanel();
        textFieldPanel.setLayout(new BoxLayout(textFieldPanel, BoxLayout.X_AXIS));
        textFieldPanel.add(Box.createRigidArea(new Dimension(4, 0)));

        rasterTextField = new JTextField();
        textFieldPanel.add(rasterTextField);
        textFieldPanel.add(Box.createRigidArea(new Dimension(4, 0)));

        FileBrowseButton browseButton = new FileBrowseButton(getClass().getName(), "");
        browseButton.setIcon(IconResources.loadIcon("images/Open16.gif"));
        browseButton.setPreferredSize(new Dimension(22, 22));
        browseButton.addActionListener(evt -> selectDestinationBrowseAction(browseButton));
        textFieldPanel.add(browseButton);

        JPanel rasterSelectionPanel = new JPanel();
        rasterSelectionPanel.setLayout(new BoxLayout(rasterSelectionPanel, BoxLayout.Y_AXIS));
        rasterSelectionPanel.add(labelPanel);
        rasterSelectionPanel.add(textFieldPanel);

        return rasterSelectionPanel;
    }

    private void selectDestinationBrowseAction(FileBrowseButton fileBrowseButton) {
        JFileChooser fileChooser = new JFileChooser(fileBrowseButton.getPersistedBrowseLocation());

        fileChooser.setAcceptAllFileFilterUsed(false);
        FileNameExtensionFilter rasterFilter = new FileNameExtensionFilter("Raster Files (*.tif, *.tiff, *.asc)", "tif", "tiff", "asc");
        fileChooser.addChoosableFileFilter(rasterFilter);

        int userChoice = fileChooser.showOpenDialog(this);

        if (userChoice == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String selectedPath = selectedFile.getAbsolutePath();
            rasterTextField.setText(selectedPath);
            File finalFile = new File(selectedPath);
            fileBrowseButton.setPersistedBrowseLocation(finalFile);
        }
    }

    /* Step 4: Destination Selection */
    private void initDestinationSelectionPanel() {
        destinationSelectionPanel = new DestinationSelectionPanel(this);
    }

    private boolean validateStepFour() {
        String destinationFile = destinationSelectionPanel.getDestinationTextField().getText();
        if (destinationFile == null || destinationFile.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Destination file is required.",
                    ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    private void submitStepFour() {
        SwingWorker<Void, Void> task = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                calculateTask();
                return null;
            }

            @Override
            protected void done() {
                nextAction();
            }
        };

        task.execute();
    }

    private void calculateTask() {
        String pathToSource = sourceFileTextField.getText();
        List<String> selectedSourceGrids = getItemsInList(chosenSourceGridsList);
        if (selectedSourceGrids == null) return;

        String destination = destinationSelectionPanel.getDestinationTextField().getText();

        Map<String, String> writeOptions = DssWriteOptionsBuilder.buildWriteOptions(selectedSourceGrids, destinationSelectionPanel);

        Runnable runnable;

        if (constantRadioButton.isSelected()) {
            String multiplyText = multiplyTextField.getText();
            String divideText = divideTextField.getText();
            String addText = addTextField.getText();
            String subtractText = subtractTextField.getText();

            float multiplyValue;
            if (multiplyText != null && !multiplyText.isEmpty()) {
                multiplyValue = Float.parseFloat(multiplyTextField.getText());
            } else {
                multiplyValue = Float.NaN;
            }

            float divideValue;
            if (divideText != null && !divideText.isEmpty()) {
                divideValue = Float.parseFloat(divideTextField.getText());
            } else {
                divideValue = Float.NaN;
            }

            float addValue;
            if (addText != null && !addText.isEmpty()) {
                addValue = Float.parseFloat(addTextField.getText());
            } else {
                addValue = Float.NaN;
            }

            float subtractValue;
            if (subtractText != null && !subtractText.isEmpty()) {
                subtractValue = Float.parseFloat(subtractTextField.getText());
            } else {
                subtractValue = Float.NaN;
            }

            BatchCalculator batchCalculator = BatchCalculator.builder()
                    .pathToInput(pathToSource)
                    .variables(selectedSourceGrids)
                    .multiplyValue(multiplyValue)
                    .divideValue(divideValue)
                    .addValue(addValue)
                    .subtractValue(subtractValue)
                    .destination(destination)
                    .writeOptions(writeOptions)
                    .build();

            batchCalculator.addPropertyChangeListener(createProgressListener());

            runnable = batchCalculator;

        } else {
            String pathToRaster = rasterTextField.getText();
            ResamplingMethod resamplingMethod = resamplingPanel.getSelected();
            Operation operation = operationComboBox.getSelected();

            BatchGridCalculator batchGridCalculator = BatchGridCalculator.builder()
                    .pathToInput(pathToSource)
                    .variables(selectedSourceGrids)
                    .setOperation(operation)
                    .setPathToRaster(pathToRaster)
                    .setResamplingMethod(resamplingMethod)
                    .destination(destination)
                    .writeOptions(writeOptions)
                    .build();

            batchGridCalculator.addPropertyChangeListener(createProgressListener());

            runnable = batchGridCalculator;
        }

        SwingWorker<Void, Void> task = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                runnable.run();
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

    /* Add main for quick UI Testing */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                 UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        CalculatorWizard calculatorWizard = new CalculatorWizard(null);
        calculatorWizard.buildAndShowUI();
    }
}
