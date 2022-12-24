import javax.swing.*;
import javax.swing.border.LineBorder;

public abstract class StepPanel extends JPanel {

    public StepPanel() {
        setBorder(new LineBorder(Frame.DARK_COLOR, 2));
    }

    public abstract void onNextStep();
    public abstract void onBackStep();
}