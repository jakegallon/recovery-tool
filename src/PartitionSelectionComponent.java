import javafx.util.Pair;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;

public class PartitionSelectionComponent extends JPanel {

    private final JPanel partitionPanel = new JPanel();
    private final JPanel informationPanel = new JPanel();
    private final JScrollPane partitionScrollPane = new JScrollPane(partitionPanel);
    private PartitionWidget selectedPartitionWidget;

    private final SpringLayout springLayout = new SpringLayout();

    public PartitionSelectionComponent() {
        setLayout(springLayout);

        initializePartitionPanel();
        initializePartitionScrollPane();
        add(partitionScrollPane);

        initializeInformationPanel();
        add(informationPanel);

        populatePartitionPanel();
        setPreferredSize(new Dimension(0, springLayout.getConstraint(SpringLayout.SOUTH, informationPanel).getValue()));
    }

    private void initializePartitionPanel() {
        partitionPanel.setBackground(Frame.DARK_COLOR);
        partitionPanel.setLayout(new BoxLayout(partitionPanel, BoxLayout.PAGE_AXIS));
    }

    private void populatePartitionPanel() {
        File[] roots = File.listRoots();
        for(File f : roots) {
            PartitionWidget partitionWidget = new PartitionWidget(f);
            partitionPanel.add(partitionWidget);
        }
    }

    private void initializePartitionScrollPane() {
        partitionScrollPane.setBorder(new LineBorder(Frame.DARK_COLOR));
        partitionScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        partitionScrollPane.getVerticalScrollBar().setUnitIncrement(3);

        springLayout.putConstraint(SpringLayout.NORTH, partitionScrollPane, 0, SpringLayout.NORTH, this);
        springLayout.putConstraint(SpringLayout.SOUTH, partitionScrollPane, 100, SpringLayout.NORTH, partitionScrollPane);
        springLayout.putConstraint(SpringLayout.WEST, partitionScrollPane, 0, SpringLayout.WEST, this);
        springLayout.putConstraint(SpringLayout.EAST, partitionScrollPane, -80, SpringLayout.HORIZONTAL_CENTER, this);
    }

    private void initializeInformationPanel() {
        informationPanel.setBackground(Frame.DARK_COLOR);
        informationPanel.setBorder(new LineBorder(Frame.DARK_COLOR));
        informationPanel.setLayout(new BoxLayout(informationPanel, BoxLayout.PAGE_AXIS));

        springLayout.putConstraint(SpringLayout.NORTH, informationPanel, 0, SpringLayout.NORTH, this);
        springLayout.putConstraint(SpringLayout.SOUTH, informationPanel, 180, SpringLayout.NORTH, informationPanel);
        springLayout.putConstraint(SpringLayout.WEST, informationPanel, 10, SpringLayout.EAST, partitionScrollPane);
        springLayout.putConstraint(SpringLayout.EAST, informationPanel, 0, SpringLayout.EAST, this);
    }

    private void setActiveStorageWidget(PartitionWidget partitionWidget) {
        if(selectedPartitionWidget != null) {
            selectedPartitionWidget.setBackground(Frame.DARK_COLOR);
        }
        selectedPartitionWidget = partitionWidget;
        selectedPartitionWidget.setBackground(Frame.SELECT_COLOR);

        onNewSelectedPartition();
        BottomPanel.setNextButtonEnabled(true);
    }

