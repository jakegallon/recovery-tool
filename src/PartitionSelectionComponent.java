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
    private final PartitionPanel parent;

    public PartitionSelectionComponent(PartitionPanel parent) {
        this.parent = parent;
        setLayout(springLayout);

        initializePartitionPanel();
        initializePartitionScrollPane();
        add(partitionScrollPane);

        initializeInformationPanel();
        add(informationPanel);

        populatePartitionPanel();
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
        partitionPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
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
        springLayout.putConstraint(SpringLayout.SOUTH, this, 0, SpringLayout.SOUTH, informationPanel);
    }

    private void setActiveStorageWidget(PartitionWidget partitionWidget) {
        if(selectedPartitionWidget != null) {
            selectedPartitionWidget.setBackground(Frame.DARK_COLOR);
        }
        selectedPartitionWidget = partitionWidget;
        selectedPartitionWidget.setBackground(Frame.SELECT_COLOR);

        DriveInformation.setRoot(partitionWidget.getRoot());

        onNewSelectedPartition();
    }

    private void onNewSelectedPartition() {
        parent.notifyPartitionSelected(selectedPartitionWidget.getRoot(), selectedPartitionWidget.getFileStore().type());
        try {
            displaySelectedPartitionInformation();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private JPanel errorMessagePanel;
    private void displaySelectedPartitionInformation() throws IOException {
        informationPanel.removeAll();
        FileStore fs = selectedPartitionWidget.getFileStore();

        displayGenericInformation(fs);

        if(fs.type().equals("NTFS")) {
            if(errorMessagePanel != null) {
                remove(errorMessagePanel);
                errorMessagePanel = null;
            }
            displayNtfsInformation();
        } else if (fs.type().equals("FAT32")) {
            if(errorMessagePanel != null) {
                remove(errorMessagePanel);
                errorMessagePanel = null;
            }
            displayFat32Information();
        } else {
            displayErrorMessage(fs.type());
        }

        repaint();
        revalidate();
    }

    private void displayErrorMessage(String type) {
        errorMessagePanel = new JPanel();
        errorMessagePanel.setBorder(new LineBorder(Color.red, 1));
        BoxLayout boxLayout = new BoxLayout(errorMessagePanel, BoxLayout.X_AXIS);
        errorMessagePanel.setLayout(boxLayout);

        JLabel exclamationMark = new JLabel("!");
        exclamationMark.setForeground(Color.RED);

        JLabel errorMessage = new JLabel("<html>The selected drive's file system, " + type + ", is not currently supported by this tool.</html>");
        errorMessage.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));

        errorMessagePanel.add(Box.createHorizontalStrut(4));
        errorMessagePanel.add(exclamationMark);
        errorMessagePanel.add(Box.createHorizontalStrut(4));
        errorMessagePanel.add(errorMessage);
        errorMessagePanel.add(Box.createHorizontalStrut(4));

        springLayout.putConstraint(SpringLayout.NORTH, errorMessagePanel, 10, SpringLayout.SOUTH, partitionScrollPane);
        springLayout.putConstraint(SpringLayout.SOUTH, errorMessagePanel, 0, SpringLayout.SOUTH, informationPanel);
        springLayout.putConstraint(SpringLayout.WEST, errorMessagePanel, 0, SpringLayout.WEST, partitionScrollPane);
        springLayout.putConstraint(SpringLayout.EAST, errorMessagePanel, 0, SpringLayout.EAST, partitionScrollPane);
        exclamationMark.setFont(new Font(Font.SANS_SERIF, Font.BOLD,  48));

        add(errorMessagePanel);
    }

    private void displayGenericInformation(FileStore fs) throws IOException {
        long totalSpace = fs.getTotalSpace();
        long unallocatedSpace = fs.getUnallocatedSpace();
        long usedSpace = totalSpace - unallocatedSpace;

        Pair<String, Long> storageUnitInfo = Utility.getUnitAndUnitSize(totalSpace);
        String storageUnit = storageUnitInfo.getKey();
        long storageUnitSize = storageUnitInfo.getValue();

        DecimalFormat df = new DecimalFormat("#.##");

        JLabel storageType = new JLabel("File System: " + fs.type());
        informationPanel.add(storageType);

        String convertedUsedSpace = df.format((double) usedSpace / storageUnitSize);
        String convertedTotalSpace = df.format((double) totalSpace / storageUnitSize);
        JLabel storageSize = new JLabel("Used space: " + convertedUsedSpace + storageUnit + " / " + convertedTotalSpace + storageUnit + " (" + usedSpace  + " B / " + totalSpace + " B)");
        informationPanel.add(storageSize);

        String convertedUnallocatedSpace = df.format((double) unallocatedSpace / storageUnitSize);
        JLabel unallocatedSize = new JLabel("Unallocated Space: " + convertedUnallocatedSpace + storageUnit + " (" + unallocatedSpace + " B)");
        informationPanel.add(unallocatedSize);
    }

    private void displayNtfsInformation() {
        NTFSInformation ntfsInformation = NTFSInformation.getInstance();

        JPanel ntfsSeparator = createNamedSeparator("NTFS Information");
        informationPanel.add(ntfsSeparator);

        JLabel bytesPerSectorLabel = new JLabel("Bytes per Sector: " + ntfsInformation.getBytesPerSector());
        informationPanel.add(bytesPerSectorLabel);

        JLabel bytesPerClusterLabel = new JLabel("Bytes per Cluster: " + ntfsInformation.getBytesPerCluster());
        informationPanel.add(bytesPerClusterLabel);

        JLabel totalSectorsLabel = new JLabel("Total Sectors: " + ntfsInformation.getTotalSectors());
        informationPanel.add(totalSectorsLabel);
    }

    private void displayFat32Information() {
        FAT32Information fat32Information = FAT32Information.getInstance();

        JPanel fat32Separator = createNamedSeparator("FAT32 Information");
        informationPanel.add(fat32Separator);

        JLabel bytesPerSectorLabel = new JLabel("Bytes per Sector: " + fat32Information.getBytesPerSector());
        informationPanel.add(bytesPerSectorLabel);

        JLabel sectorsPerClusterLabel = new JLabel("Sectors per Cluster: " + fat32Information.getSectorsPerCluster());
        informationPanel.add(sectorsPerClusterLabel);

        JLabel sectorsPerFatLabel = new JLabel("Sectors per FAT: " + fat32Information.getSectorsPerFat());
        informationPanel.add(sectorsPerFatLabel);

        JLabel fatCountLabel = new JLabel("FAT count: " + fat32Information.getFatCount());
        informationPanel.add(fatCountLabel);

        JLabel reservedSectorsLabel = new JLabel("Reserved Sectors: " + fat32Information.getReservedSectors());
        informationPanel.add(reservedSectorsLabel);

        JLabel dataStartSectorLabel = new JLabel("Data Start Sector: " + fat32Information.getDataStartSector());
        informationPanel.add(dataStartSectorLabel);
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
