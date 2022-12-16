import javax.swing.*;
import java.awt.*;

public class Frame extends JFrame {
    public Frame() {
        init();
        setVisible(true);
    }

    private void init() {
        setTitle("Recovery Tool");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1366, 768);
        setMinimumSize(new Dimension(1366, 768));
    }
}
