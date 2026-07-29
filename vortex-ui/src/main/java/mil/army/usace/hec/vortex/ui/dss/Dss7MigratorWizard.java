package mil.army.usace.hec.vortex.ui.dss;

import hec.heclib.dss.HecDSSFileAccess;
import mil.army.usace.hec.dss.migrator.BatchProgressListener;
import mil.army.usace.hec.dss.migrator.Dss7Migrator;
import mil.army.usace.hec.dss.migrator.DssFiles;
import mil.army.usace.hec.dss.migrator.MigrationResult;
import mil.army.usace.hec.dss.migrator.MigrationSummary;
import mil.army.usace.hec.vortex.ui.FileBrowseButton;
import mil.army.usace.hec.vortex.ui.IconResources;
import mil.army.usace.hec.vortex.ui.ProgressMessagePanel;
import mil.army.usace.hec.vortex.ui.Text;
import mil.army.usace.hec.vortex.ui.VortexWizard;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Migrates DSS files to version 7. Point it at a single {@code .dss} file or at
 * a directory to migrate everything beneath it.
 *
 * <p>Discovery, batching, progress and the tally all come from
 * {@code hec-dss-migrator}, which HEC-HMS drives the same way. What is left here
 * is the window and the wording. HMS additionally migrates on project open,
 * which has no counterpart in Vortex.
 */
public class Dss7MigratorWizard extends VortexWizard {

    private static final Logger logger = Logger.getLogger(Dss7MigratorWizard.class.getName());

    /**
     * Shared: constructing one probes the cache directory for writability, and
     * the underlying isolated runtime is process-cached anyway, so there is
     * nothing gained by building a fresh migrator per run.
     */
    private static final Dss7Migrator MIGRATOR = Dss7Migrator.create();

    private static final int PROGRESS_W = 800;
    private static final int PROGRESS_H = 400;
    private static final int PATH_PANEL_GAP = new FlowLayout().getHgap();

    private final Frame frame;
    private final JTextField pathField = new JTextField();

    public Dss7MigratorWizard(Frame frame) {
        super();
        this.frame = frame;
    }

    @Override
    public void buildAndShowUI() {
        setTitle(Text.format("Dss7Mig_Title"));
        setIconImage(IconResources.loadImage("images/vortex_black.png"));
        setMinimumSize(new Dimension(600, 220));
        setSize(new Dimension(700, 240));
        setLocationRelativeTo(frame);
        setLayout(new BorderLayout());

        add(buildPathPanel(), BorderLayout.NORTH);
        add(buildButtonPanel(), BorderLayout.SOUTH);

        setVisible(true);
    }

    private JPanel buildPathPanel() {
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.add(Box.createRigidArea(new Dimension(0, 4)));
        box.add(buildDescriptionPanel());
        box.add(Box.createRigidArea(new Dimension(0, 4)));
        box.add(buildPathSelectPanel());
        return box;
    }

    private JPanel buildDescriptionPanel() {
        JLabel label = new JLabel(Text.format("Dss7Mig_Description"));
        label.setToolTipText(Text.format("Dss7Mig_Path_TT"));

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        panel.add(label);
        return panel;
    }

    private JPanel buildPathSelectPanel() {
        JLabel label = new JLabel(Text.format("Dss7Mig_Path_L"));
        label.setToolTipText(Text.format("Dss7Mig_Path_TT"));
        label.setLabelFor(pathField);

        pathField.setToolTipText(Text.format("Dss7Mig_Path_TT"));

        FileBrowseButton browseButton = new FileBrowseButton(getClass().getName(), "");
        browseButton.setIcon(IconResources.loadIcon("images/Open16.gif"));
        browseButton.setPreferredSize(new Dimension(22, 22));
        browseButton.addActionListener(evt -> browseAction(browseButton));

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(label);
        panel.add(Box.createHorizontalStrut(PATH_PANEL_GAP));
        panel.add(pathField);
        panel.add(Box.createHorizontalStrut(PATH_PANEL_GAP));
        panel.add(browseButton);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        return panel;
    }

