import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
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

        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        if(deletedRecords.isEmpty()) {
            //stub
        } else {
            initializeRecoveryProgressBar();
            add(recoveryProgressBar);
            if(deletedRecords.get(0) instanceof MFTRecord) {
                recoverNTFSFiles();
            }  //stub
        }
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
}
