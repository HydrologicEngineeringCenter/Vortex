package mil.army.usace.hec.vortex.ui;

import mil.army.usace.hec.vortex.ui.util.OsUtil;
import mil.army.usace.hec.vortex.ui.util.SwingUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

public class FileSavedDialog {
    private static final Logger logger = Logger.getLogger(FileSavedDialog.class.getName());

    private final JFrame mainFrame;
    private JDialog dialog;
    private JTextField outputLink;
    private JButton copyButton;
    private JButton openButton;

    /* Constructor */
    public FileSavedDialog(JFrame mainFrame, String outputPath) {
        this.mainFrame = mainFrame;
        initDialog(outputPath);
    }

    /* Init Methods */
    private void initDialog(String outputPath) {
        /* Content Panel */
        JPanel contentPanel = new JPanel();
        contentPanel.setSize(new Dimension(400, 64));

        JLabel messageLabel = new JLabel(TextProperties.getInstance().getProperty("FileSavedDialogMessage"));

        JPanel linkPanel = new JPanel(new FlowLayout());
        initOutputLink(outputPath);
        initCopyButton();
        initOpenButton();
        linkPanel.add(outputLink);
        linkPanel.add(copyButton);
        linkPanel.add(openButton);

        contentPanel.add(messageLabel);
        contentPanel.add(linkPanel);

        /* Dialog */
        JOptionPane optionPane = new JOptionPane(contentPanel);
        optionPane.setIcon(null);
        dialog = optionPane.createDialog(TextProperties.getInstance().getProperty("FileSavedDialogTitle"));
        dialog.setLocationRelativeTo(mainFrame);
        dialog.setIconImage(mainFrame != null ? mainFrame.getIconImage() : null);
    }

    private void initOutputLink(String pathToOutputHtml) {
        outputLink = new JTextField(pathToOutputHtml);
        outputLink.setBackground(Color.WHITE);
        outputLink.setEditable(false);
        outputLink.setBounds(0, 0, 400, 64);
        outputLink.setColumns(30);
        outputLink.setHorizontalAlignment(SwingConstants.LEFT);
        outputLink.addMouseListener(copyMouseAdapter());
    }

    private void initCopyButton() {
        copyButton = new JButton(IconResources.loadIcon("images/Copy16.gif"));
        copyButton.setToolTipText(TextProperties.getInstance().getProperty("FileSavedDialogCopyButton_TT"));
        SwingUtil.setButtonSize(copyButton, outputLink);
        copyButton.addMouseListener(copyMouseAdapter());
    }

    private void initOpenButton() {
        openButton = new JButton(IconResources.loadIcon("images/Open16.gif"));
        openButton.setToolTipText(TextProperties.getInstance().getProperty("FileSavedDialogOpenButton_TT"));
        SwingUtil.setButtonSize(openButton, outputLink);
        openButton.addActionListener(evt -> {
            Path savedFile = Path.of(outputLink.getText());
            OsUtil.browseFileDirectory(savedFile);
        });
    }

    /* Helper Methods */
    private MouseAdapter copyMouseAdapter() {
        return new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                StringSelection stringSelection = new StringSelection(outputLink.getText());
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(stringSelection, null);

                JToolTip toolTip = outputLink.createToolTip();
                toolTip.setTipText(TextProperties.getInstance().getProperty("FileSavedDialogCopied"));
                Popup copiedPopup = PopupFactory.getSharedInstance().getPopup(outputLink, toolTip,
                        e.getX() + e.getComponent().getLocationOnScreen().x + 10,
                        e.getY() + e.getComponent().getLocationOnScreen().y + 10);
                copiedPopup.show();

                Timer timer = new Timer(500, null);
                timer.start();
                timer.addActionListener(evt -> {
                    copiedPopup.hide();
                    timer.stop();
                }); // Timer ActionListener to Hide Popup
            } // outputLink mouseClicked()
        };
    }

    /* Public Methods */
    public void setVisible(boolean isVisible) {
        String outputStr = outputLink.getText();
        Path pathToFile = Path.of(outputStr);
        if(outputStr.isEmpty() || Files.notExists(pathToFile)) {
            logger.warning(TextProperties.getInstance().getProperty("FileSavedDialogWarning_NoFile"));
            dialog.dispose();
            return;
        }
        dialog.setVisible(isVisible);
    }

    /* UI Testing */
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) { e.printStackTrace(); }
        FileSavedDialog fileSavedDialog = new FileSavedDialog(null, "");
        fileSavedDialog.setVisible(true);
    }
}