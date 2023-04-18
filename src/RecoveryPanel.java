import org.apache.tika.Tika;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;

public class RecoveryPanel extends StepPanel {

    private final DetailedProgressBar recoveryProgressBar = new DetailedProgressBar();
    private final File outputDirectory;
    private int filesProcessed = 0;

    protected boolean isLogging;
    protected final LogPanel recoveryLogPanel = new LogPanel(Integer.MAX_VALUE);

    @Override
    public void onNextStep() {

    }

    @Override
    public void onBackStep() {

    }

    private final ArrayList<GenericRecord> deletedRecords;
    public RecoveryPanel(ArrayList<GenericRecord> deletedRecords) {
        this.deletedRecords = deletedRecords;

        Preferences preferences = Preferences.userNodeForPackage(PartitionPanel.class);
        String selectedFilePath = preferences.get("LAST_DIRECTORY", null);
        outputDirectory = new File(selectedFilePath);

        Preferences prefs = Preferences.userNodeForPackage(PartitionPanel.class);
        isLogging = prefs.getBoolean("IS_LOGGING", false);

        if(!isLogging) {
            recoveryLogPanel.log("Although logging is disabled, recovery will still be logged.");
            recoveryLogPanel.log("This has negligible impact on performance and allows you to know why some files may not be recovered.");
        }

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
            if(mftRecord.fileName.equals("")) {
                recoveryLogPanel.log("Initializing recovery on nameless file");
            } else {
                recoveryLogPanel.log("Initializing recovery on " + mftRecord.fileName);
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

        LinkedHashMap<Long, Long> dataRunOffsetClusters = mftRecord.getDataRunOffsetClusters();

        if(dataRunOffsetClusters == null) {
            recoveryLogPanel.log("Failed recovery of " + (!mftRecord.fileName.equals("") ? mftRecord.fileName : "nameless file."), "<font color='#FF382E'>");
            recoveryLogPanel.log("↪ Reason: Unreadable MFT record Data Runs", "<font color='#FF382E'>");
            return;
        }

        String fileName = mftRecord.getFileName();
        File newFile = new File(outputDirectory, fileName);
        FileOutputStream fos = new FileOutputStream(newFile);

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

        Tika tika = new Tika();
        String mimeType = tika.detect(newFile);
        MimeTypes allTypes = MimeTypes.getDefaultMimeTypes();
        if(!mimeType.equals("application/octet-stream") && !mimeType.equals("text/plain")) {
            try {
                List<String> extensions = allTypes.forName(mimeType).getExtensions();

                boolean isMatching = false;
                for (String extension : extensions) {
                    if (extension.equalsIgnoreCase(mftRecord.fileExtension)) {
                        isMatching = true;
                        break;
                    }
                }
                if (!isMatching) {
                    recoveryLogPanel.log("Failed recovery of " + (!mftRecord.fileName.equals("") ? mftRecord.fileName : "nameless file."), "<font color='#FF382E'>");
                    recoveryLogPanel.log("↪ Reason: MIME data mismatch. Extension: " + mftRecord.fileExtension + " incompatible with MIME: " + mimeType, "<font color='#FF382E'>");
                    Files.delete(newFile.toPath());
                    return;
                }
            } catch (MimeTypeException e) {
                throw new RuntimeException(e);
            }
        } else {
            recoveryLogPanel.log("File " + (!mftRecord.fileName.equals("") ? mftRecord.fileName : "nameless file") + " has default MIME type and may be corrupt", "<font color='#F27F0C'>");
        }
        if(mftRecord.fileName.equals("")) {
            recoveryLogPanel.log("Successfully finished recovery of nameless file", "<font color='#ADF5A5'>");
        } else {
            recoveryLogPanel.log("Successfully finished recovery of " + mftRecord.fileName, "<font color='#ADF5A5'>");
        }
    }

    private static final long MAX_CLUSTER_VALUE = 0x0FFFFFEF;

    private void recoverFAT32Files() throws IOException {
        for(GenericRecord deletedRecord : deletedRecords) {
            FAT32Record fat32Record = (FAT32Record) deletedRecord;

            if(deletedRecord.fileName.equals("")) {
                recoveryLogPanel.log("Initializing recovery on nameless file");
            } else {
                recoveryLogPanel.log("Initializing recovery on " + deletedRecord.fileName);
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

            if(deletedRecord.fileName.equals("")) {
                recoveryLogPanel.log("Successfully finished recovery of nameless file", "<font color='#ADF5A5'>");
            } else {
                recoveryLogPanel.log("Successfully finished recovery of " + deletedRecord.fileName, "<font color='#ADF5A5'>");
            }
        }
    }
}
