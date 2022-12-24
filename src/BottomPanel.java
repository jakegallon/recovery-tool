import javax.swing.*;
import javax.swing.border.MatteBorder;
import java.awt.*;

public class BottomPanel extends JPanel {

    private final static JButton nextButton = new JButton("Next");
    private final static JButton backButton = new JButton("Back");

    public BottomPanel() {
        setBorder(new MatteBorder(0, 2, 2, 2, Frame.DARK_COLOR));

        SpringLayout springLayout = new SpringLayout();
        setLayout(springLayout);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> System.exit(0));
        add(cancelButton);

        int gap = 8;
        int buttonWidth = 120;

        springLayout.putConstraint(SpringLayout.WEST, cancelButton, gap, SpringLayout.WEST, this);
        springLayout.putConstraint(SpringLayout.EAST, cancelButton, buttonWidth, SpringLayout.WEST, cancelButton);
        springLayout.putConstraint(SpringLayout.NORTH, cancelButton, gap, SpringLayout.NORTH, this);
        springLayout.putConstraint(SpringLayout.SOUTH, cancelButton, -gap, SpringLayout.SOUTH, this);

        nextButton.addActionListener(e -> Frame.nextStep());
        nextButton.setEnabled(false);
        add(nextButton);

        springLayout.putConstraint(SpringLayout.EAST, nextButton, -gap, SpringLayout.EAST, this);
        springLayout.putConstraint(SpringLayout.WEST, nextButton, -buttonWidth, SpringLayout.EAST, nextButton);
        springLayout.putConstraint(SpringLayout.NORTH, nextButton, gap, SpringLayout.NORTH, this);
        springLayout.putConstraint(SpringLayout.SOUTH, nextButton, -gap, SpringLayout.SOUTH, this);

        backButton.addActionListener(e -> Frame.backStep());
        backButton.setEnabled(false);
        add(backButton);

        springLayout.putConstraint(SpringLayout.EAST, backButton, -gap, SpringLayout.WEST, nextButton);
        springLayout.putConstraint(SpringLayout.WEST, backButton, -buttonWidth, SpringLayout.EAST, backButton);
        springLayout.putConstraint(SpringLayout.NORTH, backButton, gap, SpringLayout.NORTH, this);
        springLayout.putConstraint(SpringLayout.SOUTH, backButton, -gap, SpringLayout.SOUTH, this);

        setPreferredSize(new Dimension(100, 40));
    }

    public static void setNextButtonEnabled(boolean b) {
        nextButton.setEnabled(b);
    }

    public static void setBackButtonEnabled(boolean b) {
        backButton.setEnabled(b);
    }
}