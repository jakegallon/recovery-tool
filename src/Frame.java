import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class Frame extends JFrame {

    public static final Color DARK_COLOR = new Color(43, 43, 43);
    public static final Color HOVER_COLOR = new Color(85, 88, 90);
    public static final Color SELECT_COLOR = new Color(70, 106, 146);

    private static final JPanel stepPanelContainer = new JPanel();
    private static StepPanel activeStepPanel;

    private static Frame frame;

    public Frame() {
        init();
        frame = this;

        setStepPanel(new PartitionPanel());
    }

    public static void setStepPanel(StepPanel stepPanel) {
        stepPanelContainer.removeAll();

        activeStepPanel = stepPanel;
        stepPanelContainer.add(stepPanel, BorderLayout.CENTER);

        stepPanelContainer.revalidate();
    }

    public static void nextStep() {
        activeStepPanel.onNextStep();
    }

    public static void backStep() {
        activeStepPanel.onBackStep();
    }

    public static void updateTitle(String string) {
        frame.setTitle(string);
    }

    private void init() {
        addMenuBar();

        setResizable(false);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        setLayout(new BorderLayout());

        setSize(683, 768);
        setMinimumSize(new Dimension(683, 768));

        stepPanelContainer.setLayout(new BorderLayout());
        add(stepPanelContainer, BorderLayout.CENTER);

        BottomPanel bottomPanel = new BottomPanel();
        add(bottomPanel, BorderLayout.PAGE_END);

        getRootPane().setBorder(BorderFactory.createMatteBorder(3, 3, 3, 3, DARK_COLOR));

        setVisible(true);
    }

    private void addMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu helpMenu = new JMenu();
        helpMenu.setText("?");
        helpMenu.setFont(new Font("Arial", Font.PLAIN, 16));

        JMenuItem helpMenuGithub = new JMenuItem();
        helpMenuGithub.setText("View on GitHub");
        helpMenuGithub.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(new URI("https://github.com/jakegallon/recovery-tool"));
            } catch (IOException | URISyntaxException ex) {
                throw new RuntimeException(ex);
            }
        });

        helpMenu.add(helpMenuGithub);

        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }
}
