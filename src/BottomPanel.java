import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class BottomPanel extends JPanel {

    private final static JButton nextButton = new JButton("Next");
    private final static JButton backButton = new JButton("Back");
    private final static JButton cancelButton = new JButton("Cancel");

    public BottomPanel() {
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
        cancelButton.addActionListener(e -> System.exit(0));
        add(cancelButton);
    }

    private void addNextButton() {
        nextButton.addActionListener(e -> Frame.nextStep());
        nextButton.setEnabled(false);
        add(nextButton);
    }

    public static void onIsFinished() {
        nextButton.setText("Exit");
        nextButton.removeActionListener(nextButton.getActionListeners()[0]);
        nextButton.addActionListener(e -> System.exit(0));
        cancelButton.setVisible(false);

        backButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                nextButton.setText("Next");
                nextButton.removeActionListener(nextButton.getActionListeners()[0]);
                nextButton.addActionListener(f -> Frame.nextStep());
                cancelButton.setVisible(true);

                backButton.removeActionListener(this);
            }
        });
    }

    public static void detachBackButton() {
        backButton.setVisible(false);
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