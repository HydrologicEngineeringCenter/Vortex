package mil.army.usace.hec.vortex.ui;

import mil.army.usace.hec.vortex.VortexProperty;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.List;

abstract class ProcessingWizard extends VortexWizard {

    private static final String BACK = Text.format("VortexWiz_Back");
    private static final String BACK_TT = Text.format("VortexWiz_Back_TT");
    private static final String NEXT = Text.format("VortexWiz_Next");
    private static final String NEXT_TT = Text.format("VortexWiz_Next_TT");
    private static final String CANCEL = Text.format("VortexWiz_Cancel");
    private static final String CANCEL_TT = Text.format("VortexWiz_Cancel_TT");
    private static final String RESTART = Text.format("VortexWiz_Restart");
    private static final String RESTART_TT = Text.format("VortexWiz_Restart_TT");
    private static final String CLOSE = Text.format("VortexWiz_Close");
    private static final String CLOSE_TT = Text.format("VortexWiz_Close_TT");

    protected final Frame frame;
    protected final ProgressMessagePanel progressMessagePanel = new ProgressMessagePanel();

    private Container contentCards;
    private CardLayout cardLayout;
    private JButton backButton;
    private JButton nextButton;
    private JButton cancelButton;
    private int cardNumber;

    protected ProcessingWizard(Frame frame) {
        super();
        this.frame = frame;
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeAction();
            }
        });
    }

    // --- Abstract methods subclasses must implement ---

    protected abstract String getTitlePropertyKey();

    protected abstract List<JPanel> createStepPanels();

    protected abstract int getLastInteractiveStep();

    protected abstract boolean validateStep(int stepIndex);

    protected abstract void submitStep(int stepIndex);

    protected abstract void clearWizardState();

    protected abstract void showSaveResult();

    // --- Hook methods subclasses may override ---

    protected void onBackAction(int newCardNumber) {
        // Default: no-op. Override for wizard-specific back behavior.
    }

    // --- Template implementation ---

    @Override
    public void buildAndShowUI() {
        setTitle(Text.format(getTitlePropertyKey()));
        setIconImage(IconResources.loadImage("images/vortex_black.png"));
        setMinimumSize(new Dimension(600, 400));
        setLocation(getPersistedLocation());
        if (frame != null) setLocationRelativeTo(frame);
        setSize(getPersistedSize());
        setLayout(new BorderLayout());

        initializeContentCards();
        initializeButtonPanel();

        add(contentCards, BorderLayout.CENTER);
        setVisible(true);
    }

    private void initializeContentCards() {
        contentCards = new Container();
        cardLayout = new CardLayout();
        contentCards.setLayout(cardLayout);
        cardNumber = 0;

        List<JPanel> panels = createStepPanels();
        for (int i = 0; i < panels.size(); i++) {
            contentCards.add("Step " + (i + 1), panels.get(i));
        }
    }

    private void initializeButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        backButton = new JButton(BACK);
        backButton.setToolTipText(BACK_TT);
        backButton.setEnabled(false);
        backButton.addActionListener(evt -> backAction());

        nextButton = new JButton(NEXT);
        nextButton.setToolTipText(NEXT_TT);
        nextButton.addActionListener(evt -> {
            if (RESTART.equals(nextButton.getText())) {
                restartAction();
            } else if (NEXT.equals(nextButton.getText())) {
                nextAction();
            }
        });

        cancelButton = new JButton(CANCEL);
        cancelButton.setToolTipText(CANCEL_TT);
        cancelButton.addActionListener(evt -> closeAction());

        buttonPanel.add(backButton);
        buttonPanel.add(nextButton);
        buttonPanel.add(cancelButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    protected void nextAction() {
        if (!validateStep(cardNumber)) return;
        submitStep(cardNumber);
        cardNumber++;
        updateButtonState();
        cardLayout.next(contentCards);
    }

    private void updateButtonState() {
        int lastInteractive = getLastInteractiveStep();
        backButton.setEnabled(cardNumber > 0 && cardNumber <= lastInteractive);
        nextButton.setEnabled(cardNumber <= lastInteractive);
        cancelButton.setEnabled(cardNumber <= lastInteractive);
    }

    protected void setButtonsForRestartOrClose() {
        backButton.setVisible(false);
        nextButton.setText(RESTART);
        nextButton.setToolTipText(RESTART_TT);
        nextButton.setEnabled(true);
        cancelButton.setText(CLOSE);
        cancelButton.setToolTipText(CLOSE_TT);
        cancelButton.setEnabled(true);
    }

    private void backAction() {
        cardNumber--;
        if (cardNumber == 0) {
            backButton.setEnabled(false);
        }
        onBackAction(cardNumber);
        cardLayout.previous(contentCards);
    }

    private void restartAction() {
        cardNumber = 0;
        cardLayout.first(contentCards);

        backButton.setVisible(true);
        backButton.setEnabled(false);

        nextButton.setEnabled(true);
        nextButton.setText(NEXT);
        nextButton.setToolTipText(NEXT_TT);

        cancelButton.setText(CANCEL);
        cancelButton.setToolTipText(CANCEL_TT);

        clearWizardState();
        progressMessagePanel.clear();
    }

    private void closeAction() {
        setVisible(false);
        dispose();
        showSaveResult();
    }

    // --- Utility methods for subclasses ---

    protected JPanel createProgressPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(progressMessagePanel, BorderLayout.CENTER);
        return panel;
    }

    protected PropertyChangeListener createProgressListener() {
        return evt -> {
            VortexProperty property = VortexProperty.parse(evt.getPropertyName());
            if (VortexProperty.PROGRESS == property) {
                if (!(evt.getNewValue() instanceof Integer)) return;
                progressMessagePanel.setValue((int) evt.getNewValue());
            } else {
                progressMessagePanel.write(String.valueOf(evt.getNewValue()));
            }
        };
    }

    protected List<String> getItemsInList(JList<String> list) {
        DefaultListModel<String> model = Util.getDefaultListModel(list);
        if (model == null) return null;
        return Collections.list(model.elements());
    }
}
