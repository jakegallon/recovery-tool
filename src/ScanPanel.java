import java.io.*;
import java.nio.channels.FileChannel;

public class ScanPanel extends StepPanel {

    @Override
    public void onNextStep() {

    }

    @Override
    public void onBackStep() {
        PartitionPanel partitionPanel = new PartitionPanel();
        Frame.setStepPanel(partitionPanel);

        BottomPanel.setNextButtonEnabled(true);
        BottomPanel.setBackButtonEnabled(false);
    }

    public ScanPanel() {
        try {
            printAllFileNames();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void printAllFileNames() throws IOException {
        NTFSInformation ntfsInformation = NTFSInformation.getInstance();
        long mftSizeBytes = ntfsInformation.getMFTSizeBytes();
        int mftRecordLength = ntfsInformation.getMFTRecordLength();
        long mftRecordsInMFT = mftSizeBytes/mftRecordLength;

        RandomAccessFile diskAccess = new RandomAccessFile(ntfsInformation.getRoot(), "r");
        FileChannel diskChannel = diskAccess.getChannel();

        long offset = ntfsInformation.getMFTByteLocation();
        for(int i = 0; i < mftRecordsInMFT; i++) {
            byte[] mftRecordBytes = Utility.readMFTRecord(diskChannel, offset);
            if (mftRecordBytes != null) {
                MFTRecord mftRecord = new MFTRecord(mftRecordBytes);
                if(mftRecord.isDeleted()){
                    System.out.println(mftRecord.getFileName());
                }
            }
            offset += mftRecordLength;
        }
        diskAccess.close();
    }
}
