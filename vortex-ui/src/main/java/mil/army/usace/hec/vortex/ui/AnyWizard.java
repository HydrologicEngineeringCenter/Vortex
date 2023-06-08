package mil.army.usace.hec.vortex.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowListener;

public class AnyWizard extends VortexWizard {
    private final Frame frame;

    private JRadioButton calculatorButton;
    private JRadioButton clipperButton;
    private JRadioButton gridToPointButton;
    private JRadioButton imageExporterButton;
    private JRadioButton sanitizerButton;
    private JRadioButton shifterButton;

    AnyWizard(Frame frame) {
        super();
        this.frame = frame;
    }

    @Override
    public void buildAndShowUI() {
        setTitle(TextProperties.getInstance().getProperty("AnyWiz_Title"));
        setIconImage(IconResources.loadImage("images/vortex_black.png"));
        setMinimumSize(new Dimension(600, 400));
        setLocation(getPersistedLocation());
        setSize(getPersistedSize());
        setLayout(new BorderLayout());
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        JLabel selectionLabel = new JLabel(TextProperties.getInstance().getProperty("AnyWiz_Select_L"));
        selectionLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 0));
        add(selectionLabel, BorderLayout.NORTH);

        initializeSelectionPanel();

        initializeButtonPanel();

        setVisible(true);
    }

    private void initializeSelectionPanel() {
        JRadioButton importerButton = new JRadioButton(TextProperties.getInstance().getProperty("AnyWiz_Importer_L"));
        calculatorButton = new JRadioButton(TextProperties.getInstance().getProperty("AnyWiz_Calculator_L"));
        clipperButton = new JRadioButton(TextProperties.getInstance().getProperty("AnyWiz_Clipper_L"));
        gridToPointButton = new JRadioButton(TextProperties.getInstance().getProperty("AnyWiz_GridToPoint_L"));
        imageExporterButton = new JRadioButton(TextProperties.getInstance().getProperty("AnyWiz_ImageExporter_L"));
        sanitizerButton = new JRadioButton(TextProperties.getInstance().getProperty("AnyWiz_Sanitizer_L"));
        shifterButton = new JRadioButton(TextProperties.getInstance().getProperty("AnyWiz_Shifter_L"));

        // Select importer by default
        importerButton.setSelected(true);

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(importerButton);
        buttonGroup.add(calculatorButton);
        buttonGroup.add(clipperButton);
        buttonGroup.add(gridToPointButton);
        buttonGroup.add(imageExporterButton);
        buttonGroup.add(sanitizerButton);
        buttonGroup.add(shifterButton);

        Box buttonBox = Box.createVerticalBox();
        buttonBox.add(importerButton);
        buttonBox.add(calculatorButton);
        buttonBox.add(clipperButton);
        buttonBox.add(gridToPointButton);
        buttonBox.add(imageExporterButton);
        buttonBox.add(sanitizerButton);
        buttonBox.add(shifterButton);

        add(buttonBox, BorderLayout.CENTER);
    }

    private void initializeButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        /* Continue Button */
        JButton nextButton = new JButton(TextProperties.getInstance().getProperty("AnyWiz_Continue_L"));
        nextButton.addActionListener(evt -> continueAction());

        /* Cancel Button */
        JButton cancelButton = new JButton(TextProperties.getInstance().getProperty("AnyWiz_Cancel_L"));
        cancelButton.addActionListener(evt -> closeAction());

        /* Adding Buttons to NavigationPanel */
        buttonPanel.add(nextButton);
        buttonPanel.add(cancelButton);

        /* Add buttonPanel to SanitizerWizard */
        this.add(buttonPanel, BorderLayout.SOUTH);
    }

    private void continueAction() {
        VortexWizard wizard;
        if (calculatorButton.isSelected()) {
            wizard = new CalculatorWizard(frame);
        } else if (clipperButton.isSelected()) {
            wizard = new ClipperWizard(frame);
        } else if (gridToPointButton.isSelected()) {
            wizard = new GridToPointWizard(frame);
        } else if (imageExporterButton.isSelected()) {
            wizard = new ImageExporterWizard(frame);
        } else if (sanitizerButton.isSelected()) {
            wizard = new SanitizerWizard(frame);
        } else if (shifterButton.isSelected()) {
            wizard = new ShifterWizard(frame);
        } else {
            wizard = new ImportMetWizard(frame);
        }

        setVisible(false);

        WindowListener listener = new WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                dispose();
            }

            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                dispose();
            }
        };

        wizard.addWindowListener(listener);
        wizard.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        wizard.buildAndShowUI();
    }

    private void closeAction() {
        setVisible(false);
        dispose();
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) { e.printStackTrace(); }

        AnyWizard anyWizard = new AnyWizard(null);
        anyWizard.buildAndShowUI();
    }
}
