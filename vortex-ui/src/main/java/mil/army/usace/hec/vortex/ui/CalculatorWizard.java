package mil.army.usace.hec.vortex.ui;

import mil.army.usace.hec.vortex.VortexProperty;
import mil.army.usace.hec.vortex.geo.ResamplingMethod;
import mil.army.usace.hec.vortex.math.BatchCalculator;
import mil.army.usace.hec.vortex.math.BatchGridCalculator;
import mil.army.usace.hec.vortex.math.Operation;
import mil.army.usace.hec.vortex.ui.util.FileSaveUtil;
import mil.army.usace.hec.vortex.util.DssUtil;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CalculatorWizard extends VortexWizard {
    private static final Logger logger = Logger.getLogger(CalculatorWizard.class.getName());
    private static final String NEXT = TextProperties.getInstance().getProperty("CalculatorWiz_Next");
    private static final boolean IS_VALID = true;
    private static final String ERROR_TITLE = "Error";

    private final Frame frame;

    private Container contentCards;
    private CardLayout cardLayout;
    private JButton backButton;
    private JButton nextButton;
    private JButton cancelButton;
    private int cardNumber;

    private SourceFileSelectionPanel sourceFileSelectionPanel; // Step 1
    private JPanel constantOrRasterSelectionPanel; // Step 2
    private JPanel calculationSelectionPanel; // Step 3 (Constant or Raster)
    private DestinationSelectionPanel destinationSelectionPanel; // Step 4

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

    private final ProgressMessagePanel progressMessagePanel = new ProgressMessagePanel();

    public CalculatorWizard(Frame frame) {
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

    public void buildAndShowUI() {
        /* Setting Wizard's names and layout */
        this.setTitle(TextProperties.getInstance().getProperty("CalculatorWiz_Title"));
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

        /* Initialize Panels */
        initSourceFileSelectionPanel();
        initConstantOrRasterSelectionPanel();
        initCalculationSelectionPanel();
        initDestinationSelectionPanel();

        /* Adding Step Content Panels to contentCards */
        contentCards.add("Step One", sourceFileSelectionPanel);
        contentCards.add("Step Two", constantOrRasterSelectionPanel);
        contentCards.add("Step Three", calculationSelectionPanel);
        contentCards.add("Step Four", destinationSelectionPanel);
        contentCards.add("Step Five", initProgressBarPanel());
        contentCards.add("Step Six", initCompletedPanel());
    }

    private void initializeButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        /* Back Button */
        backButton = new JButton(TextProperties.getInstance().getProperty("CalculatorWiz_Back"));
        backButton.setToolTipText(TextProperties.getInstance().getProperty("CalculatorWiz_Back_TT"));
        backButton.setEnabled(false);
        backButton.addActionListener(evt -> backAction());

        /* Next Button */
        nextButton = new JButton(NEXT);
        nextButton.setToolTipText(TextProperties.getInstance().getProperty("CalculatorWiz_Next_TT"));
        nextButton.addActionListener(evt -> {
            if (nextButton.getText().equals(TextProperties.getInstance().getProperty("CalculatorWiz_Restart"))) {
                restartAction();
            } else if (nextButton.getText().equals(NEXT)) {
                nextAction();
            }
        });

        /* Cancel Button */
        cancelButton = new JButton(TextProperties.getInstance().getProperty("CalculatorWiz_Cancel"));
        cancelButton.setToolTipText(TextProperties.getInstance().getProperty("CalculatorWiz_Cancel_TT"));
        cancelButton.addActionListener(evt -> closeAction());

        /* Adding Buttons to NavigationPanel */
        buttonPanel.add(backButton);
        buttonPanel.add(nextButton);
        buttonPanel.add(cancelButton);

        /* Add buttonPanel to SanitizerWizard */
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
        backButton.setEnabled(cardNumber > 0 && cardNumber < 4);
        nextButton.setEnabled(cardNumber < 4);
        cancelButton.setEnabled(cardNumber < 4);
    }

    private void setButtonsForRestartOrClose() {
        backButton.setVisible(false);
        nextButton.setText(TextProperties.getInstance().getProperty("CalculatorWiz_Restart"));
        nextButton.setToolTipText(TextProperties.getInstance().getProperty("CalculatorWiz_Restart_TT"));
        nextButton.setEnabled(true);
        cancelButton.setText(TextProperties.getInstance().getProperty("CalculatorWiz_Close"));
        cancelButton.setToolTipText(TextProperties.getInstance().getProperty("CalculatorWiz_Close_TT"));
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

        /* Reset Buttons */
        backButton.setVisible(true);
        backButton.setEnabled(false);

        nextButton.setEnabled(true);
        nextButton.setText(NEXT);
        nextButton.setToolTipText(TextProperties.getInstance().getProperty("CalculatorWiz_Next_TT"));

        cancelButton.setText(TextProperties.getInstance().getProperty("CalculatorWiz_Cancel"));
        cancelButton.setToolTipText(TextProperties.getInstance().getProperty("CalculatorWiz_Cancel_TT"));

        /* Clear Step One Panel */
        sourceFileSelectionPanel.clear();

        /* Clear Constant Panel */
        multiplyTextField.setText("");
        divideTextField.setText("");
        addTextField.setText("");
        subtractTextField.setText("");

        /* Clear Raster Panel */
        operationComboBox.refresh();
        rasterTextField.setText("");
        resamplingPanel.refresh();

        /* Clear Destination Panel */
        destinationSelectionPanel.getDestinationTextField().setText("");
        destinationSelectionPanel.getFieldA().setText("");
        destinationSelectionPanel.getFieldB().setText("");
        destinationSelectionPanel.getFieldF().setText("");

        /* Clear Progress Panel */
        progressMessagePanel.clear();
    }

    private boolean validateCurrentStep() {
        switch (cardNumber) {
            case 0:
                return validateStepOne();
            case 1:
                return validateStepTwo();
            case 2:
                return validateCalculationParameters();
            case 3:
                return validateStepFour();
            case 4:
                return IS_VALID;
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
            case 4:
                submitStepFive();
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

    private void initSourceFileSelectionPanel() {
        sourceFileSelectionPanel = new SourceFileSelectionPanel(CalculatorWizard.class.getName());
        sourceFileTextField = sourceFileSelectionPanel.getSourceFileTextField();
        chosenSourceGridsList = sourceFileSelectionPanel.getChosenSourceGridsList();
    }

    private boolean validateStepOne() {
        return sourceFileSelectionPanel.validateInput();
    }

    private void submitStepOne() {
        // No operations needed
    }

    private void initConstantOrRasterSelectionPanel() {
        /* calcTypeLabel */
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

    private boolean validateStepTwo() {
        return true;
    }

    private void submitStepTwo() {
        JPanel calculationPanel = constantRadioButton.isSelected() ? constantCalculationPanel() : rasterCalculationPanel();
        calculationSelectionPanel.removeAll();
        calculationSelectionPanel.add(calculationPanel, getCalculationSelectionPanelGbc(0));
        calculationSelectionPanel.add(new JPanel(), getCalculationSelectionPanelGbc(1));
    }

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

        /* Check for at least one entry */
        if (multiplyText.isEmpty() && divideText.isEmpty() && addText.isEmpty() && subtractText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "One constant value entry required",
                    ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
            return false;
        }

        /* Check for multiple entries */
        if (!multiplyText.isEmpty())
            count++;
        if (!divideText.isEmpty())
            count++;
        if (!addText.isEmpty())
            count++;
        if (!subtractText.isEmpty())
            count++;
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

    private void submitStepThree() {
        // No operations needed
    }

    private JPanel constantCalculationPanel() {
        /* constantLabel */
        JLabel setConstantLabel = new JLabel(TextProperties.getInstance().getProperty("CalculatorWiz_SetConstant_L"));
        setConstantLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));

        /* Multiply Panel */
        JLabel multiplyLabel = new JLabel(Operation.MULTIPLY.getDisplayString());
        multiplyTextField = new JTextField(25);

        JPanel multiplyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        multiplyPanel.add(multiplyLabel);
        multiplyPanel.add(Box.createRigidArea(new Dimension(75, 0)));
        multiplyPanel.add(multiplyTextField);

        /* Divide Panel */
        JLabel divideLabel = new JLabel(Operation.DIVIDE.getDisplayString());
        divideTextField = new JTextField(25);

        JPanel dividePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        dividePanel.add(divideLabel);
        dividePanel.add(Box.createRigidArea(new Dimension(82, 0)));
        dividePanel.add(divideTextField);

        /* Add Panel */
        JLabel addLabel = new JLabel(Operation.ADD.getDisplayString());
        addTextField = new JTextField(25);

        JPanel addPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addPanel.add(addLabel);
        addPanel.add(Box.createRigidArea(new Dimension(92, 0)));
        addPanel.add(addTextField);

        /* subtract Panel */
        JLabel subtractLabel = new JLabel(Operation.SUBTRACT.getDisplayString());
        subtractTextField = new JTextField(25);

        JPanel subtractPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        subtractPanel.add(subtractLabel);
        subtractPanel.add(Box.createRigidArea(new Dimension(70, 0)));
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

    private JPanel rasterCalculationPanel() {
        //Operation
        JPanel operationPanel = operationSelectionPanel();

        // Raster
        JPanel rasterSelectionPanel = rasterSelectionPanel();

        // Resampling
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
        /* Select Destination section (of stepFourPanel) */
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

        // Configuring fileChooser dialog
        fileChooser.setAcceptAllFileFilterUsed(false);
        FileNameExtensionFilter rasterFilter = new FileNameExtensionFilter("Raster Files (*.tif, *.tiff, *.asc)", "tif", "tiff", "asc");
        fileChooser.addChoosableFileFilter(rasterFilter);

        // Pop up fileChooser dialog
        int userChoice = fileChooser.showOpenDialog(this);

        // Deal with user's choice
        if (userChoice == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String selectedPath = selectedFile.getAbsolutePath();
            rasterTextField.setText(selectedPath);
            File finalFile = new File(selectedPath);
            fileBrowseButton.setPersistedBrowseLocation(finalFile);
        }
    }

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

        /* Setting parts */
        Map<String, Set<String>> pathnameParts = DssUtil.getPathnameParts(selectedSourceGrids);
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

            batchCalculator.addPropertyChangeListener(evt -> {
                VortexProperty property = VortexProperty.parse(evt.getPropertyName());
                if (VortexProperty.PROGRESS == property) {
                    if (!(evt.getNewValue() instanceof Integer)) return;
                    int progressValue = (int) evt.getNewValue();
                    progressMessagePanel.setValue(progressValue);
                } else {
                    String value = String.valueOf(evt.getNewValue());
                    progressMessagePanel.write(value);
                }
            });

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

            batchGridCalculator.addPropertyChangeListener(evt -> {
                VortexProperty property = VortexProperty.parse(evt.getPropertyName());
                if (VortexProperty.PROGRESS == property) {
                    if (!(evt.getNewValue() instanceof Integer)) return;
                    int progressValue = (int) evt.getNewValue();
                    progressMessagePanel.setValue(progressValue);
                } else {
                    String value = String.valueOf(evt.getNewValue());
                    progressMessagePanel.write(value);
                }
            });

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
            }
        };

        task.execute();
    }

    private JPanel initProgressBarPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(progressMessagePanel, BorderLayout.CENTER);
        return panel;
    }

    private void submitStepFive() {
        // No operations needed
    }

    private JPanel initCompletedPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(progressMessagePanel, BorderLayout.CENTER);
        return panel;
    }

    private List<String> getItemsInList(JList<String> list) {
        DefaultListModel<String> defaultRightModel = Util.getDefaultListModel(list);
        if (defaultRightModel == null) {
            return Collections.emptyList();
        }
        return Collections.list(defaultRightModel.elements());
    }

    private void closeAction() {
        CalculatorWizard.this.setVisible(false);
        CalculatorWizard.this.dispose();
        String savedFile = destinationSelectionPanel.getDestinationTextField().getText();
        FileSaveUtil.showFileLocation(CalculatorWizard.this, Path.of(savedFile));
    }

    /* Add main for quick UI Testing */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                 UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        CalculatorWizard sanitizerWizard = new CalculatorWizard(null);
        sanitizerWizard.buildAndShowUI();
    }
}