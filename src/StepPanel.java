import javax.swing.*;
import javax.swing.border.EmptyBorder;

public abstract class StepPanel extends JPanel {

    public StepPanel() {
        EmptyBorder emptyBorder = new EmptyBorder(10, 10, 10, 10);
        setBorder(emptyBorder);
    }

    public abstract void onNextStep();
    public abstract void onBackStep();
}