    private JPanel buildButtonPanel() {
        JButton okButton = new JButton(Text.format("Dss7Mig_Migrate_L"));
        okButton.addActionListener(evt -> process());

        JButton cancelButton = new JButton(Text.format("VortexWiz_Cancel"));
        cancelButton.addActionListener(evt -> closeAction());

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        panel.add(okButton);
        panel.add(cancelButton);
        return panel;
    }

    private void browseAction(FileBrowseButton browseButton) {
        JFileChooser chooser = new JFileChooser(browseButton.getPersistedBrowseLocation());
        // Directories are selectable as well as files: the point of the utility
        // is usually to sweep a folder, not to name one file at a time.
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        chooser.setFileFilter(new FileNameExtensionFilter("DSS files (*.dss)", "dss"));
        chooser.setDialogTitle(Text.format("Dss7Mig_Chooser_Title"));

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            pathField.setText(selectedFile.getAbsolutePath());
            browseButton.setPersistedBrowseLocation(selectedFile);
        }
    }

    private void process() {
        Path userPath;
        try {
            userPath = Path.of(pathField.getText().trim());
        } catch (InvalidPathException e) {
            showError(Text.format("Dss7Mig_PathNotFound", pathField.getText().trim()));
            return;
        }

        if (!Files.exists(userPath)) {
            showError(Text.format("Dss7Mig_PathNotFound", userPath));
            return;
        }

        List<Path> dssFiles = DssFiles.discover(userPath);
        if (dssFiles.isEmpty()) {
            showError(Text.format("Dss7Mig_NoFilesFound", userPath));
            return;
        }

        if (!showConfirmationDialog(dssFiles)) {
            return;
        }

        // The batch upgrades v6 files and repairs v6-format grid records inside
        // v7 files; clean v7 files are no-ops. So everything discovered is
        // offered, not just the v6 ones.
        showProgressAndMigrate(dssFiles);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message,
                Text.format("Dss7Mig_Title"), JOptionPane.ERROR_MESSAGE);
    }

    private boolean showConfirmationDialog(List<Path> files) {
        // DssFiles.discover already returns them sorted.
        List<String> sortedFiles = files.stream().map(Path::toString).toList();

        JPanel content = new JPanel(new BorderLayout(0, 10));

        JLabel messageLabel = new JLabel(Text.format("Dss7Mig_Confirm_Message"));
        messageLabel.setIcon(UIManager.getIcon("OptionPane.warningIcon"));
        messageLabel.setIconTextGap(12);
        content.add(messageLabel, BorderLayout.NORTH);

        JPanel filePanel = new JPanel(new BorderLayout(0, 4));
        filePanel.add(new JLabel(Text.format("Dss7Mig_Confirm_Files_L")), BorderLayout.NORTH);

        JTextArea area = new JTextArea(String.join("\n", sortedFiles));
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setLineWrap(false);
        area.setRows(Math.min(sortedFiles.size(), 8));

        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(500, Math.min(sortedFiles.size() * 20 + 10, 160)));
        filePanel.add(scroll, BorderLayout.CENTER);

        content.add(filePanel, BorderLayout.CENTER);

        String migrateText = Text.format("Dss7Mig_Migrate_L");
        String cancelText = Text.format("VortexWiz_Cancel");

        JOptionPane optionPane = new JOptionPane(
                content,
                JOptionPane.PLAIN_MESSAGE,
                JOptionPane.DEFAULT_OPTION,
                null,
                new Object[]{migrateText, cancelText},
                migrateText
        );

        JDialog dialog = optionPane.createDialog(this, Text.format("Dss7Mig_Confirm_Title"));
        dialog.setResizable(true);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);

        return migrateText.equals(optionPane.getValue());
    }

    private void showProgressAndMigrate(List<Path> files) {
        ProgressDialog progressDialog = new ProgressDialog(this);

        SwingWorker<MigrationSummary, Void> worker = new SwingWorker<>() {
            @Override
            protected MigrationSummary doInBackground() {
                // The host's own DSS handles, not the migrator's. It runs an
                // isolated heclib and cannot close ours; a file another wizard
                // left open in this JVM would otherwise be migrated underneath
                // it.
                HecDSSFileAccess.closeAllFiles();
                return MigrationSummary.of(
                        MIGRATOR.migrateBatch(files, progressReporter(progressDialog)));
            }

            @Override
            protected void done() {
                try {
                    MigrationSummary summary = get();
                    progressDialog.write(Text.format("Dss7Mig_Complete",
                            summary.migrated(), summary.alreadyUpToDate(), summary.failed()));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    logger.log(Level.SEVERE, e, e::getMessage);
                    progressDialog.write(Text.format("Dss7Mig_BatchError", String.valueOf(e.getCause())));
                }
                progressDialog.setCloseEnabled(true);
            }
        };

        worker.execute();
        progressDialog.setVisible(true); // modal -- blocks until the user closes it
    }

    /**
     * Renders the library's batch events as progress-panel lines. The migrator
     * reports facts -- index, total, status, its own message -- and the wording
     * is chosen here, so the shared code carries no Vortex text.
     */
    private static BatchProgressListener progressReporter(ProgressDialog progressDialog) {
        return new BatchProgressListener() {
            @Override
            public void onFileStart(int index, int total, Path file) {
                progressDialog.write(Text.format("Dss7Mig_Status", file.getFileName(), index, total));
            }

            @Override
            public void onFileComplete(int index, int total, Path file, MigrationResult result) {
                switch (result.status()) {
                    case MIGRATED -> progressDialog.write(
                            Text.format("Dss7Mig_Migrated", file, result.message()));
                    case ALREADY_UP_TO_DATE -> progressDialog.write(
                            Text.format("Dss7Mig_Skipped", file));
                    case FAILED -> progressDialog.write(
                            Text.format("Dss7Mig_Failed", file, result.message()));
                }
                progressDialog.setValue(index * 100 / total);
            }
        };
    }

    private void closeAction() {
        setVisible(false);
        dispose();
    }

    /**
     * Modal progress window. Vortex has {@link ProgressMessagePanel} where HMS
     * has a whole dialog class, so this wraps the panel to match: the close
     * button stays disabled until the batch finishes, so the window cannot be
     * dismissed while native DSS writes are still in flight.
     */
    private static final class ProgressDialog extends JDialog {
        private final ProgressMessagePanel messagePanel = new ProgressMessagePanel();
        private final JButton closeButton = new JButton(Text.format("VortexWiz_Close"));

        ProgressDialog(Window owner) {
            super(owner, Text.format("Dss7Mig_Progress_Title"), ModalityType.APPLICATION_MODAL);

            closeButton.setEnabled(false);
            closeButton.addActionListener(e -> dispose());

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
            buttonPanel.add(closeButton);

            setLayout(new BorderLayout());
            add(messagePanel, BorderLayout.CENTER);
            add(buttonPanel, BorderLayout.SOUTH);

            setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            setSize(PROGRESS_W, PROGRESS_H);
            setLocationRelativeTo(owner);
        }

        void setValue(int progress) {
            messagePanel.setValue(progress);
        }

        void write(String message) {
            messagePanel.write(message);
        }

        void setCloseEnabled(boolean enabled) {
            SwingUtilities.invokeLater(() -> {
                closeButton.setEnabled(enabled);
                setDefaultCloseOperation(enabled
                        ? WindowConstants.DISPOSE_ON_CLOSE
                        : WindowConstants.DO_NOTHING_ON_CLOSE);
            });
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                 | UnsupportedLookAndFeelException e) {
            logger.log(Level.SEVERE, e, e::getMessage);
        }

        new Dss7MigratorWizard(null).buildAndShowUI();
    }
}
