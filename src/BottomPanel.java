import javax.swing.*;
import javax.swing.border.MatteBorder;
import java.awt.*;

public class BottomPanel extends JPanel {

    private final static JButton nextButton = new JButton("Next");
    private final static JButton backButton = new JButton("Back");

    public BottomPanel() {
        setBorder(new MatteBorder(0, 2, 2, 2, Frame.DARK_COLOR));
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setPreferredSize(new Dimension(100, 40));

        add(Box.createHorizontalStrut(8));
        addCancelButton();

        add(Box.createHorizontalGlue());

        addBackButton();
        add(Box.createHorizontalStrut(8));
        addNextButton();
        add(Box.createHorizontalStrut(8));
    }

    private void addCancelButton() {
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> System.exit(0));
        add(cancelButton);
    }

    private void addNextButton() {
        nextButton.addActionListener(e -> Frame.nextStep());
        nextButton.setEnabled(false);
        add(nextButton);
    }

    private void addBackButton() {
        backButton.addActionListener(e -> Frame.backStep());
        backButton.setEnabled(false);
        add(backButton);
    }

    public static void setNextButtonEnabled(boolean b) {
        nextButton.setEnabled(b);
    }

    public static void setBackButtonEnabled(boolean b) {
        backButton.setEnabled(b);
    }
}