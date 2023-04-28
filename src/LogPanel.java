import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedDeque;

public class LogPanel extends JPanel {

    private final JPanel logPanel = new JPanel();
    private final JScrollPane logScrollPane = new JScrollPane(logPanel);

    private int logSize = 100;

    private final LocalDateTime timestampOrigin = LocalDateTime.now();

    private String timestamp = "[0m 0s] ";
    private boolean userHasScrolled = false;

    private final Timer timestampTimer;
    private final Timer updateTimer;

    public LogPanel(int maxSize) {
        this();
        logSize = maxSize;
    }

    public LogPanel() {
        setLayout(new BorderLayout());

        initializeLogPanel();
        initializeLogScrollPane();

        add(logScrollPane, BorderLayout.CENTER);

        timestampTimer = new Timer(1000, e -> updateTimestamp());
        timestampTimer.start();
        updateTimer = new Timer(1, e -> logUpdate());
        updateTimer.start();
    }

    public void shutdown() {
        timestampTimer.stop();
        updateTimer.stop();
        logUpdate();
    }

    private void updateTimestamp() {
        LocalDateTime now = LocalDateTime.now();
        Duration elapsed = Duration.between(timestampOrigin, now);
        long minutes = elapsed.toMinutes();
        long seconds = elapsed.getSeconds() - (minutes * 60);
        timestamp = "[" + minutes + "m " + seconds + "s] ";
    }

    //for handling rapid logging
    ConcurrentLinkedDeque<String> incomingLog = new ConcurrentLinkedDeque<>();

    public void log(String string) {
        incomingLog.add(timestamp + string);
        if (incomingLog.size() > logSize) {
            incomingLog.removeFirst();
        }
    }

    public void log(String string, String fontTag) {
        incomingLog.add("<html>" + timestamp + fontTag + string + "</font></html>");
        if (incomingLog.size() > logSize) {
            incomingLog.removeFirst();
        }
    }

    private void logUpdate() {
        logPanel.removeAll();

        LinkedList<String> showLog = new LinkedList<>(incomingLog);
        while(showLog.size() < 100) {
            showLog.add(" ");
        }
        while(showLog.size() > 100) {
            showLog.removeFirst();
        }

        for(String message : showLog) {
            JLabel label = new JLabel(message);
            logPanel.add(label);
        }

        logPanel.revalidate();
        logPanel.repaint();
    }

    private void initializeLogPanel() {
        logPanel.setBackground(Frame.DARK_COLOR);
        logPanel.setLayout(new BoxLayout(logPanel, BoxLayout.PAGE_AXIS));

        for(int i = 0; i < logSize; i++) {
            incomingLog.add(" ");
        }
    }

    private void initializeLogScrollPane() {
        logScrollPane.setBorder(new LineBorder(Frame.DARK_COLOR));
        logScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        logScrollPane.getVerticalScrollBar().addAdjustmentListener(e -> {
            if(!userHasScrolled) e.getAdjustable().setValue(e.getAdjustable().getMaximum());
        });

        logScrollPane.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                userHasScrolled = true;
                logScrollPane.removeMouseWheelListener(this);
            }
        });
        JScrollBar scrollBar = logScrollPane.getVerticalScrollBar();
        scrollBar.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                userHasScrolled = true;
                logScrollPane.removeMouseListener(this);
            }
        });
    }
}
