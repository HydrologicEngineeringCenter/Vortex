package mil.army.usace.hec.vortex.ui;

import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.io.DataReader;
import mil.army.usace.hec.vortex.io.DataWriter;
import mil.army.usace.hec.vortex.io.ImageFileType;
import mil.army.usace.hec.vortex.ui.util.FileSaveUtil;
import mil.army.usace.hec.vortex.util.ImageUtils;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ImageExporterWizard extends VortexWizard {
	private final Frame frame;
	private SourceFileSelectionPanel sourceFileSelectionPanel;
	
	private Container contentCards;
	private CardLayout cardLayout;
	private JButton backButton;
	private JButton nextButton;
	private JButton cancelButton;
	private int cardNumber;
	
	private JTextField sourceFileTextField;
	private JTextField destinationDirectoryTextField;
	private JTextField filenamePrefixTextField;
	private JComboBox<ImageFileType> formatComboBox;
	private JList<String> chosenSourceGridsList;
	private JProgressBar progressBar;
	
	private static final String NEXT = TextProperties.getInstance().getProperty("ImageExporterWiz_Next");
	private static final boolean IS_VALID = true;
	
	private static final Logger logger = Logger.getLogger(ImageExporterWizard.class.getName());
	
	public ImageExporterWizard(Frame frame) {
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
		this.setTitle(TextProperties.getInstance().getProperty("ImageExporterWiz_Title"));
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
		
		/* Adding Step Content Panels to contentCards */
		contentCards.add("Step One", stepOnePanel());
		contentCards.add("Step Two", stepTwoPanel());
		contentCards.add("Step Three", stepThreePanel());
		contentCards.add("Step Four", stepFourPanel());
	}
	
	private void initializeButtonPanel() {
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		
		/* Back Button */
		backButton = new JButton(TextProperties.getInstance().getProperty("ImageExporterWiz_Back"));
		backButton.setToolTipText(TextProperties.getInstance().getProperty("ImageExporterWiz_Back_TT"));
		backButton.setEnabled(false);
		backButton.addActionListener(evt -> backAction());
		
		/* Next Button */
		nextButton = new JButton(NEXT);
		nextButton.setToolTipText(TextProperties.getInstance().getProperty("ImageExporterWiz_Next_TT"));
		nextButton.addActionListener(evt -> {
			if(nextButton.getText().equals(TextProperties.getInstance().getProperty("ImageExporterWiz_Restart"))) { restartAction(); }
			else if(nextButton.getText().equals(NEXT)) { nextAction(); }
		});
		
		/* Cancel Button */
		cancelButton = new JButton(TextProperties.getInstance().getProperty("ImageExporterWiz_Cancel"));
		cancelButton.setToolTipText(TextProperties.getInstance().getProperty("ImageExporterWiz_Cancel_TT"));
		cancelButton.addActionListener(evt -> closeAction());
		
		/* Adding Buttons to NavigationPanel */
		buttonPanel.add(backButton);
		buttonPanel.add(nextButton);
		buttonPanel.add(cancelButton);
		
		/* Add buttonPanel to SanitizerWizard */
		this.add(buttonPanel, BorderLayout.SOUTH);
	}
	
	private void nextAction() {
		if(!validateCurrentStep()) return;
		submitCurrentStep();
		cardNumber++;
		backButton.setEnabled(true);
		
		if(cardNumber == 2) {
			backButton.setEnabled(false);
			nextButton.setEnabled(false);
		} // If: Step Four (Processing...) Then disable Back and Next button
		
		if(cardNumber == 3) {
			backButton.setVisible(false);
			nextButton.setText(TextProperties.getInstance().getProperty("ImageExporterWiz_Restart"));
			nextButton.setToolTipText(TextProperties.getInstance().getProperty("ImageExporterWiz_Restart_TT"));
			nextButton.setEnabled(true);
			cancelButton.setText(TextProperties.getInstance().getProperty("ImageExporterWiz_Close"));
			cancelButton.setToolTipText(TextProperties.getInstance().getProperty("ImageExporterWiz_Close_TT"));
		} // If: Step Five (Change Cancel to Close)
		
		cardLayout.next(contentCards);
	}
	
	private void backAction() {
		cardNumber--;
		if(cardNumber == 0) {
			backButton.setEnabled(false);
		}
		cardLayout.previous(contentCards);
	}
	
	private void restartAction() {
		cardNumber = 0;
		cardLayout.first(contentCards);
		
		/* Resetting Buttons */
		backButton.setVisible(true);
		backButton.setEnabled(false);
		
		nextButton.setEnabled(true);
		nextButton.setText(NEXT);
		nextButton.setToolTipText(TextProperties.getInstance().getProperty("ImageExporterWiz_Next_TT"));
		
		cancelButton.setText(TextProperties.getInstance().getProperty("ImageExporterWiz_Cancel"));
		cancelButton.setToolTipText(TextProperties.getInstance().getProperty("ImageExporterWiz_Cancel_TT"));
		
		/* Clearing Step One Panel */
		sourceFileSelectionPanel.clear();
		
		/* Clearing Step Two Panel */
		destinationDirectoryTextField.setText("");
		filenamePrefixTextField.setText("");
		
		/* Clearing Step Three Panel */
		progressBar.setIndeterminate(true);
		progressBar.setStringPainted(false);
		progressBar.setValue(0);
		progressBar.setString("0%");
	}
	
	private boolean validateCurrentStep() {
		switch(cardNumber) {
			case 0: return validateStepOne();
			case 1: return validateStepTwo();
			case 2: return IS_VALID;
			default: return unknownStepError();
		}
	}
	
	private void submitCurrentStep() {
		switch(cardNumber) {
			case 0: submitStepOne(); break;
			case 1: submitStepTwo(); break;
			case 2: submitStepThree(); break;
			default: unknownStepError(); break;
		}
	}
	
	private boolean unknownStepError() {
		logger.log(Level.SEVERE, "Unknown Step in Wizard");
		return false;
	}
	
	private JPanel stepOnePanel() {
		sourceFileSelectionPanel = new SourceFileSelectionPanel(ImageExporterWizard.class.getName());
		sourceFileTextField = sourceFileSelectionPanel.getSourceFileTextField();
		chosenSourceGridsList = sourceFileSelectionPanel.getChosenSourceGridsList();
		return sourceFileSelectionPanel;
	}
	
	private boolean validateStepOne() {
		return sourceFileSelectionPanel.validateInput();
	}
	
	private void submitStepOne() {
		String sourceFile = sourceFileTextField.getText();
		String fileSeparator = System.getProperty("file.separator");
		String prefix = sourceFile.substring(sourceFile.lastIndexOf(fileSeparator) + 1);
		String prefixSansExt = prefix.substring(0, prefix.lastIndexOf('.'));
		destinationDirectoryTextField.setText(sourceFile.substring(0, sourceFile.lastIndexOf(fileSeparator)));
		filenamePrefixTextField.setText(prefixSansExt);
	}
	
	private JPanel stepTwoPanel() {
		/* Destination directory Panel */
		JPanel destinationSelectionPanel = destinationSelectionPanel();
		
		/* Filename Prefix Panel */
		JPanel filenamePrefixPanel = filenamePrefixPanel();
		
		/* Format Panel */
		JPanel formatPanel = formatPanel();		
		
		/* Setting GridBagLayout for stepTwoPanel */
		GridBagLayout gridBagLayout = new GridBagLayout();
		
		/* Adding Panels to stepTwoPanel */
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
		JLabel destinationDirectoryLabel = new JLabel(TextProperties.getInstance().getProperty("ImageExporterWiz_DestinationDirectory_L"));
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
		
		// Pop up fileChooser dialog
		int userChoice = fileChooser.showOpenDialog(frame);
		
		// Deal with user's choice
		if(userChoice == JFileChooser.APPROVE_OPTION) {
			File selectedFile = fileChooser.getCurrentDirectory();
			String selectedPath = selectedFile.getAbsolutePath();
			selectedPath = selectedPath + System.getProperty("file.separator");
			destinationDirectoryTextField.setText(selectedPath);
			File finalFile = new File(selectedPath);
			fileBrowseButton.setPersistedBrowseLocation(finalFile);
		}
	}
	
	private JPanel filenamePrefixPanel() {
		JLabel filenamePrefixLabel = new JLabel(TextProperties.getInstance().getProperty("ImageExporterWiz_FilenamePrefix_L"));
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
		JLabel formatLabel = new JLabel(TextProperties.getInstance().getProperty("ImageExporterWiz_Format_L"));
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
	
	private boolean validateStepTwo() {
		String destinationFile = destinationDirectoryTextField.getText();
		if(destinationFile == null || destinationFile.isEmpty() ) {
			JOptionPane.showMessageDialog(this, "Destination file is required.",
					"Error: Missing Field", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		
		return true;
	}
	
	private void submitStepTwo() {
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
	
	private ImageFileType getFileType() {
		return (ImageFileType) formatComboBox.getSelectedItem();
	}
	
	private JPanel stepThreePanel() {
		JPanel stepThreePanel = new JPanel(new GridBagLayout());
		
		JPanel insidePanel = new JPanel();
		insidePanel.setLayout(new BoxLayout(insidePanel, BoxLayout.Y_AXIS));
		
		JLabel processingLabel = new JLabel(TextProperties.getInstance().getProperty("ImageExporterWiz_Processing_L"));
		JPanel processingPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		processingPanel.add(processingLabel);
		insidePanel.add(processingPanel);
		
		progressBar = new JProgressBar(0, 100);
		progressBar.setIndeterminate(true);
		progressBar.setStringPainted(false);
		JPanel progressPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		progressPanel.add(progressBar);
		insidePanel.add(progressPanel);
		
		stepThreePanel.add(insidePanel);
		
		return stepThreePanel;
	}
	
	private void submitStepThree() { 
		//No validation required for this step 
	}
	
	private JPanel stepFourPanel() {
		JPanel stepFourPanel = new JPanel(new GridBagLayout());
		JLabel completeLabel = new JLabel(TextProperties.getInstance().getProperty("ImageExporterWiz_Complete_L"));
		stepFourPanel.add(completeLabel);
		return stepFourPanel;
	}
	
	private void exportImageTask() {
		String pathToSource = sourceFileTextField.getText();
		List<String> chosenSourceGrids = getItemsInList(chosenSourceGridsList);
		if (chosenSourceGrids == null) return;
		Set<String> variables = new HashSet<>(chosenSourceGrids);
		
		variables.forEach(variable -> {
			List<VortexData> grids = DataReader.builder()
					.path(pathToSource)
					.variable(variable)
					.build()
					.getDtos();
			
			grids.forEach(grid -> {
				String fileName = ImageUtils.generateFileName(filenamePrefixTextField.getText(), (VortexGrid) grid, getFileType());
				Path destination = Paths.get(destinationDirectoryTextField.getText(), fileName);
				DataWriter writer = DataWriter.builder()
						.data(grids)
						.destination(destination)
						.build();
				
				writer.write();
			});
		});
	}
	
	private List<String> getItemsInList(JList<String> list) {
		DefaultListModel<String> defaultRightModel = Util.getDefaultListModel(list);
		if(defaultRightModel == null) { return Collections.emptyList(); }
		return Collections.list(defaultRightModel.elements());
	}
	
	private void closeAction() {
		ImageExporterWizard.this.setVisible(false);
		ImageExporterWizard.this.dispose();
		String savedFile = destinationDirectoryTextField.getText();
		FileSaveUtil.showDirectoryLocation(ImageExporterWizard.this, Path.of(savedFile));
	}
	
	/* Add main for quick UI Testing */
	public static void main(String[] args) {
		try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
		catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) { e.printStackTrace(); }
		
		ImageExporterWizard imageExporterWizard = new ImageExporterWizard(null);
		imageExporterWizard.buildAndShowUI();
	}
}