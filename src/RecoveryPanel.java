import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;

public class RecoveryPanel extends StepPanel {

    private final DetailedProgressBar recoveryProgressBar = new DetailedProgressBar();
    private final File outputDirectory;
    private int filesProcessed = 0;

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

        Thread recoveryThread = new Thread(this::initializeRecovery);
        recoveryThread.start();

        BottomPanel.onIsFinished();
        BottomPanel.detachBackButton();

        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        setBorder(new EmptyBorder(10, 10, 10, 10));
    }

    private void initializeRecovery() {
        if(deletedRecords.isEmpty()) {
        //stub
        } else {
            initializeRecoveryProgressBar();
            add(recoveryProgressBar);
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
        BottomPanel.setNextButtonEnabled(true);
    }

    private void initializeRecoveryProgressBar() {
        recoveryProgressBar.setPercentageLabelPrefix("Recovering Deleted Files:");
        recoveryProgressBar.setProgressLabelSuffix("files");
        recoveryProgressBar.setMaximum(deletedRecords.size());
        recoveryProgressBar.updateInformationLabels();
    }

    private void recoverNTFSFiles() {
        for(GenericRecord deletedRecord : deletedRecords) {
            MFTRecord mftRecord = (MFTRecord) deletedRecord;
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
        }
    }
}