    private void onNewSelectedPartition() {
        try {
            displaySelectedPartitionInformation();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void displaySelectedPartitionInformation() throws IOException {
        informationPanel.removeAll();

        FileStore fs = selectedPartitionWidget.getFileStore();

        long totalSpace = fs.getTotalSpace();
        long unallocatedSpace = fs.getUnallocatedSpace();
        long usedSpace = totalSpace - unallocatedSpace;

        Pair<String, Long> storageUnitInfo = getStorageUnitInfo(totalSpace);
        String storageUnit = storageUnitInfo.getKey();
        long storageUnitBytes = storageUnitInfo.getValue();

        DecimalFormat df = new DecimalFormat("#.##");

        JLabel storageType = new JLabel("File System: " + fs.type());
        informationPanel.add(storageType);

        String convertedUsedSpace = df.format((double) usedSpace / storageUnitBytes);
        String convertedTotalSpace = df.format((double) totalSpace / storageUnitBytes);
        JLabel storageSize = new JLabel("Used space: " + convertedUsedSpace + storageUnit + " / " + convertedTotalSpace + storageUnit + " (" + usedSpace  + " B / " + totalSpace + " B)");
        informationPanel.add(storageSize);

        String convertedUnallocatedSpace = df.format((double) unallocatedSpace / storageUnitBytes);
        JLabel unallocatedSize = new JLabel("Unallocated Space: " + convertedUnallocatedSpace + storageUnit + " (" + unallocatedSpace + " B)");
        informationPanel.add(unallocatedSize);

        if(fs.type().equals("NTFS")) {
            displayNtfsInformation();
        }

        informationPanel.repaint();
        informationPanel.revalidate();
    }

    private Pair<String, Long> getStorageUnitInfo(long totalSpace) {
        String[] storageUnits = {" B", " KB", " MB", " GB", " TB", " PB", " EB"};
        long[] bytesPerUnit = {0, 1024, 1048576, 1073741824, 1099511627776L, 1125899906842624L, 1152921504606846976L};

        int i = 0;
        while(totalSpace >= bytesPerUnit[i]) {
            i++;
            if(i + 1 > bytesPerUnit.length) break;
        }

        return new Pair<>(storageUnits[i-1], bytesPerUnit[i-1]);
    }

    private void displayNtfsInformation() {
        NTFSInformation ntfsInformation = NTFSInformation.createInstance(selectedPartitionWidget.getRoot());

        JPanel ntfsSeparator = createNamedSeparator("NTFS Information");
        informationPanel.add(ntfsSeparator);

        JLabel bytesPerSectorLabel = new JLabel("Bytes per Sector: " + ntfsInformation.getBytesPerSector());
        informationPanel.add(bytesPerSectorLabel);

        JLabel bytesPerClusterLabel = new JLabel("Bytes per Cluster: " + ntfsInformation.getBytesPerCluster());
        informationPanel.add(bytesPerClusterLabel);

        JLabel totalSectorsLabel = new JLabel("Total Sectors: " + ntfsInformation.getTotalSectors());
        informationPanel.add(totalSectorsLabel);
    }

    private JPanel createNamedSeparator(String name) {
        JPanel namedSeparator = new JPanel();

        SpringLayout namedSeparatorLayout = new SpringLayout();
        namedSeparator.setLayout(namedSeparatorLayout);
        namedSeparator.setBackground(new Color(0, true));

        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        namedSeparator.add(nameLabel);
        JSeparator firstSeparator = new JSeparator();
        namedSeparator.add(firstSeparator);
        JSeparator lastSeparator = new JSeparator();
        namedSeparator.add(lastSeparator);

        namedSeparatorLayout.putConstraint(SpringLayout.NORTH, firstSeparator, -1, SpringLayout.VERTICAL_CENTER, nameLabel);
        namedSeparatorLayout.putConstraint(SpringLayout.SOUTH, firstSeparator, 1, SpringLayout.VERTICAL_CENTER, nameLabel);
        namedSeparatorLayout.putConstraint(SpringLayout.WEST, firstSeparator, 0, SpringLayout.WEST, informationPanel);
        namedSeparatorLayout.putConstraint(SpringLayout.EAST, firstSeparator, 5, SpringLayout.WEST, firstSeparator);
        namedSeparatorLayout.putConstraint(SpringLayout.WEST, nameLabel, 5, SpringLayout.EAST, firstSeparator);
        namedSeparatorLayout.putConstraint(SpringLayout.NORTH, lastSeparator, -1, SpringLayout.VERTICAL_CENTER, nameLabel);
        namedSeparatorLayout.putConstraint(SpringLayout.SOUTH, lastSeparator, 1, SpringLayout.VERTICAL_CENTER, nameLabel);
        namedSeparatorLayout.putConstraint(SpringLayout.WEST, lastSeparator, 5, SpringLayout.EAST, nameLabel);
        namedSeparatorLayout.putConstraint(SpringLayout.EAST, lastSeparator, informationPanel.getWidth(), SpringLayout.WEST, informationPanel);

        namedSeparator.setMaximumSize(new Dimension(Integer.MAX_VALUE, nameLabel.getPreferredSize().height));
        nameLabel.setForeground(firstSeparator.getForeground());

        return namedSeparator;
    }

    private class PartitionWidget extends JPanel implements MouseListener {

        private final File file;
        private String assignedLetter;

        private PartitionWidget(File file) {
            this.file = file;
            init();
        }

        public FileStore getFileStore() {
            try {
                return Files.getFileStore(Paths.get(file.getAbsolutePath()));
            } catch (IOException e) {
                throw new RuntimeException("Partition Widget failed to acquire FileStore");
            }
        }

        public File getRoot() {
            return new File ("\\\\.\\" + assignedLetter +":");
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if(selectedPartitionWidget != this) {
                setActiveStorageWidget(this);
            }
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            if(selectedPartitionWidget != this) {
                setBackground(Frame.HOVER_COLOR);
            }
        }

        @Override
        public void mouseExited(MouseEvent e) {
            if(selectedPartitionWidget != this) {
                setBackground(Frame.DARK_COLOR);
            }
        }

        @Override
        public void mouseClicked(MouseEvent e) {}

        @Override
        public void mouseReleased(MouseEvent e) {}

        private void init() {
            addMouseListener(this);
            setBackground(Frame.DARK_COLOR);
            setLayout(new FlowLayout(FlowLayout.LEFT, 6, 4));

            addPartitionNameLabel();
            addPartitionDriveLabel();
        }

        private void addPartitionNameLabel() {
            String partitionName = FileSystemView.getFileSystemView().getSystemDisplayName(file);
            partitionName = partitionName.substring(0, partitionName.length() - 5);

            JLabel partitionNameLabel = new JLabel(partitionName);
            add(partitionNameLabel);
        }

        private void addPartitionDriveLabel() {
            String partitionDrive = file.getAbsolutePath();
            assignedLetter = partitionDrive.substring(0, 1);

            JLabel partitionDriveLabel = new JLabel(partitionDrive);
            add(partitionDriveLabel);
        }
    }
}
