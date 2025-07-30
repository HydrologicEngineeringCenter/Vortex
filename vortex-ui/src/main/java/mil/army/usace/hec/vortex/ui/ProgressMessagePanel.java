package mil.army.usace.hec.vortex.ui;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A panel that displays a progress bar and text messages with different styling based on message type.
 * Automatically adapts to light/dark mode themes for better visibility.
 */
public class ProgressMessagePanel extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(ProgressMessagePanel.class.getName());

    private static final Color LOW_LUMINANCE_RED = new Color(220, 30, 60);
    private static final Color LOW_LUMINANCE_ORANGE = new Color(255, 127, 14);

    private static final Color HIGH_LUMINANCE_RED = new Color(214, 39, 40);
    private static final Color HIGH_LUMINANCE_ORANGE = new Color(255, 127, 14);

    // Message type identifiers
    private static final String ERROR_PREFIX = "ERROR";
    private static final String WARNING_PREFIX = "WARNING";

    // UI Components
    private final JProgressBar progressBar;
    private final JTextPane textPane;

    private final StringBuilder byteBuffer = new StringBuilder();

    // Text styling
    private final transient Document document;
    private SimpleAttributeSet infoAttributeSet;
    private SimpleAttributeSet warnAttributeSet;
    private SimpleAttributeSet errorAttributeSet;

    /**
     * Constructs a new ProgressMessagePanel with default settings.
     */
    public ProgressMessagePanel() {
        setLayout(new BorderLayout());

        // Create progress bar panel
        progressBar = new JProgressBar(0, 100);
        progressBar.setIndeterminate(true);
        progressBar.setStringPainted(false);

        JPanel progressPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        progressPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        progressPanel.add(progressBar);

        // Create text display area
        textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setFocusable(false);

        DefaultCaret caret = (DefaultCaret) textPane.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        JScrollPane scrollPane = new JScrollPane(textPane);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BorderLayout());
        textPanel.add(scrollPane, BorderLayout.CENTER);
        textPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Add components to main panel
        add(progressPanel, BorderLayout.NORTH);
        add(textPanel, BorderLayout.CENTER);

        // Initialize text styling
        document = textPane.getDocument();
        initWriteAttributes();
    }

    /**
     * Sets the current progress value.
     *
     * @param progress The progress value (0-100)
     */
    public void setValue(int progress) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setStringPainted(true);
            progressBar.setIndeterminate(false);
            progressBar.setValue(Math.max(0, Math.min(100, progress)));
        });
    }

    /**
     * Writes a message to the panel with appropriate styling based on the message prefix.
     * Messages starting with "ERROR" will be shown in red.
     * Messages starting with "WARNING" will be shown in orange/amber.
     * Other messages will be shown in the default text color.
     *
     * @param message The message to display
     */
    public void write(String message) {
        if (message == null) return;

        SwingUtilities.invokeLater(() -> {
            try {
                AttributeSet attributeSet;

                if (message.startsWith(ERROR_PREFIX)) {
                    attributeSet = errorAttributeSet;
                } else if (message.startsWith(WARNING_PREFIX)) {
                    attributeSet = warnAttributeSet;
                } else {
                    attributeSet = infoAttributeSet;
                }

                document.insertString(document.getLength(),
                        message + System.lineSeparator(),
                        attributeSet);

                // Auto-scroll to show newest message
                textPane.setCaretPosition(document.getLength());
            } catch (BadLocationException e) {
                LOGGER.log(Level.WARNING, "Error writing message to panel", e);
            }
        });
    }

    /**
     * Writes a single byte to the panel. This method is compatible with the OutputStream
     * write(int b) method, allowing the panel to be used with stream-based output.
     * Bytes are accumulated until a line separator is encountered, then the complete
     * line is processed with appropriate styling.
     *
     * @param b The byte to write (the low-order byte of the provided integer)
     */
    public void write(int b) {
        // Convert the int to a char (just the low-order byte)
        char c = (char) (b & 0xFF);

        if (c == '\n') {
            // Process the complete line when a newline is encountered
            String message = byteBuffer.toString();
            if (message.endsWith("\r")) {
                // Handle CR+LF line endings by removing the CR
                message = message.substring(0, message.length() - 1);
            }

            // Write the accumulated message
            write(message);

            // Clear the buffer for the next line
            byteBuffer.setLength(0);
        } else if (c != '\r') {
            // Accumulate non-CR characters
            byteBuffer.append(c);
        }
        // CR characters are handled with the following LF
    }

    /**
     * Flushes any remaining content in the byte buffer.
     * This should be called when the stream is closed to ensure all content is displayed.
     */
    public void flush() {
        if (!byteBuffer.isEmpty()) {
            write(byteBuffer.toString());
            byteBuffer.setLength(0);
        }
    }

    /**
     * Clears all messages and resets the progress bar.
     */
    public void clear() {
        setValue(0);

        SwingUtilities.invokeLater(() -> {
            try {
                document.remove(0, document.getLength());
            } catch (BadLocationException e) {
                LOGGER.log(Level.SEVERE, "Error clearing message panel", e);
            }
        });
    }

    /**
     * Initializes the text attribute sets with appropriate colors based on UI theme.
     */
    private void initWriteAttributes() {
        Font f = UIManager.getFont("TextField.font");
        String fontFamily = f != null ? f.getFamily() : Font.SANS_SERIF;
        int fontSize = f != null ? f.getSize() : 12;

        // Get the foreground color from the UI
        Color defaultTextColor = UIManager.getColor("TextField.foreground");
        if (defaultTextColor == null) {
            defaultTextColor = Color.BLACK;
        }

        // Determine if we're in dark mode
        boolean isDarkMode = isInDarkMode();

        // Create error and warning colors based on the theme
        Color errorColor = isDarkMode ? LOW_LUMINANCE_RED : HIGH_LUMINANCE_RED;
        Color warningColor = isDarkMode ? LOW_LUMINANCE_ORANGE : HIGH_LUMINANCE_ORANGE;

        // Create attribute sets
        infoAttributeSet = new SimpleAttributeSet();
        StyleConstants.setFontFamily(infoAttributeSet, fontFamily);
        StyleConstants.setFontSize(infoAttributeSet, fontSize);
        StyleConstants.setForeground(infoAttributeSet, defaultTextColor);

        warnAttributeSet = new SimpleAttributeSet();
        StyleConstants.setFontFamily(warnAttributeSet, fontFamily);
        StyleConstants.setFontSize(warnAttributeSet, fontSize);
        StyleConstants.setForeground(warnAttributeSet, warningColor);

        errorAttributeSet = new SimpleAttributeSet();
        StyleConstants.setFontFamily(errorAttributeSet, fontFamily);
        StyleConstants.setFontSize(errorAttributeSet, fontSize);
        StyleConstants.setForeground(errorAttributeSet, errorColor);
    }

    /**
     * Determines if the current UI theme is dark mode by analyzing background luminance.
     *
     * @return true if the UI appears to be in dark mode
     */
    private boolean isInDarkMode() {
        Color background = UIManager.getColor("Panel.background");
        if (background == null) {
            background = this.getBackground();
        }

        // Calculate relative luminance (perceived brightness)
        // Formula: 0.299R + 0.587G + 0.114B (standard luminance perception weights)
        double luminance = (0.299 * background.getRed() +
                0.587 * background.getGreen() +
                0.114 * background.getBlue()) / 255;

        // If luminance is less than 0.5, consider it dark mode
        return luminance < 0.5;
    }
}