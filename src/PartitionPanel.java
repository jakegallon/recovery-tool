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

public class PartitionPanel extends StepPanel {

    private final SpringLayout springLayout = new SpringLayout();

    private final JPanel partitionInformation = new JPanel();
    private StorageWidget selectedStorageWidget;

    @Override
    public void onNextStep() {
        ScanPanel scanPanel = new ScanPanel();
        Frame.setStepPanel(scanPanel);

        BottomPanel.setNextButtonEnabled(false);
        BottomPanel.setBackButtonEnabled(true);
    }

    @Override
    public void onBackStep() {

    }

    public PartitionPanel() {
        setLayout(springLayout);

        addPartitionInformation();
    }

    private void addPartitionInformation() {
        // partition selector
        JPanel partitionPanel = new JPanel();
        partitionPanel.setBackground(Frame.DARK_COLOR);
        partitionPanel.setLayout(new BoxLayout(partitionPanel, BoxLayout.PAGE_AXIS));

        JScrollPane partitionScrollPane = new JScrollPane(partitionPanel);
        partitionScrollPane.setBorder(new LineBorder(Frame.DARK_COLOR));
        partitionScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        partitionScrollPane.getVerticalScrollBar().setUnitIncrement(3);
        add(partitionScrollPane);

        springLayout.putConstraint(SpringLayout.NORTH, partitionScrollPane, 10, SpringLayout.NORTH, this);
        springLayout.putConstraint(SpringLayout.SOUTH, partitionScrollPane, 100, SpringLayout.NORTH, partitionScrollPane);
        springLayout.putConstraint(SpringLayout.WEST, partitionScrollPane, 10, SpringLayout.WEST, this);
        springLayout.putConstraint(SpringLayout.EAST, partitionScrollPane, -80, SpringLayout.HORIZONTAL_CENTER, this);

        File[] roots = File.listRoots();
        for(File f : roots) {
            StorageWidget storageWidget = new StorageWidget(f);
            partitionPanel.add(storageWidget);
        }

        // partition information
        partitionInformation.setBackground(Frame.DARK_COLOR);
        partitionInformation.setBorder(new LineBorder(Frame.DARK_COLOR));
        add(partitionInformation);

        springLayout.putConstraint(SpringLayout.NORTH, partitionInformation, 0, SpringLayout.NORTH, partitionScrollPane);
        springLayout.putConstraint(SpringLayout.SOUTH, partitionInformation, 180, SpringLayout.NORTH, partitionInformation);
        springLayout.putConstraint(SpringLayout.WEST, partitionInformation, 10, SpringLayout.EAST, partitionScrollPane);
        springLayout.putConstraint(SpringLayout.EAST, partitionInformation, -10, SpringLayout.EAST, this);
    }

