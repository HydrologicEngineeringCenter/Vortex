package mil.army.usace.hec.vortex.ui;

import com.formdev.flatlaf.FlatLightLaf;
import mil.army.usace.hec.vortex.Message;
import mil.army.usace.hec.vortex.VortexProperty;
import mil.army.usace.hec.vortex.io.BatchImporter;
import mil.army.usace.hec.vortex.io.DataReader;
import mil.army.usace.hec.vortex.io.Validation;
import mil.army.usace.hec.vortex.ui.util.FileSaveUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ImportMetWizard extends ProcessingWizard {
    private static final Logger logger = Logger.getLogger(ImportMetWizard.class.getName());

    private DestinationSelectionPanel destinationSelectionPanel;

    private JList<String> addFilesList, leftVariablesList, rightVariablesList;
    private JTextField dataSourceTextField;
    private JTextField targetCellSizeTextField;
    private CellSizeUnitsComboBox targetCellSizeUnitsComboBox;
    private JTextArea targetWktTextArea;
    private ResamplingMethodSelectionPanel resamplingPanel;

    private final AtomicBoolean importComplete = new AtomicBoolean();

    public ImportMetWizard(Frame frame) {
        super(frame);
    }

    @Override
    protected String getTitlePropertyKey() {
        return "ImportMetWizNameTitle";
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
            case 0 -> validateStepOne();
            case 1 -> validateStepTwo();
            case 2 -> validateStepThree();
            case 3 -> validateStepFour();
            case 4 -> true;
            default -> {
                logger.log(Level.SEVERE, "Unknown Step in Wizard");
                yield false;
            }
        };
    }

    @Override
    protected void submitStep(int stepIndex) {
        switch (stepIndex) {
            case 0:
                submitStepOne();
                break;
            case 3:
                SwingWorker<Void, Void> task = new SwingWorker<>() {
                    @Override
                    protected Void doInBackground() {
                        importerTask();
                        return null;
                    }

                    @Override
                    protected void done() {
                        nextAction();
                    }
                };
                task.execute();
                break;
            default:
                break;
        }
    }

    @Override
    protected void clearWizardState() {
        importComplete.set(false);

        /* Clearing Step One Panel */
        Objects.requireNonNullElse(getDefaultListModel(addFilesList), new DefaultListModel<>()).clear();

        /* Clearing Step Two Panel */
        Objects.requireNonNullElse(getDefaultListModel(leftVariablesList), new DefaultListModel<>()).clear();
        Objects.requireNonNullElse(getDefaultListModel(rightVariablesList), new DefaultListModel<>()).clear();

        /* Clearing Step Three Panel */
        dataSourceTextField.setText("");
        targetWktTextArea.setText("");
        targetCellSizeTextField.setText("");
        resamplingPanel.refresh();

        /* Clearing Step Four Panel */
        destinationSelectionPanel.getDestinationTextField().setText("");
        destinationSelectionPanel.getFieldA().setText("");
        destinationSelectionPanel.getFieldB().setText("");
        destinationSelectionPanel.getFieldF().setText("");
    }

    @Override
    protected void showSaveResult() {
        if (importComplete.get()) {
            String savedFile = destinationSelectionPanel.getDestinationTextField().getText();
            FileSaveUtil.showFileLocation(ImportMetWizard.this, Path.of(savedFile));
        }
    }

    @Override
    protected void onBackAction(int newCardNumber) {
        if (newCardNumber == 0) {
            DefaultListModel<String> leftVariablesModel = getDefaultListModel(leftVariablesList);
            if (leftVariablesModel != null) leftVariablesModel.clear();
            DefaultListModel<String> rightVariablesModel = getDefaultListModel(rightVariablesList);
            if (rightVariablesModel != null) rightVariablesModel.clear();
        }
    }

    private boolean validateStepOne() {
        DefaultListModel<String> addFilesListModel = getDefaultListModel(addFilesList);
        if (addFilesListModel == null) {
            return false;
        }

        List<String> files = Collections.list(addFilesListModel.elements());

        if (files.isEmpty()) {
            JOptionPane.showMessageDialog(this, Text.format("Error_InputRequired"),
                    Text.format("Error_MissingField_Title"), JOptionPane.ERROR_MESSAGE);
            return false;
        }

        String errorTemplate = Message.format("error_archive_file");

        List<String> messages = files.stream()
                .filter(DataReader::isArchive)
                .filter(f -> !DataReader.isSupportedArchive(f))
                .map(f -> String.format(errorTemplate, f))
                .collect(Collectors.toList());

        if (!messages.isEmpty()) {
            showUnsupportedArchiveError(messages);
            return false;
        }

        return true;
    }

    private boolean validateStepTwo() {
        DefaultListModel<String> defaultListModel = getDefaultListModel(rightVariablesList);
        if (defaultListModel == null) {
            return false;
        }

        if (defaultListModel.isEmpty()) {
            JOptionPane.showMessageDialog(this, Text.format("Error_NoVariablesSelected"),
                    Text.format("Error_NoVariables_Title"), JOptionPane.ERROR_MESSAGE);
            return false;
        }

        return true;
    }

    private boolean validateStepThree() { return true; }

    private boolean validateStepFour() {
        String destinationPath = destinationSelectionPanel.getDestinationTextField().getText();

        if (destinationPath.isEmpty()) {
            JOptionPane.showMessageDialog(this, Text.format("Error_DestinationRequired"),
                    Text.format("Error_MissingField_Title"), JOptionPane.ERROR_MESSAGE);
            return false;
        }

        if (!destinationPath.matches(".*(dss|nc[34]?)")) {
            JOptionPane.showMessageDialog(this, Text.format("ImportMetWiz_InvalidExtension"),
                    Text.format("ImportMetWiz_UnsupportedFileType_Title"), JOptionPane.ERROR_MESSAGE);
            return false;
        }

        if (destinationPath.endsWith(".dss")) {
            File dssFile = new File(destinationPath);
            if (!dssFile.exists()) {
                try {
                    boolean success = dssFile.createNewFile();
                    if (success) {
                        logger.log(Level.INFO, "Created DSS File: " + dssFile);
                    }
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "DSS File not created: " + dssFile);
                }
            }
        }

        return validateSelections();
    }

    private boolean validateSelections() {
        DefaultListModel<String> defaultAddFilesModel = getDefaultListModel(addFilesList);
        List<String> files = Collections.list(Optional.ofNullable(defaultAddFilesModel).orElse(new DefaultListModel<>()).elements());

        DefaultListModel<String> defaultSelectedVariablesModel = getDefaultListModel(rightVariablesList);
        List<String> variables = Collections.list(Optional.ofNullable(defaultSelectedVariablesModel).orElse(new DefaultListModel<>()).elements());

        boolean isValid = true;
        Set<String> messages = new LinkedHashSet<>();
        for (String file : files) {
            Set<String> availableVariables = DataReader.getVariables(file.trim());
            for (String variable : variables) {
                if (!availableVariables.contains(variable))
                    continue;
                try (DataReader reader = DataReader.builder().path(file.trim()).variable(variable).build()) {
                    Validation validation = reader.isValid();
                    if (!validation.isValid()) isValid = false;
                    messages.addAll(validation.getMessages());
                } catch (Exception e) {
                    logger.log(Level.SEVERE, e, e::getMessage);
                    return false;
                }
            }
        }

        String message = String.join(System.lineSeparator(), messages);
        if (!isValid) {
            String title = Text.format("Error_Title");
            JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
            return false;
        }

        if (!messages.isEmpty()) {
            String title = Text.format("Warning_Title");
            return JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this, message, title,
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        }

        return true;
    }

    private void submitStepOne() {
        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        DefaultListModel<String> defaultListModel = getDefaultListModel(addFilesList);
        if (defaultListModel == null) {
            return;
        }
        List<String> fileList = Collections.list(defaultListModel.elements());

        Set<String> variables = fileList.stream()
                .map(DataReader::getVariables)
                .flatMap(Collection::stream)
                .sorted()
                .collect(Collectors.toCollection(LinkedHashSet::new));

        sortDssVariables(variables);

        DefaultListModel<String> variableDefaultListModel = getDefaultListModel(leftVariablesList);
        if (variableDefaultListModel == null) {
            return;
        }
        variableDefaultListModel.addAll(variables);

        this.setCursor(Cursor.getDefaultCursor());
    }

    private void importerTask() {
        /* Getting inFiles */
        DefaultListModel<String> defaultAddFilesModel = getDefaultListModel(addFilesList);
        if (defaultAddFilesModel == null) {
            return;
        }
        List<String> inFilesList = Collections.list(defaultAddFilesModel.elements());
        inFilesList = inFilesList.stream().map(String::trim).collect(Collectors.toList());

        /* Getting selected variables */
        DefaultListModel<String> defaultSelectedVariablesModel = getDefaultListModel(rightVariablesList);
        if (defaultSelectedVariablesModel == null) {
            return;
        }
        List<String> selectedVariables = Collections.list(defaultSelectedVariablesModel.elements());

        /* Getting geoOptions */
        Map<String, String> geoOptions = new HashMap<>();

        String clippingDatasource = dataSourceTextField.getText();
        if (!clippingDatasource.isEmpty()) {
            geoOptions.put("pathToShp", clippingDatasource);
        }

        String targetWkt = targetWktTextArea.getText();
        if (!targetWkt.isEmpty()) {
            geoOptions.put("targetWkt", targetWkt);
        }

        String targetCellSize = targetCellSizeTextField.getText();
        if (!targetCellSize.isEmpty()) {
            geoOptions.put("targetCellSize", targetCellSize);
            String targetCellSizeUnits = String.valueOf(targetCellSizeUnitsComboBox.getSelectedItem());
            geoOptions.put("targetCellSizeUnits", targetCellSizeUnits);
        }

        geoOptions.put("resamplingMethod", resamplingPanel.getSelected().toString());

        /* Getting Destination */
        String destination = destinationSelectionPanel.getDestinationTextField().getText();

        /* Building write options */
        Map<String, String> writeOptions = DssWriteOptionsBuilder.buildWriteOptions(selectedVariables, destinationSelectionPanel, "*");

        if (destination.toLowerCase().endsWith(".nc")) {
            writeOptions.put("isOverwrite", String.valueOf(destinationSelectionPanel.isOverwrite()));
        }

        BatchImporter importer = BatchImporter.builder()
                .inFiles(inFilesList)
                .variables(selectedVariables)
                .geoOptions(geoOptions)
                .destination(destination)
                .writeOptions(writeOptions)
                .build();

        importer.addPropertyChangeListener(evt -> {
            VortexProperty property = VortexProperty.parse(evt.getPropertyName());
            if (VortexProperty.STATUS == property) {
                String value = String.valueOf(evt.getNewValue());
                progressMessagePanel.write(value);
            }

            if (VortexProperty.PROGRESS == property) {
                if (!(evt.getNewValue() instanceof Integer)) return;
                int progressValue = (int) evt.getNewValue();
                progressMessagePanel.setValue(progressValue);
            }

            if (VortexProperty.ERROR == property) {
                String errorMessage = String.valueOf(evt.getNewValue());
                progressMessagePanel.write(errorMessage);
            }

            if (VortexProperty.COMPLETE == property) {
                String value = String.valueOf(evt.getNewValue());
                progressMessagePanel.write(value);
                importComplete.set(true);
            }
        });

        SwingWorker<Void, Void> task = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                importer.process();
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

    private void sortDssVariables(Set<String> variableList) {
        DefaultListModel<String> defaultListModel = getDefaultListModel(addFilesList);
        if (defaultListModel == null) {
            return;
        }
        List<String> fileList = Collections.list(defaultListModel.elements());

        Set<String> extensions = fileList.stream()
                .map(file -> file.substring(file.length() - 3))
                .collect(Collectors.toSet());

        if (extensions.size() == 1 && extensions.iterator().next().equals("dss")) {
            Util.sortDssVariables(variableList);
        }
    }

    private JPanel stepOnePanel() {
        JPanel stepOnePanel = new JPanel(new BorderLayout());
        stepOnePanel.setBorder(BorderFactory.createEmptyBorder(5,10,5,10));

        JLabel addFilesLabel = new JLabel(Text.format("ImportMetWizAddFiles"));
        addFilesLabel.setBorder(BorderFactory.createEmptyBorder(0,0,5,0));
        stepOnePanel.add(addFilesLabel, BorderLayout.NORTH);

        DefaultListModel<String> addFilesListModel = new DefaultListModel<>();
        addFilesList = new JList<>(addFilesListModel);
        addFilesList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    addFilesListModel.remove(addFilesList.getSelectedIndex());
                    addFilesList.revalidate();
                    addFilesList.repaint();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(addFilesList);
        stepOnePanel.add(scrollPane, BorderLayout.CENTER);

        JPanel browsePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0,0));
        String uniqueId = this.getClass().getName() + ".addFiles";
        FileBrowseButton browseButton = new FileBrowseButton(uniqueId, "");
        browseButton.setIcon(IconResources.loadIcon("images/Open16.gif"));
        browseButton.setPreferredSize(new Dimension(22,22));
        browsePanel.add(Box.createRigidArea(new Dimension(8,0)));
        browsePanel.add(browseButton);
        stepOnePanel.add(browsePanel, BorderLayout.EAST);
        browseButton.addActionListener(arg0 -> addFilesBrowseAction(browseButton));

        return stepOnePanel;
    }

    private JPanel stepTwoPanel() {
        JPanel stepTwoPanel = new JPanel(new BorderLayout());
        stepTwoPanel.setBorder(BorderFactory.createEmptyBorder(5,10,5,10));

        JLabel selectVariablesLabel = new JLabel(Text.format("ImportMetWizSelectVariables"));
        selectVariablesLabel.setBorder(BorderFactory.createEmptyBorder(0,0,5,0));
        stepTwoPanel.add(selectVariablesLabel, BorderLayout.NORTH);

        DefaultListModel<String> leftVariablesListModel = new DefaultListModel<>();
        leftVariablesList = new JList<>(leftVariablesListModel);
        leftVariablesList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    updateVariablesAction(VariableChooserAction.ADD);
                }
            }
        });
        JScrollPane leftScrollPanel = new JScrollPane();
        leftScrollPanel.setViewportView(leftVariablesList);

        DefaultListModel<String> rightVariablesListModel = new DefaultListModel<>();
        rightVariablesList = new JList<>(rightVariablesListModel);
        rightVariablesList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    updateVariablesAction(VariableChooserAction.REMOVE);
                }
            }
        });
        JScrollPane rightScrollPanel = new JScrollPane();
        rightScrollPanel.setViewportView(rightVariablesList);

        JPanel transferButtonsPanel = new JPanel();
        transferButtonsPanel.setLayout(new BoxLayout(transferButtonsPanel, BoxLayout.Y_AXIS));
        JButton addVariableButton = new JButton(IconResources.loadIcon("images/right-arrow-24.png"));
        addVariableButton.setPreferredSize(new Dimension(22,22));
        addVariableButton.setMaximumSize(new Dimension(22,22));
        addVariableButton.addActionListener(evt -> updateVariablesAction(VariableChooserAction.ADD));
        JButton removeVariableButton = new JButton(IconResources.loadIcon("images/left-arrow-24.png"));
        removeVariableButton.setPreferredSize(new Dimension(22,22));
        removeVariableButton.setMaximumSize(new Dimension(22,22));
        removeVariableButton.addActionListener(evt -> updateVariablesAction(VariableChooserAction.REMOVE));
        transferButtonsPanel.add(addVariableButton);
        transferButtonsPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        transferButtonsPanel.add(removeVariableButton);

        JPanel mainContentPanel = new JPanel();
        mainContentPanel.setLayout(new BoxLayout(mainContentPanel, BoxLayout.X_AXIS));
        mainContentPanel.add(leftScrollPanel);
        mainContentPanel.add(Box.createRigidArea(new Dimension(4,0)));
        mainContentPanel.add(transferButtonsPanel);
        mainContentPanel.add(Box.createRigidArea(new Dimension(4,0)));
        mainContentPanel.add(rightScrollPanel);
        stepTwoPanel.add(mainContentPanel, BorderLayout.CENTER);

        int mainContentWidth = mainContentPanel.getPreferredSize().width;
        int scrollPanelWidth = mainContentWidth * 45 / 100;
        leftScrollPanel.setPreferredSize(new Dimension(scrollPanelWidth, 0));
        rightScrollPanel.setPreferredSize(new Dimension(scrollPanelWidth, 0));

        return stepTwoPanel;
    }

    private JPanel stepThreePanel() {
        GridBagLayout gridBagLayout = new GridBagLayout();
        gridBagLayout.columnWidths = new int[]{this.getPreferredSize().width, 0};
        gridBagLayout.rowHeights = new int[]{50, 100, 50, 50, 0};
        gridBagLayout.columnWeights = new double[]{1.0, Double.MIN_VALUE};
        gridBagLayout.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};

        JPanel stepThreePanel = new JPanel(gridBagLayout);
        stepThreePanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,7));

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(0, 0, 5, 0);
        gridBagConstraints.gridx = 0;

        gridBagConstraints.gridy = 0;
        stepThreePanel.add(dataSourceSectionPanel(), gridBagConstraints);

        gridBagConstraints.gridy = 1;
        gridBagConstraints.weighty = 1;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        stepThreePanel.add(targetWktSectionPanel(), gridBagConstraints);

        gridBagConstraints.gridy = 2;
        gridBagConstraints.weighty = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        stepThreePanel.add(targetCellSizeSectionPanel(), gridBagConstraints);

        gridBagConstraints.gridy = 3;
        resamplingPanel = new ResamplingMethodSelectionPanel();
        stepThreePanel.add(resamplingPanel, gridBagConstraints);

        return stepThreePanel;
    }

    private JPanel stepFourPanel() {
        destinationSelectionPanel = new DestinationSelectionPanel(this);
        return destinationSelectionPanel;
    }

    private JPanel dataSourceSectionPanel() {
        JLabel dataSourceLabel = new JLabel(Text.format("ImportMetWizClippingDatasourceL"));
        JPanel dataSourceLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        dataSourceLabelPanel.add(dataSourceLabel);

        JPanel dataSourceTextFieldPanel = new JPanel();
        dataSourceTextFieldPanel.setLayout(new BoxLayout(dataSourceTextFieldPanel, BoxLayout.X_AXIS));

        dataSourceTextFieldPanel.add(Box.createRigidArea(new Dimension(4,0)));

        dataSourceTextField = new JTextField();
        dataSourceTextField.setFont(addFilesList.getFont());
        dataSourceTextFieldPanel.add(dataSourceTextField);

        dataSourceTextFieldPanel.add(Box.createRigidArea(new Dimension(8,0)));

        String uniqueId = this.getClass().getName() + ".dataSource";
        FileBrowseButton dataSourceBrowseButton = new FileBrowseButton(uniqueId, "");
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

    private JPanel targetWktSectionPanel() {
        JLabel targetWktLabel = new JLabel(Text.format("ImportMetWizTargetWktL"));
        JPanel targetWktLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        targetWktLabelPanel.add(targetWktLabel);

        JPanel targetWktTextAreaPanel = new JPanel();
        targetWktTextAreaPanel.setLayout(new BoxLayout(targetWktTextAreaPanel, BoxLayout.X_AXIS));
        targetWktTextAreaPanel.add(Box.createRigidArea(new Dimension(4,0)));

        targetWktTextArea = new JTextArea();
        targetWktTextArea.setLineWrap(true);
        targetWktTextArea.setColumns(0);
        targetWktTextArea.setRows(5);
        targetWktTextArea.setBorder(null);
        targetWktTextArea.setFont(addFilesList.getFont());
        JScrollPane targetWktTextAreaScrollPanel = new JScrollPane(targetWktTextArea);
        targetWktTextAreaPanel.add(targetWktTextAreaScrollPanel);

        targetWktTextAreaPanel.add(Box.createRigidArea(new Dimension(8,0)));

        JPanel targetWktButtonsPanel = new JPanel();
        targetWktButtonsPanel.setLayout(new BoxLayout(targetWktButtonsPanel, BoxLayout.Y_AXIS));

        String uniqueId = this.getClass().getName() + ".targetWkt";
        FileBrowseButton targetWktBrowseButton = new FileBrowseButton(uniqueId, "");
        targetWktBrowseButton.setIcon(IconResources.loadIcon("images/Open16.gif"));
        targetWktBrowseButton.setPreferredSize(new Dimension(22,22));
        targetWktBrowseButton.addActionListener(evt -> targetWktBrowseAction(targetWktBrowseButton));
        JButton targetWktProjectionButton = new JButton(IconResources.loadIcon("images/geography-16.png"));
        targetWktProjectionButton.setPreferredSize(new Dimension(22,22));
        targetWktProjectionButton.addActionListener(evt -> targetWktProjectionAction());
        targetWktButtonsPanel.add(targetWktBrowseButton);
        targetWktButtonsPanel.add(Box.createRigidArea(new Dimension(0,5)));
        targetWktButtonsPanel.add(targetWktProjectionButton);
        targetWktButtonsPanel.add(Box.createVerticalGlue());

        targetWktTextAreaPanel.add(targetWktButtonsPanel);

        JPanel targetWktSectionPanel = new JPanel(new BorderLayout());
        targetWktSectionPanel.add(targetWktLabelPanel, BorderLayout.NORTH);
        targetWktSectionPanel.add(targetWktTextAreaPanel, BorderLayout.CENTER);

        return targetWktSectionPanel;
    }

    private JPanel targetCellSizeSectionPanel() {
        JLabel targetCellSizeLabel = new JLabel(Text.format("ImportMetWizTargetCellSizeL"));
        JPanel targetCellSizeLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        targetCellSizeLabelPanel.add(targetCellSizeLabel);

        JPanel targetCellSizeTextFieldPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        targetCellSizeTextFieldPanel.setLayout(new BoxLayout(targetCellSizeTextFieldPanel, BoxLayout.X_AXIS));
        targetCellSizeTextFieldPanel.add(Box.createRigidArea(new Dimension(4,0)));

        targetCellSizeTextField = new JTextField();
        targetCellSizeTextFieldPanel.add(targetCellSizeTextField);
        targetCellSizeTextFieldPanel.add(Box.createRigidArea(new Dimension(8, 0)));

        targetCellSizeUnitsComboBox = new CellSizeUnitsComboBox();
        targetCellSizeTextFieldPanel.add(targetCellSizeUnitsComboBox);
        targetCellSizeTextFieldPanel.add(Box.createRigidArea(new Dimension(8, 0)));

        JButton selectCellSizeButton = new JButton(IconResources.loadIcon("images/grid-16.png"));
        selectCellSizeButton.setPreferredSize(new Dimension(22,22));
        selectCellSizeButton.addActionListener(evt -> targetCellSizeSelectionAction());
        targetCellSizeTextFieldPanel.add(selectCellSizeButton);

        JPanel targetCellSizeSectionPanel = new JPanel();
        targetCellSizeSectionPanel.setLayout(new BoxLayout(targetCellSizeSectionPanel, BoxLayout.Y_AXIS));
        targetCellSizeSectionPanel.add(targetCellSizeLabelPanel);
        targetCellSizeSectionPanel.add(targetCellSizeTextFieldPanel);

        return targetCellSizeSectionPanel;
    }

    /* Actions */
    private enum VariableChooserAction {
        ADD, REMOVE
    }

    private void updateVariablesAction(VariableChooserAction action) {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        SwingWorker<Map<String, List<String>>, Void> worker = new SwingWorker<>() {
            @Override
            protected Map<String, List<String>> doInBackground() {
                DefaultListModel<String> defaultLeftModel = getDefaultListModel(leftVariablesList);
                DefaultListModel<String> defaultRightModel = getDefaultListModel(rightVariablesList);

                if (defaultLeftModel == null || defaultRightModel == null) {
                    return Collections.emptyMap();
                }

                /* MUST be a LinkedHashSet to preserve order */
                Set<String> availableVariableSet = new LinkedHashSet<>(Collections.list(defaultLeftModel.elements()));
                Set<String> selectedVariableSet = new LinkedHashSet<>(Collections.list(defaultRightModel.elements()));

                if (action == VariableChooserAction.ADD) {
                    List<String> selectedVariables = leftVariablesList.getSelectedValuesList();
                    selectedVariables.forEach(availableVariableSet::remove);
                    selectedVariableSet.addAll(selectedVariables);
                } else if (action == VariableChooserAction.REMOVE) {
                    List<String> selectedVariables = rightVariablesList.getSelectedValuesList();
                    selectedVariables.forEach(selectedVariableSet::remove);
                    availableVariableSet.addAll(selectedVariables);
                }

                return generateSortedLists(availableVariableSet, selectedVariableSet);
            }

            @Override
            protected void done() {
                DefaultListModel<String> defaultLeftModel = getDefaultListModel(leftVariablesList);
                DefaultListModel<String> defaultRightModel = getDefaultListModel(rightVariablesList);
                if (defaultLeftModel == null || defaultRightModel == null) return;

                try {
                    Map<String, List<String>> lists = get();
                    if (lists.isEmpty()) return;

                    defaultLeftModel.clear();
                    defaultRightModel.clear();

                    defaultLeftModel.addAll(lists.getOrDefault("available", Collections.emptyList()));
                    defaultRightModel.addAll(lists.getOrDefault("selected", Collections.emptyList()));
                } catch (InterruptedException | ExecutionException e) {
                    logger.warning(e.getMessage());
                    Thread.currentThread().interrupt();
                } finally {
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        };
        worker.execute();
    }

    private Map<String, List<String>> generateSortedLists(Set<String> available, Set<String> selected) {
        sortDssVariables(available);
        sortDssVariables(selected);

        Map<String, List<String>> resultMap = new HashMap<>();
        resultMap.put("available", new ArrayList<>(available));
        resultMap.put("selected", new ArrayList<>(selected));

        return resultMap;
    }

    private void addFilesBrowseAction(FileBrowseButton fileBrowseButton) {
        JFileChooser fileChooser = new JFileChooser(fileBrowseButton.getPersistedBrowseLocation());
        fileChooser.setMultiSelectionEnabled(true);

        fileChooser.setAcceptAllFileFilterUsed(false);
        FileNameExtensionFilterEnhanced recognizedFilter = new FileNameExtensionFilterEnhanced(
                "All recognized files", ".nc", ".nc4", ".hdf", ".hdf5", ".h5",
                ".grib", ".gb2", ".grb2", ".grib2", ".grb",
                ".asc", ".bil", "*bil.zip", ".dss", ".tif", ".tiff", ".dat", ".tar", "bil.zip");
        fileChooser.addChoosableFileFilter(recognizedFilter);
        FileNameExtensionFilterEnhanced ncFilter = new FileNameExtensionFilterEnhanced(
                "netCDF datasets", ".nc", ".nc4");
        fileChooser.addChoosableFileFilter(ncFilter);
        FileNameExtensionFilterEnhanced hdfFilter = new FileNameExtensionFilterEnhanced(
                "HDF datasets", ".hdf", ".hdf5");
        fileChooser.addChoosableFileFilter(hdfFilter);
        FileNameExtensionFilterEnhanced gribFilter = new FileNameExtensionFilterEnhanced(
                "GRIB datasets", ".grib", ".gb2", ".grb2", ".grib2", ".grb");
        fileChooser.addChoosableFileFilter(gribFilter);

        FileNameExtensionFilterEnhanced ascFilter = new FileNameExtensionFilterEnhanced(
                "ASC datasets", ".asc");
        fileChooser.addChoosableFileFilter(ascFilter);

        FileNameExtensionFilterEnhanced tifFilter = new FileNameExtensionFilterEnhanced(
                "TIF datasets", ".tif");
        fileChooser.addChoosableFileFilter(tifFilter);

        FileNameExtensionFilterEnhanced bilFilter = new FileNameExtensionFilterEnhanced(
                "BIL datasets", ".bil", "bil.zip");
        fileChooser.addChoosableFileFilter(bilFilter);

        FileNameExtensionFilterEnhanced tarFilter = new FileNameExtensionFilterEnhanced(
                "SNODAS datasets", ".tar", ".dat");
        fileChooser.addChoosableFileFilter(tarFilter);

        FileNameExtensionFilterEnhanced dssFilter = new FileNameExtensionFilterEnhanced(
                "DSS datasets", ".dss");
        fileChooser.addChoosableFileFilter(dssFilter);

        FileNameExtensionFilterEnhanced allFilesFilter = new FileNameExtensionFilterEnhanced(
                "All files", "");
        fileChooser.addChoosableFileFilter(allFilesFilter);

        int userChoice = fileChooser.showOpenDialog(this);

        if (userChoice == JFileChooser.APPROVE_OPTION) {
            File[] selectedFiles = fileChooser.getSelectedFiles();
            ListModel<String> listModel = addFilesList.getModel();
            if (listModel instanceof DefaultListModel<String> defaultListModel) {
                List<String> elementList = Collections.list(defaultListModel.elements());
                for (File file : selectedFiles) {
                    if (!elementList.contains(file.getAbsolutePath()))
                        defaultListModel.addElement(file.getAbsolutePath());
                }
                fileBrowseButton.setPersistedBrowseLocation(selectedFiles[0]);
            }
        }
    }

    private void dataSourceBrowseAction(FileBrowseButton fileBrowseButton) {
        JFileChooser fileChooser = new JFileChooser(fileBrowseButton.getPersistedBrowseLocation());

        fileChooser.setAcceptAllFileFilterUsed(false);
        FileNameExtensionFilterEnhanced acceptableExtension = new FileNameExtensionFilterEnhanced("Shapefiles (*.shp)", ".shp");
        fileChooser.addChoosableFileFilter(acceptableExtension);

        int userChoice = fileChooser.showOpenDialog(this);

        if (userChoice == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            dataSourceTextField.setText(selectedFile.getAbsolutePath());
            fileBrowseButton.setPersistedBrowseLocation(selectedFile);
        }
    }

    private void targetWktBrowseAction(FileBrowseButton fileBrowseButton) {
        JFileChooser fileChooser = new JFileChooser(fileBrowseButton.getPersistedBrowseLocation());

        fileChooser.setAcceptAllFileFilterUsed(false);
        FileNameExtensionFilterEnhanced acceptableExtension = new FileNameExtensionFilterEnhanced("Projection Files (*.prj)", ".prj");
        fileChooser.setFileFilter(acceptableExtension);

        int userChoice = fileChooser.showOpenDialog(this);

        if (userChoice == JFileChooser.APPROVE_OPTION) {
            try {
                File selectedFile = fileChooser.getSelectedFile();
                String wkt = Files.readString(Paths.get(selectedFile.getAbsolutePath()));
                targetWktTextArea.setText(wkt);
                fileBrowseButton.setPersistedBrowseLocation(selectedFile);
            } catch (IOException e) { logger.log(Level.WARNING, e.toString()); }
        }
    }

    private void targetWktProjectionAction() {
        ProjectionsComboBox projectionComboBox = new ProjectionsComboBox();
        projectionComboBox.setSelectedItem("SHG");

        JDialog projectionDialog = new JDialog();
        projectionDialog.setSize(240, 100);
        projectionDialog.setIconImage(IconResources.loadImage("images/vortex_black.png"));
        projectionDialog.setLayout(new BorderLayout());
        projectionDialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        projectionDialog.setLocationRelativeTo(this);

        JPanel contentPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        contentPanel.add(new JLabel(Text.format("ImportMetWizSelectProjection")));
        contentPanel.add(projectionComboBox);
        projectionDialog.add(BorderLayout.CENTER, contentPanel);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton projectionOkButton = new JButton(Text.format("ImportMetWizOk"));
        projectionOkButton.addActionListener(evt -> {
            targetWktTextArea.setText(projectionComboBox.getWkt());
            projectionDialog.setVisible(false);
            projectionDialog.dispose();
        });
        JButton projectionCancelButton = new JButton(Text.format("ImportMetWizCancel"));
        projectionCancelButton.addActionListener(evt -> { projectionDialog.setVisible(false); projectionDialog.dispose();});
        buttonPanel.add(projectionOkButton);
        buttonPanel.add(projectionCancelButton);
        projectionDialog.add(buttonPanel, BorderLayout.SOUTH);

        projectionDialog.setVisible(true);
    }

    private void targetCellSizeSelectionAction() {
        CellSizesComboBox cellSizeComboBox = new CellSizesComboBox();
        cellSizeComboBox.setSelectedItem("2000");

        JDialog cellSizeDialog = new JDialog();
        cellSizeDialog.setSize(240, 100);
        cellSizeDialog.setIconImage(IconResources.loadImage("images/vortex_black.png"));
        cellSizeDialog.setLayout(new BorderLayout());
        cellSizeDialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        cellSizeDialog.setLocationRelativeTo(this);

        JPanel contentPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        contentPanel.add(new JLabel(Text.format("ImportMetWizSelectCellSize")));
        contentPanel.add(cellSizeComboBox);
        cellSizeDialog.add(BorderLayout.CENTER, contentPanel);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton projectionOkButton = new JButton(Text.format("ImportMetWizOk"));
        projectionOkButton.addActionListener(evt -> {
            String chosenCellSize = String.valueOf(cellSizeComboBox.getSelectedItem());
            targetCellSizeTextField.setText(chosenCellSize);
            cellSizeDialog.setVisible(false);
            cellSizeDialog.dispose();
        });
        JButton projectionCancelButton = new JButton(Text.format("ImportMetWizCancel"));
        projectionCancelButton.addActionListener(evt -> { cellSizeDialog.setVisible(false); cellSizeDialog.dispose();});
        buttonPanel.add(projectionOkButton);
        buttonPanel.add(projectionCancelButton);
        cellSizeDialog.add(buttonPanel, BorderLayout.SOUTH);

        cellSizeDialog.setVisible(true);
    }

    private DefaultListModel<String> getDefaultListModel(JList<String> list) {
        ListModel<String> listModel = list.getModel();
        if (!(listModel instanceof DefaultListModel)) {
            logger.log(Level.SEVERE, list.getName() + " may have not been initialized");
            return null;
        }

        return (DefaultListModel<String>) listModel;
    }

    private void showUnsupportedArchiveError(List<String> errors) {
        String suggestion = Message.format("error_archive_file_suggestion");

        List<String> lines = new ArrayList<>(errors);
        lines.add(suggestion);

        JTextArea textArea = new JTextArea(String.join("\n", lines));
        textArea.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(400, 200));

        JOptionPane.showMessageDialog(
                this,
                scrollPane,
                Text.format("ImportMetWiz_UnsupportedArchive_Title"),
                JOptionPane.ERROR_MESSAGE
        );
    }

    /* Add main for quick UI Testing */
    static public void main(String[] args) {
        FlatLightLaf.setup();
        ImportMetWizard metWizard = new ImportMetWizard(null);
        metWizard.buildAndShowUI();
    }

}
