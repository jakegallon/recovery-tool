import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.prefs.Preferences;

public class RecoveryPanel extends StepPanel {

    private final DetailedProgressBar recoveryProgressBar = new DetailedProgressBar();
    private final File outputDirectory;
    private int filesProcessed = 0;

    protected boolean isLogging;
    protected final LogPanel recoveryLogPanel = new LogPanel();

    @Override
    public void onNextStep() {

    }

    @Override
    public void onBackStep() {

    }

    private final ArrayList<GenericRecord> deletedRecords;
    public RecoveryPanel(ArrayList<GenericRecord> deletedRecords) {
        this.deletedRecords = deletedRecords;
        outputDirectory = PartitionPanel.getOutput();

        Preferences prefs = Preferences.userNodeForPackage(PartitionPanel.class);
        isLogging = prefs.getBoolean("IS_LOGGING", false);

        Thread recoveryThread = new Thread(this::initializeRecovery);
        recoveryThread.start();

        BottomPanel.onIsFinished();
        BottomPanel.detachBackButton();

        Font headerFont = new Font("Arial", Font.BOLD, 17);
        Font textFont = new Font("Arial", Font.PLAIN, 14);

        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel waitLabel = new JLabel("Step 5: Wait");
        waitLabel.setFont(headerFont);
        waitLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(waitLabel);

        JLabel waitText = new JLabel("<html>The software is now attempting to recover the selected files. This may take a while depending on how large the files are. Refer to the progress bar below to track the progress of the recovery.</html>");
        waitText.setFont(textFont);
        waitText.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(waitText);

        add(Box.createVerticalStrut(10));

        initializeRecoveryProgressBar();
        add(recoveryProgressBar);

        add(Box.createVerticalStrut(10));

        recoveryLogPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(recoveryLogPanel);

        if(!isLogging) {
            recoveryLogPanel.log("Logging is not currently enabled.");
        }
    }