    private void displaySelectedPartitionInformation() {
        partitionInformation.removeAll();

        SpringLayout springLayout = new SpringLayout();
        partitionInformation.setLayout(springLayout);

        FileStore fs = selectedStorageWidget.getFileStore();

        long totalSpace;
        long usedSpace;
        long unallocatedSpace;
        try {
            totalSpace = fs.getTotalSpace();
            usedSpace = totalSpace - fs.getUnallocatedSpace();
            unallocatedSpace = fs.getUnallocatedSpace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        double mBConversionDenominator = Math.pow(1024, 2);
        double gBConversionDenominator = Math.pow(1024, 3);
        double tBConversionDenominator = Math.pow(1024, 4);
        double conversionDenominator = 1;

        String storageUnit = " B";
        if(totalSpace > 1099511627776L) {
            conversionDenominator = tBConversionDenominator;
            storageUnit = " TB";
        }
        else if(totalSpace > 1073741824) {
            conversionDenominator = gBConversionDenominator;
            storageUnit = " GB";
        }
        else if(totalSpace > 1048576) {
            conversionDenominator = mBConversionDenominator;
            storageUnit = " MB";
        }
        else if(totalSpace > 1024) {
            conversionDenominator = 1024;
            storageUnit = " KB";
        }

        String convertedUsedSpace = String.valueOf(usedSpace/conversionDenominator).substring(0, 4);
        if(convertedUsedSpace.endsWith(".")) convertedUsedSpace = convertedUsedSpace.substring(0, 3);
        String convertedTotalSpace = String.valueOf(totalSpace/conversionDenominator).substring(0, 4);
        if(convertedTotalSpace.endsWith(".")) convertedTotalSpace = convertedTotalSpace.substring(0, 3);
        String convertedUnallocatedSpace = String.valueOf(unallocatedSpace/conversionDenominator).substring(0, 5);
        if(convertedUnallocatedSpace.endsWith(".")) convertedUnallocatedSpace = convertedUnallocatedSpace.substring(0, 4);

        JLabel storageType = new JLabel("File System: " + fs.type());
        partitionInformation.add(storageType);

        JLabel storageSize = new JLabel("Used space: " + convertedUsedSpace + storageUnit + " / " + convertedTotalSpace + storageUnit + " (" + usedSpace  + " B / " + totalSpace + " B)");
        partitionInformation.add(storageSize);

        JLabel unallocatedSize = new JLabel("Unallocated Space: " + convertedUnallocatedSpace + storageUnit + " (" + unallocatedSpace + " B)");
        partitionInformation.add(unallocatedSize);

        springLayout.putConstraint(SpringLayout.NORTH, storageType, 3, SpringLayout.NORTH, partitionInformation);
        springLayout.putConstraint(SpringLayout.WEST, storageType, 3, SpringLayout.WEST, partitionInformation);

        springLayout.putConstraint(SpringLayout.NORTH, storageSize, 3, SpringLayout.SOUTH, storageType);
        springLayout.putConstraint(SpringLayout.WEST, storageSize, 3, SpringLayout.WEST, partitionInformation);

        springLayout.putConstraint(SpringLayout.NORTH, unallocatedSize, 3, SpringLayout.SOUTH, storageSize);
        springLayout.putConstraint(SpringLayout.WEST, unallocatedSize, 3, SpringLayout.WEST, partitionInformation);

        if(fs.type().equals("NTFS")) {
            displayNtfsInformation(springLayout, unallocatedSize);
        }

        partitionInformation.repaint();
        partitionInformation.revalidate();
    }

    private void displayNtfsInformation(SpringLayout springLayout, Component northernConstraint) {
        NTFSInformation ntfsInformation = NTFSInformation.createInstance(selectedStorageWidget.getRoot());

        JSeparator ntfsSeparator = new JSeparator();
        JLabel ntfsInformationLabel = new JLabel("NTFS Information");
        ntfsInformationLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        ntfsInformationLabel.setForeground(ntfsSeparator.getForeground());
        partitionInformation.add(ntfsInformationLabel);
        partitionInformation.add(ntfsSeparator);

        springLayout.putConstraint(SpringLayout.NORTH, ntfsInformationLabel, 3, SpringLayout.SOUTH, northernConstraint);
        springLayout.putConstraint(SpringLayout.WEST, ntfsInformationLabel, 3, SpringLayout.WEST, partitionInformation);
        springLayout.putConstraint(SpringLayout.VERTICAL_CENTER, ntfsSeparator, 2, SpringLayout.VERTICAL_CENTER, ntfsInformationLabel);
        springLayout.putConstraint(SpringLayout.WEST, ntfsSeparator, 3, SpringLayout.EAST, ntfsInformationLabel);
        springLayout.putConstraint(SpringLayout.EAST, ntfsSeparator, -3, SpringLayout.EAST, partitionInformation);

        JLabel bytesPerSectorLabel = new JLabel("Bytes per Sector: " + ntfsInformation.getBytesPerSector());
        partitionInformation.add(bytesPerSectorLabel);

        JLabel bytesPerClusterLabel = new JLabel("Bytes per Cluster: " + ntfsInformation.getBytesPerCluster());
        partitionInformation.add(bytesPerClusterLabel);

        JLabel totalSectorsLabel = new JLabel("Total Sectors: " + ntfsInformation.getTotalSectors());
        partitionInformation.add(totalSectorsLabel);

        springLayout.putConstraint(SpringLayout.NORTH, bytesPerSectorLabel, 2, SpringLayout.SOUTH, ntfsInformationLabel);
        springLayout.putConstraint(SpringLayout.WEST, bytesPerSectorLabel, 3, SpringLayout.WEST, partitionInformation);

        springLayout.putConstraint(SpringLayout.NORTH, bytesPerClusterLabel, 3, SpringLayout.SOUTH, bytesPerSectorLabel);
        springLayout.putConstraint(SpringLayout.WEST, bytesPerClusterLabel, 3, SpringLayout.WEST, partitionInformation);

        springLayout.putConstraint(SpringLayout.NORTH, totalSectorsLabel, 3, SpringLayout.SOUTH, bytesPerClusterLabel);
        springLayout.putConstraint(SpringLayout.WEST, totalSectorsLabel, 3, SpringLayout.WEST, partitionInformation);
    }

    private void setActiveStorageWidget(StorageWidget storageWidget) {
        if(selectedStorageWidget != null) {
            selectedStorageWidget.setBackground(Frame.DARK_COLOR);
        }
        selectedStorageWidget = storageWidget;
        selectedStorageWidget.setBackground(Frame.SELECT_COLOR);

        displaySelectedPartitionInformation();

        BottomPanel.setNextButtonEnabled(true);
    }

    private class StorageWidget extends JPanel implements MouseListener {

        private final FileStore fileStore;
        private final String assignedLetter;

        private StorageWidget(File file) {
            try {
                fileStore = Files.getFileStore(Paths.get(file.getAbsolutePath()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            addMouseListener(this);

            setLayout(new FlowLayout(FlowLayout.LEFT, 6, 4));
            setBackground(Frame.DARK_COLOR);

            String partitionName = FileSystemView.getFileSystemView().getSystemDisplayName(file);
            partitionName = partitionName.substring(0, partitionName.length() - 5);
            String partitionDrive = file.getAbsolutePath();

            assignedLetter = partitionDrive.substring(0, 1);

            JLabel partitionDriveLabel = new JLabel(partitionDrive);
            add(partitionDriveLabel);

            JLabel partitionNameLabel = new JLabel(partitionName);
            add(partitionNameLabel);
        }

        public FileStore getFileStore() {
            return fileStore;
        }

        public File getRoot() {
            return new File ("\\\\.\\" + assignedLetter +":");
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if(selectedStorageWidget != this) {
                setActiveStorageWidget(this);
            }
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            if(selectedStorageWidget != this) {
                setBackground(Frame.HOVER_COLOR);
            }
        }

        @Override
        public void mouseExited(MouseEvent e) {
            if(selectedStorageWidget != this) {
                setBackground(Frame.DARK_COLOR);
            }
        }

        @Override
        public void mouseClicked(MouseEvent e) {}

        @Override
        public void mouseReleased(MouseEvent e) {}
    }
}
