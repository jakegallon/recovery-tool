import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

public class SelectDirectoryComponent extends JPanel {

    private final JLabel fileLabel = new JLabel("");
    private final JButton browseButton = new JButton("Browse");

    private File selectedFile;
    public File getSelectedFile() {
        return selectedFile;
    }

    private final MouseAdapter mouseAdapter = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            super.mouseClicked(e);
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            int result = fileChooser.showOpenDialog(fileLabel);
            if (result == JFileChooser.APPROVE_OPTION) {
                selectedFile = fileChooser.getSelectedFile();
                String filePath = selectedFile.getAbsolutePath();
                fileLabel.setText(filePath);
            }
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            super.mouseEntered(e);
            browseButton.setBackground(Frame.SELECT_COLOR);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            super.mouseExited(e);
            browseButton.setBackground(new Color(0, true));
        }
    };

    public SelectDirectoryComponent() {
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
    }
}