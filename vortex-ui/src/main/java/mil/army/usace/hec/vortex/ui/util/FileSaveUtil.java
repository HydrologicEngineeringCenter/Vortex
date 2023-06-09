package mil.army.usace.hec.vortex.ui.util;

import mil.army.usace.hec.vortex.ui.IconResources;
import mil.army.usace.hec.vortex.ui.TextProperties;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Logger;

public class FileSaveUtil {
    private static final Logger logger = Logger.getLogger(FileSaveUtil.class.getName());
    public static final String FILE_NOT_FOUND = TextProperties.getInstance().getProperty("FileSavedDialogWarning_NoFile");

    /* File Saved Dialog UI */
    private static JPanel fileSavedPanel(Path filePath) {
        JPanel contentPanel = new JPanel();
        contentPanel.setSize(new Dimension(400, 64));

        JLabel messageLabel = new JLabel(TextProperties.getInstance().getProperty("FileSavedDialogMessage"));
        contentPanel.add(messageLabel);
        contentPanel.add(outputLinkPanel(filePath));
        return contentPanel;
    }

    private static JPanel outputLinkPanel(Path filePath) {
        JPanel linkPanel = new JPanel(new FlowLayout());
        JTextField outputLink = outputLinkField(filePath);
        JButton copyButton = copyButton(outputLink);
        JButton openButton = openButton(outputLink);
        linkPanel.add(outputLink);
        linkPanel.add(copyButton);
        linkPanel.add(openButton);
        return linkPanel;
    }

    private static JTextField outputLinkField(Path filePath) {
        JTextField outputLink = new JTextField(filePath.toAbsolutePath().toString());
        outputLink.setEditable(false);
        outputLink.setBounds(0, 0, 400, 64);
        outputLink.setColumns(30);
        outputLink.setHorizontalAlignment(SwingConstants.LEFT);
        outputLink.addMouseListener(copyMouseAdapter(outputLink));
        return outputLink;
    }

    private static JButton copyButton(JTextField textField) {
        JButton copyButton = new JButton(IconResources.loadIcon("images/Copy16.gif"));
        copyButton.setToolTipText(TextProperties.getInstance().getProperty("FileSavedDialogCopyButton_TT"));
        SwingUtil.setButtonSize(copyButton, textField);
        copyButton.addMouseListener(copyMouseAdapter(textField));
        return copyButton;
    }

    private static JButton openButton(JTextField textField) {
        JButton openButton = new JButton(IconResources.loadIcon("images/Open16.gif"));
        openButton.setToolTipText(TextProperties.getInstance().getProperty("FileSavedDialogOpenButton_TT"));
        SwingUtil.setButtonSize(openButton, textField);
        openButton.addActionListener(evt -> {
            Path savedFile = Path.of(textField.getText());
            OsUtil.browseFileDirectory(savedFile);
        });
        return openButton;
    }

    private static MouseAdapter copyMouseAdapter(JTextField textField) {
        return new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                StringSelection stringSelection = new StringSelection(textField.getText());
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(stringSelection, null);

                JToolTip toolTip = textField.createToolTip();
                toolTip.setTipText(TextProperties.getInstance().getProperty("FileSavedDialogCopied"));
                Popup copiedPopup = PopupFactory.getSharedInstance().getPopup(textField, toolTip,
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

    private static Image getWindowIcon(Window window) {
        return Optional.ofNullable(window)
                .map(Window::getIconImages)
                .filter(list -> !list.isEmpty())
                .map(list -> list.get(0))
                .orElse(null);
    }

    private static boolean isFileExists(Path filePath) {
        return Files.isRegularFile(filePath) && Files.exists(filePath);
    }

    /* Utility Methods */
    public static boolean promptFileOverride(Window window, Path filePath) {
        if(!isFileExists(filePath)) {
            logger.warning(FILE_NOT_FOUND);
            return true;
        }

        String title = TextProperties.getInstance().getProperty("FileOverrideDialogTitle");
        String message = filePath + " " + TextProperties.getInstance().getProperty("FileOverrideDialogMessage");
        JOptionPane optionPane = new JOptionPane(message, JOptionPane.WARNING_MESSAGE, JOptionPane.YES_NO_OPTION);

        JDialog dialog = optionPane.createDialog(window, title);
        dialog.setIconImage(getWindowIcon(window));
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setVisible(true);
        Object choice = optionPane.getValue();
        dialog.dispatchEvent(new WindowEvent(dialog, WindowEvent.WINDOW_CLOSING));
        return (int) choice == JOptionPane.OK_OPTION;
    }

    public static void showFileLocation(Window window, Path filePath) {
        if(!isFileExists(filePath)) {
            logger.warning(FILE_NOT_FOUND);
            return;
        }

        String title = TextProperties.getInstance().getProperty("FileSavedDialogTitle");
        JOptionPane optionPane = new JOptionPane(fileSavedPanel(filePath));

        JDialog dialog = optionPane.createDialog(window, title);
        dialog.setIconImage(getWindowIcon(window));
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setVisible(true);
        dialog.dispatchEvent(new WindowEvent(dialog, WindowEvent.WINDOW_CLOSING));
    }

    public static void showDirectoryLocation(Window window, Path directoryPath) {
        if (Files.notExists(directoryPath)) {
            logger.warning(FILE_NOT_FOUND);
            return;
        }

        String title = TextProperties.getInstance().getProperty("FileSavedDialogTitle");
        JOptionPane optionPane = new JOptionPane(fileSavedPanel(directoryPath));

        JDialog dialog = optionPane.createDialog(window, title);
        dialog.setIconImage(getWindowIcon(window));
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setVisible(true);
        dialog.dispatchEvent(new WindowEvent(dialog, WindowEvent.WINDOW_CLOSING));
    }

    /* UI Testing */
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) { e.printStackTrace(); }

        JFrame frame = new JFrame();
        frame.setIconImage(IconResources.loadImage("images/vortex_black.png"));
        Path filePath = Path.of(".");

        FileSaveUtil.showFileLocation(frame, filePath);

        boolean override = FileSaveUtil.promptFileOverride(frame, filePath);
        String logMessage = String.valueOf(override);
        logger.info(logMessage);
    }
}
