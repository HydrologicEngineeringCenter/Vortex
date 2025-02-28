package mil.army.usace.hec.vortex.ui;

import mil.army.usace.hec.vortex.io.DataReader;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;
import java.util.*;

public class SourceFileSelectionPanel extends JPanel {
    private final String className;
    private JTextField sourceFileTextField;
    private JList<String> availableSourceGridsList;
    private JList<String> chosenSourceGridsList;

    public SourceFileSelectionPanel(String className) {
        this.className = className;
        /* selectSourceFilePanel and selectSourceGridsPanel*/
        JPanel selectSourceFilePanel = sourceFilePanel();
        JPanel selectSourceGridsPanel = sourceGridPanel();

        /* Setting GridBagLayout */
        GridBagLayout gridBagLayout = new GridBagLayout();
        gridBagLayout.columnWidths = new int[]{this.getPreferredSize().width, 0};
        gridBagLayout.rowHeights = new int[]{50, 100};
        gridBagLayout.columnWeights = new double[]{1.0, Double.MIN_VALUE};
        gridBagLayout.rowWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};

        /* Adding Panels */
        setLayout(gridBagLayout);
        setBorder(BorderFactory.createEmptyBorder(5,9,5,8));

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(0, 0, 5, 0);
        gridBagConstraints.gridx = 0;

        gridBagConstraints.gridy = 0;
        add(selectSourceFilePanel, gridBagConstraints);

