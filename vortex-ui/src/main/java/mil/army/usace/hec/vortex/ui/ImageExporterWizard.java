package mil.army.usace.hec.vortex.ui;

import mil.army.usace.hec.vortex.io.BatchExporter;
import mil.army.usace.hec.vortex.io.ImageFileType;
import mil.army.usace.hec.vortex.ui.util.FileSaveUtil;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

public class ImageExporterWizard extends ProcessingWizard {
	private SourceFileSelectionPanel sourceFileSelectionPanel;

	private JTextField sourceFileTextField;
	private JTextField destinationDirectoryTextField;
	private JTextField filenamePrefixTextField;
	private JComboBox<ImageFileType> formatComboBox;
	private JList<String> chosenSourceGridsList;

	public ImageExporterWizard(Frame frame) {
        super(frame);
    }

    @Override
    protected String getTitlePropertyKey() {
        return "ImageExporterWiz_Title";
    }

    @Override
    protected List<JPanel> createStepPanels() {
        return List.of(
                stepOnePanel(),
                stepTwoPanel(),
                createProgressPanel(),
                createProgressPanel()
        );
    }

    @Override
    protected int getLastInteractiveStep() {
        return 1;
    }

    @Override
    protected boolean validateStep(int stepIndex) {
        return switch (stepIndex) {
            case 0 -> sourceFileSelectionPanel.validateInput();
            case 1 -> validateDestination();
            default -> true;
        };
    }