    private void initializeRecoveryProgressBar() {
        recoveryProgressBar.setPercentageLabelPrefix("Recovering Deleted Files:");
        recoveryProgressBar.setProgressLabelSuffix("files");
        recoveryProgressBar.setMaximum(deletedRecords.size());
        recoveryProgressBar.updateInformationLabels();
        recoveryProgressBar.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private void initializeRecovery() {
        if(deletedRecords.isEmpty()) {
            recoveryLogPanel.log("The list of files to recover is empty.", "<font size=5 color='red'>");
            recoveryLogPanel.log("Press \"Exit\" to exit the program.", "<font size=5 color='red'>");
        } else {
            if(deletedRecords.get(0) instanceof MFTRecord) {
                recoverNTFSFiles();
            } else if(deletedRecords.get(0) instanceof FAT32Record) {
                try {
                    recoverFAT32Files();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        onRecoveryEnd();
    }

    private void onRecoveryEnd() {
        BottomPanel.setNextButtonEnabled(true);
        recoveryLogPanel.shutdown();
    }

    private void recoverNTFSFiles() {
        for(GenericRecord deletedRecord : deletedRecords) {
            MFTRecord mftRecord = (MFTRecord) deletedRecord;
            if(isLogging) {
                if(mftRecord.fileName.equals("")) {
                    recoveryLogPanel.log("Initializing recovery on nameless file");
                } else {
                    recoveryLogPanel.log("Initializing recovery on " + mftRecord.fileName);
                }
            }
            try {
                mftRecord.parseDataAttribute();
                if (mftRecord.isDataResident()) {
                    recoverResidentFile(mftRecord);
                } else {
                    recoverNonResidentFile(mftRecord);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            filesProcessed ++;
            recoveryProgressBar.setValue(filesProcessed);
            recoveryProgressBar.updateInformationLabels();
            if(isLogging) {
                if(mftRecord.fileName.equals("")) {
                    recoveryLogPanel.log("Successfully finished recovery of nameless file", "<font color='#ADF5A5'>");
                } else {
                    recoveryLogPanel.log("Successfully finished recovery of " + mftRecord.fileName, "<font color='#ADF5A5'>");
                }
            }
        }
    }

    private void recoverResidentFile(MFTRecord mftRecord) throws IOException {
        byte[] fullBytes = mftRecord.getAttribute(Attribute.DATA);
        byte[] dataBytes = Arrays.copyOfRange(fullBytes, 0x18, 0x18 + (int) mftRecord.getFileSizeBytes());
        String fileName = mftRecord.getFileName();

        File newFile = new File(outputDirectory, fileName);
        FileOutputStream fos = new FileOutputStream(newFile);
        fos.write(dataBytes);
        fos.close();
    }

    private void recoverNonResidentFile(MFTRecord mftRecord) throws IOException {
        NTFSInformation ntfsInformation = NTFSInformation.getInstance();
        final int bytesPerCluster = ntfsInformation.getBytesPerCluster();
        File root = ntfsInformation.getRoot();

        String fileName = mftRecord.getFileName();
        File newFile = new File(outputDirectory, fileName);
        FileOutputStream fos = new FileOutputStream(newFile);

        HashMap<Long, Long> dataRunOffsetClusters = mftRecord.getDataRunOffsetClusters();
        for(Map.Entry<Long, Long> dataRun : dataRunOffsetClusters.entrySet()) {
            long dataRunOffsetBytes = dataRun.getKey();
            long dataRunLengthClusters = dataRun.getValue();

            RandomAccessFile diskAccess = new RandomAccessFile(root, "r");
            FileChannel diskChannel = diskAccess.getChannel();

            long internalOffsetBytes = 0L;
            for (long i = 0L; i < dataRunLengthClusters; i++) {
                diskChannel.position(dataRunOffsetBytes + internalOffsetBytes);
                byte[] thisCluster = new byte[bytesPerCluster];
                ByteBuffer buffer = ByteBuffer.wrap(thisCluster);

                diskChannel.read(buffer);
                fos.write(thisCluster);

                internalOffsetBytes += bytesPerCluster;
            }
            diskAccess.close();
        }
        fos.close();
    }

    private static final long MAX_CLUSTER_VALUE = 0x0FFFFFEF;

    private void recoverFAT32Files() throws IOException {
        for(GenericRecord deletedRecord : deletedRecords) {
            FAT32Record fat32Record = (FAT32Record) deletedRecord;

            if(isLogging) {
                if(deletedRecord.fileName.equals("")) {
                    recoveryLogPanel.log("Initializing recovery on nameless file");
                } else {
                    recoveryLogPanel.log("Initializing recovery on " + deletedRecord.fileName);
                }
            }

            FAT32Information fat32Information = FAT32Information.getInstance();
            int bytesPerCluster = fat32Information.bytesPerSector * fat32Information.sectorsPerCluster;
            int fatByteOffset = fat32Information.reservedSectors * fat32Information.bytesPerSector;
            int dataOffsetBytes = fat32Information.bytesPerSector * fat32Information.dataStartSector;

            ArrayList<Integer> thisDirectoriesStartClusters = new ArrayList<>();

            RandomAccessFile diskAccess = new RandomAccessFile(fat32Information.getRoot(), "r");
            FileChannel diskChannel = diskAccess.getChannel();
            int nextCluster = fat32Record.startCluster;

            while(nextCluster <= MAX_CLUSTER_VALUE) {
                thisDirectoriesStartClusters.add(nextCluster);

                int internalFatByteOffset = nextCluster * 4;
                long fatClusterTarget = internalFatByteOffset / bytesPerCluster;
                internalFatByteOffset -= fatClusterTarget * bytesPerCluster;

                byte[] thisCluster = Utility.readCluster(diskChannel, fatByteOffset + (fatClusterTarget * bytesPerCluster));
                nextCluster = Utility.byteArrayToUnsignedInt(Arrays.copyOfRange(thisCluster, internalFatByteOffset, internalFatByteOffset + 4), true);
            }

            byte[][] thisDirectoriesData = new byte[thisDirectoriesStartClusters.size()][bytesPerCluster];

            for(int i = 0; i < thisDirectoriesStartClusters.size(); i++) {
                byte[] thisCluster = Utility.readCluster(diskChannel, dataOffsetBytes + (long) (thisDirectoriesStartClusters.get(i) - 2) * bytesPerCluster);
                thisDirectoriesData[i] = thisCluster;
            }
            diskAccess.close();

            String fileName = fat32Record.getFileName();

            File newFile = new File(outputDirectory, fileName);
            FileOutputStream fos = new FileOutputStream(newFile);
            for(byte[] cluster : thisDirectoriesData) {
                try {
                    fos.write(cluster);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            fos.close();

            if(isLogging) {
                if(deletedRecord.fileName.equals("")) {
                    recoveryLogPanel.log("Successfully finished recovery of nameless file", "<font color='#ADF5A5'>");
                } else {
                    recoveryLogPanel.log("Successfully finished recovery of " + deletedRecord.fileName, "<font color='#ADF5A5'>");
                }
            }
        }
    }
}