        gridBagConstraints.gridy = 1;
        gridBagConstraints.weighty = 1;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        add(selectSourceGridsPanel, gridBagConstraints);
    }

    private JPanel sourceFilePanel() {
        /* selectSourceFileLabel */
        JLabel selectSourceFileLabel = new JLabel(TextProperties.getInstance().getProperty("SourceFilePanel_SelectSourceFile_L"));
        selectSourceFileLabel.setBorder(BorderFactory.createEmptyBorder(0,0,5,0));

        /* TextField and Browse Panel */
        JPanel sourceFileTextFieldPanel = new JPanel();
        sourceFileTextFieldPanel.setLayout(new BoxLayout(sourceFileTextFieldPanel, BoxLayout.X_AXIS));

        sourceFileTextField = new JTextField();
        sourceFileTextField.setColumns(0);
        sourceFileTextFieldPanel.add(sourceFileTextField);

        sourceFileTextFieldPanel.add(Box.createRigidArea(new Dimension(8,0)));

        String uniqueId = className + ".sourceFile";
        FileBrowseButton sourceFileBrowseButton = new FileBrowseButton(uniqueId, "");
        sourceFileBrowseButton.setIcon(IconResources.loadIcon("images/Open16.gif"));
        sourceFileBrowseButton.setPreferredSize(new Dimension(22,22));
        sourceFileBrowseButton.addActionListener(evt -> sourceFileBrowseAction(sourceFileBrowseButton));
        sourceFileTextFieldPanel.add(sourceFileBrowseButton);

        /* Adding everything together */
        JPanel sourceFilePanel = new JPanel(new BorderLayout());
        sourceFilePanel.add(selectSourceFileLabel, BorderLayout.NORTH);
        sourceFilePanel.add(sourceFileTextFieldPanel, BorderLayout.CENTER);

        return sourceFilePanel;
    }

    private JPanel sourceGridPanel() {
        /* selectSourceGridsLabel */
        JLabel selectSourceGridsLabel = new JLabel(TextProperties.getInstance().getProperty("SourceFilePanel_SelectSourceGrids_L"));
        selectSourceGridsLabel.setBorder(BorderFactory.createEmptyBorder(0,0,5,0));

        /* Available Source Grids List */
        DefaultListModel<String> availableGridsListModel = new DefaultListModel<>();
        availableSourceGridsList = new JList<>(availableGridsListModel);
        availableSourceGridsList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(e.getClickCount() == 2) { addSelectedVariables(); }
            }
        });
        JScrollPane leftScrollPanel = new JScrollPane();
        leftScrollPanel.setViewportView(availableSourceGridsList);

        /* Chosen Source Grids List */
        DefaultListModel<String> chosenGridsListModel = new DefaultListModel<>();
        chosenSourceGridsList = new JList<>(chosenGridsListModel);
        chosenSourceGridsList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(e.getClickCount() == 2) { removeSelectedVariables(); }
            }
        });
        JScrollPane rightScrollPanel = new JScrollPane();
        rightScrollPanel.setViewportView(chosenSourceGridsList);

        /* Transfer Buttons Panel */
        JPanel transferButtonsPanel = new JPanel();
        transferButtonsPanel.setLayout(new BoxLayout(transferButtonsPanel, BoxLayout.Y_AXIS));
        JButton addVariableButton = new JButton(IconResources.loadIcon("images/right-arrow-24.png"));
        addVariableButton.setPreferredSize(new Dimension(22,22));
        addVariableButton.setMaximumSize(new Dimension(22,22));
        addVariableButton.addActionListener(evt -> addSelectedVariables());
        JButton removeVariableButton = new JButton(IconResources.loadIcon("images/left-arrow-24.png"));
        removeVariableButton.setPreferredSize(new Dimension(22,22));
        removeVariableButton.setMaximumSize(new Dimension(22,22));
        removeVariableButton.addActionListener(evt -> removeSelectedVariables());
        transferButtonsPanel.add(addVariableButton);
        transferButtonsPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        transferButtonsPanel.add(removeVariableButton);

        /* Adding grid lists and transfer buttons to sourceGridsSelectionPanel */
        JPanel sourceGridsSelectionPanel = new JPanel();
        sourceGridsSelectionPanel.setLayout(new BoxLayout(sourceGridsSelectionPanel, BoxLayout.X_AXIS));
        sourceGridsSelectionPanel.add(leftScrollPanel);
        sourceGridsSelectionPanel.add(Box.createRigidArea(new Dimension(4,0)));
        sourceGridsSelectionPanel.add(transferButtonsPanel);
        sourceGridsSelectionPanel.add(Box.createRigidArea(new Dimension(4,0)));
        sourceGridsSelectionPanel.add(rightScrollPanel);

        /* Setting Preferred Sizes of Components */
        int mainContentWidth = sourceGridsSelectionPanel.getPreferredSize().width;
        int scrollPanelWidth = mainContentWidth * 45 / 100;
        leftScrollPanel.setPreferredSize(new Dimension(scrollPanelWidth, 0));
        rightScrollPanel.setPreferredSize(new Dimension(scrollPanelWidth, 0));

        /* Adding everything together */
        JPanel sourceGridsPanel = new JPanel(new BorderLayout());
        sourceGridsPanel.add(selectSourceGridsLabel, BorderLayout.NORTH);
        sourceGridsPanel.add(sourceGridsSelectionPanel, BorderLayout.CENTER);

        return sourceGridsPanel;
    }

    private void addSelectedVariables() {
        List<String> selectedVariables = availableSourceGridsList.getSelectedValuesList();

        /* Adding to Right Variables List */
        DefaultListModel<String> defaultRightModel = Util.getDefaultListModel(chosenSourceGridsList);
        if (defaultRightModel == null) return;

        /* MUST be a LinkedHashSet to preserve order */
        Set<String> rightVariables = new LinkedHashSet<>(Collections.list(defaultRightModel.elements()));
        rightVariables.addAll(selectedVariables);

        String fileName = sourceFileTextField.getText();
        if (fileName.matches(".*\\.dss"))
            Util.sortDssVariables(rightVariables);

        defaultRightModel.clear();
        defaultRightModel.addAll(rightVariables);

        /* Removing from Left Variables List */
        DefaultListModel<String> defaultLeftModel = Util.getDefaultListModel(availableSourceGridsList);
        if (defaultLeftModel == null) return;

        /* MUST be a LinkedHashSet to preserve order */
        Set<String> leftVariables = new LinkedHashSet<>(Collections.list(defaultLeftModel.elements()));
        selectedVariables.forEach(leftVariables::remove);
        defaultLeftModel.clear();
        defaultLeftModel.addAll(leftVariables);
    }

    private void removeSelectedVariables() {
        List<String> selectedVariables = chosenSourceGridsList.getSelectedValuesList();

        /* Adding to Left Variables List */
        DefaultListModel<String> defaultLeftModel = Util.getDefaultListModel(availableSourceGridsList);
        if (defaultLeftModel == null) return;

        /* MUST be a LinkedHashSet to preserve order */
        Set<String> leftVariables = new LinkedHashSet<>(Collections.list(defaultLeftModel.elements()));
        leftVariables.addAll(selectedVariables);

        String fileName = sourceFileTextField.getText();
        if (fileName.matches(".*\\.dss"))
            Util.sortDssVariables(leftVariables);

        defaultLeftModel.clear();
        defaultLeftModel.addAll(leftVariables);

        /* Removing from Right Variables List */
        DefaultListModel<String> defaultRightModel = Util.getDefaultListModel(chosenSourceGridsList);
        if (defaultRightModel == null) return;

        /* MUST be a LinkedHashSet to preserve order */
        Set<String> rightVariables = new LinkedHashSet<>(Collections.list(defaultRightModel.elements()));
        selectedVariables.forEach(rightVariables::remove);
        defaultRightModel.clear();
        defaultRightModel.addAll(rightVariables);
    }

    private void sourceFileBrowseAction(FileBrowseButton fileBrowseButton) {
        JFileChooser fileChooser = new JFileChooser(fileBrowseButton.getPersistedBrowseLocation());
        fileChooser.setMultiSelectionEnabled(true);

        // Configuring fileChooser dialog
        fileChooser.setAcceptAllFileFilterUsed(false);
        FileNameExtensionFilter acceptableExtension = new FileNameExtensionFilter("All recognized files",
                "nc", "nc4", "hdf", "h5", "grib", "gb2", "grb2", "grib2", "grb", "asc", "tif", "tiff", "dss");
        fileChooser.addChoosableFileFilter(acceptableExtension);

        // Pop up fileChooser dialog
        int userChoice = fileChooser.showOpenDialog(this);

        // Deal with user's choice
        if(userChoice == JFileChooser.APPROVE_OPTION) {
            /* Set path to text field and save browse location */
            File selectedFile = fileChooser.getSelectedFile();
            sourceFileTextField.setText(selectedFile.getAbsolutePath());
            fileBrowseButton.setPersistedBrowseLocation(selectedFile);

            /* Populate variables for available source grids list */
            Set<String> variables = DataReader.getVariables(selectedFile.toString());
            DefaultListModel<String> defaultListModel = Util.getDefaultListModel(availableSourceGridsList);

            if(defaultListModel != null) {
                defaultListModel.clear();
                /* MUST be a LinkedHashSet to preserve order */
                Set<String> sorted = new LinkedHashSet<>(variables);
                if (selectedFile.toString().matches(".*\\.dss"))
                    Util.sortDssVariables(sorted);
                defaultListModel.addAll(sorted);
            }
        } // If: User selected OK -> Populate Available Source Grids
    }

    public JList<String> getAvailableSourceGridsList() {
        return availableSourceGridsList;
    }

    public JList<String> getChosenSourceGridsList() {
        return chosenSourceGridsList;
    }

    public JTextField getSourceFileTextField() {
        return sourceFileTextField;
    }

    public boolean validateInput() {
        /* Popup Alert of Missing Inputs */
        if(sourceFileTextField.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Input dataset is required.",
                    "Error: Missing Field", JOptionPane.ERROR_MESSAGE);

            return false;
        }

        /* Popup Alert of No Variables Selected */
        DefaultListModel<String> chosenGridsModel = Util.getDefaultListModel(chosenSourceGridsList);
        if(chosenGridsModel == null || chosenGridsModel.isEmpty()) {
            JOptionPane.showMessageDialog(this, "At least one variable must be selected.",
                    "Error: No Variables Selected", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        return true;
    }

    public void clear() {
        sourceFileTextField.setText("");
        Objects.requireNonNull(Util.getDefaultListModel(availableSourceGridsList)).clear();
        Objects.requireNonNull(Util.getDefaultListModel(chosenSourceGridsList)).clear();
    }
}
