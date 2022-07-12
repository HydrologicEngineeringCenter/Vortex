package mil.army.usace.hec.vortex.ui;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

public class InputHintTextField extends JTextField implements FocusListener {
    String hintText;

    public InputHintTextField(String hintText) {
        this.hintText = hintText;
        this.addFocusListener(this);
        setHintText();
    }

    private void setHintText() {
        Color hintTextColor = Color.GRAY;
        this.setForeground(hintTextColor);
        this.setText(hintText);
    }

    public void resetTextField() {
        setHintText();
    }

    @Override
    public void focusGained(FocusEvent e) {
        if(this.getText().equals(hintText)) {
            this.setForeground(new JTextField().getForeground());
            this.setText("");
        }
    }

    @Override
    public void focusLost(FocusEvent e) {
        if(this.getText().isEmpty()) {
            setHintText();
        }
    }
}
