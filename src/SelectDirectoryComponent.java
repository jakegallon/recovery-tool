import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;

public class SelectDirectoryComponent extends JPanel {

    private final JLabel fileLabel = new JLabel("");
    private final JButton browseButton = new JButton("Browse");

    private final MouseAdapter mouseAdapter = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            super.mouseClicked(e);
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            try {
                fileChooser.setCurrentDirectory(new File(new File("C:\\").getCanonicalPath()));
                fileChooser.changeToParentDirectory();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

            int result = fileChooser.showOpenDialog(fileLabel);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                String filePath = selectedFile.getAbsolutePath();
                fileLabel.setText(filePath);
                parent.notifyOutputLocationSelected(selectedFile);
            }
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            super.mouseEntered(e);
            browseButton.setBackground(Frame.SELECT_COLOR);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        public void mouseExited(MouseEvent e) {
            super.mouseExited(e);
            browseButton.setBackground(new Color(0, true));
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    };

    private final PartitionPanel parent;
    public SelectDirectoryComponent(PartitionPanel parent) {
        this.parent = parent;

        setBackground(Frame.DARK_COLOR);
        addMouseListener(mouseAdapter);
        setLayout(new BorderLayout());

        fileLabel.setBorder(new EmptyBorder(4, 6, 4, 6));
        fileLabel.setHorizontalAlignment(SwingConstants.LEFT);
        fileLabel.setBackground(new Color(0, true));

        browseButton.setRolloverEnabled(false);
        browseButton.setBackground(new Color(0, true));
        browseButton.setBorder(new EmptyBorder(4, 6, 4, 6));
        browseButton.addMouseListener(mouseAdapter);

        add(fileLabel, BorderLayout.CENTER);
        add(browseButton, BorderLayout.EAST);

        setMaximumSize(new Dimension(Integer.MAX_VALUE, 27));
    }
}