    @Override
    protected void submitStep(int stepIndex) {
        switch (stepIndex) {
            case 0 -> submitStepOne();
            case 1 -> {
                SwingWorker<Void, Void> task = new SwingWorker<>() {
                    @Override
                    protected Void doInBackground() {
                        exportImageTask();
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
    }

    @Override
    protected void clearWizardState() {
		sourceFileSelectionPanel.clear();
		destinationDirectoryTextField.setText("");
		filenamePrefixTextField.setText("");
	}

    @Override
    protected void showSaveResult() {
        String savedFile = destinationDirectoryTextField.getText();
        FileSaveUtil.showDirectoryLocation(this, Path.of(savedFile));
	}

	private JPanel stepOnePanel() {
		sourceFileSelectionPanel = new SourceFileSelectionPanel(ImageExporterWizard.class.getName());
		sourceFileTextField = sourceFileSelectionPanel.getSourceFileTextField();
		chosenSourceGridsList = sourceFileSelectionPanel.getChosenSourceGridsList();
		return sourceFileSelectionPanel;
	}

	private void submitStepOne() {
		String sourceFile = sourceFileTextField.getText();
		String fileSeparator = FileSystems.getDefault().getSeparator();
		String prefix = sourceFile.substring(sourceFile.lastIndexOf(fileSeparator) + 1);
		String prefixSansExt = prefix.substring(0, prefix.lastIndexOf('.'));
		destinationDirectoryTextField.setText(sourceFile.substring(0, sourceFile.lastIndexOf(fileSeparator)));
		filenamePrefixTextField.setText(prefixSansExt);
	}

	private JPanel stepTwoPanel() {
		JPanel destinationSelectionPanel = destinationSelectionPanel();
		JPanel filenamePrefixPanel = filenamePrefixPanel();
        JPanel formatPanel = formatPanel();

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
		stepTwoPanel.add(destinationSelectionPanel, gridBagConstraints);

        gridBagConstraints.gridy = 1;
		stepTwoPanel.add(filenamePrefixPanel, gridBagConstraints);

        gridBagConstraints.gridy = 2;
		stepTwoPanel.add(formatPanel, gridBagConstraints);

        gridBagConstraints.gridy = 3;
		gridBagConstraints.weighty = 1;
		gridBagConstraints.fill = GridBagConstraints.BOTH;
		stepTwoPanel.add(new JPanel(), gridBagConstraints);

        return stepTwoPanel;
	}

    private JPanel destinationSelectionPanel() {
		JLabel destinationDirectoryLabel = new JLabel(Text.format("ImageExporterWiz_DestinationDirectory_L"));
		JPanel destinationDirectoryLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		destinationDirectoryLabelPanel.add(destinationDirectoryLabel);

        JPanel destinationDirectoryTextFieldPanel = new JPanel();
		destinationDirectoryTextFieldPanel.setLayout(new BoxLayout(destinationDirectoryTextFieldPanel, BoxLayout.X_AXIS));
		destinationDirectoryTextFieldPanel.add(Box.createRigidArea(new Dimension(4,0)));

        destinationDirectoryTextField = new JTextField();
		destinationDirectoryTextField.setColumns(0);
		destinationDirectoryTextField.setText(sourceFileTextField.getText());
		destinationDirectoryTextFieldPanel.add(destinationDirectoryTextField);
		destinationDirectoryTextFieldPanel.add(Box.createRigidArea(new Dimension(4,0)));

        FileBrowseButton selectDestinationBrowseButton = new FileBrowseButton(this.getClass().getName(), "");
		selectDestinationBrowseButton.setIcon(IconResources.loadIcon("images/Open16.gif"));
		selectDestinationBrowseButton.setPreferredSize(new Dimension(22,22));
		selectDestinationBrowseButton.addActionListener(evt -> selectDestinationBrowseAction(selectDestinationBrowseButton));
		destinationDirectoryTextFieldPanel.add(selectDestinationBrowseButton);

        JPanel selectDestinationSectionPanel = new JPanel();
		selectDestinationSectionPanel.setLayout(new BoxLayout(selectDestinationSectionPanel, BoxLayout.Y_AXIS));
		selectDestinationSectionPanel.add(destinationDirectoryLabelPanel);
		selectDestinationSectionPanel.add(destinationDirectoryTextFieldPanel);

        return selectDestinationSectionPanel;
	}

    private void selectDestinationBrowseAction(FileBrowseButton fileBrowseButton) {
		JFileChooser fileChooser = new JFileChooser(fileBrowseButton.getPersistedBrowseLocation());
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		if (fileBrowseButton.getPersistedBrowseLocation() != null && fileBrowseButton.getPersistedBrowseLocation().exists()){
			fileChooser.setCurrentDirectory(fileBrowseButton.getPersistedBrowseLocation());
		} else {
			fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
		}

        int userChoice = fileChooser.showOpenDialog(frame);

        if(userChoice == JFileChooser.APPROVE_OPTION) {
			File selectedFile = fileChooser.getCurrentDirectory();
			String selectedPath = selectedFile.getAbsolutePath();
			selectedPath = selectedPath + FileSystems.getDefault().getSeparator();
			destinationDirectoryTextField.setText(selectedPath);
			File finalFile = new File(selectedPath);
			fileBrowseButton.setPersistedBrowseLocation(finalFile);
		}
	}

    private JPanel filenamePrefixPanel() {
		JLabel filenamePrefixLabel = new JLabel(Text.format("ImageExporterWiz_FilenamePrefix_L"));
		JPanel filenamePrefixLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		filenamePrefixLabelPanel.add(filenamePrefixLabel);

        JPanel filenamePrefixTextFieldPanel = new JPanel();
		filenamePrefixTextFieldPanel.setLayout(new BoxLayout(filenamePrefixTextFieldPanel, BoxLayout.X_AXIS));
		filenamePrefixTextFieldPanel.add(Box.createRigidArea(new Dimension(4,0)));

        filenamePrefixTextField = new JTextField();
		filenamePrefixTextField.setColumns(0);
		filenamePrefixTextFieldPanel.add(filenamePrefixTextField);
		filenamePrefixTextFieldPanel.add(Box.createRigidArea(new Dimension(4,0)));

        JPanel filenamePrefixPanel = new JPanel();
		filenamePrefixPanel.setLayout(new BoxLayout(filenamePrefixPanel, BoxLayout.Y_AXIS));
		filenamePrefixPanel.add(filenamePrefixLabelPanel);
		filenamePrefixPanel.add(filenamePrefixTextFieldPanel);

        return filenamePrefixPanel;
	}

    private JPanel formatPanel() {
		JLabel formatLabel = new JLabel(Text.format("ImageExporterWiz_Format_L"));
		JPanel formatLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		formatLabelPanel.add(formatLabel);

        JPanel formatComboBoxPanel = new JPanel();
		formatComboBoxPanel.setLayout(new BoxLayout(formatComboBoxPanel, BoxLayout.X_AXIS));
		formatComboBoxPanel.add(Box.createRigidArea(new Dimension(4,0)));

        ImageFileType[] fileTypes = ImageFileType.values();
		formatComboBox = new JComboBox<>(fileTypes);
		formatComboBox.setSelectedIndex(0);
		formatComboBoxPanel.add(formatComboBox);
		formatComboBoxPanel.add(Box.createRigidArea(new Dimension(4,0)));

        JPanel formatPanel = new JPanel();
		formatPanel.setLayout(new BoxLayout(formatPanel, BoxLayout.Y_AXIS));
		formatPanel.add(formatLabelPanel);
		formatPanel.add(formatComboBoxPanel);

        return formatPanel;
	}

    private boolean validateDestination() {
		String destinationFile = destinationDirectoryTextField.getText();
		if(destinationFile == null || destinationFile.isEmpty() ) {
			JOptionPane.showMessageDialog(this, Text.format("Error_DestinationRequired"),
					Text.format("Error_MissingField_Title"), JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return true;
	}

    private void exportImageTask() {
		String pathToSource = sourceFileTextField.getText();
		List<String> chosenSourceGrids = getItemsInList(chosenSourceGridsList);
		if (chosenSourceGrids == null) return;
		Set<String> variables = new HashSet<>(chosenSourceGrids);

		BatchExporter batchExporter = BatchExporter.builder()
				.pathToSource(pathToSource)
				.variables(variables)
				.filenamePrefix(filenamePrefixTextField.getText())
				.destinationDir(destinationDirectoryTextField.getText())
                .imageFileType((ImageFileType) formatComboBox.getSelectedItem())
				.build();

        batchExporter.addPropertyChangeListener(createProgressListener());

		SwingWorker<Void, Void> task = new SwingWorker<>() {
			@Override
			protected Void doInBackground() {
				batchExporter.run();
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

        ImageExporterWizard imageExporterWizard = new ImageExporterWizard(null);
		imageExporterWizard.buildAndShowUI();
	}
}
