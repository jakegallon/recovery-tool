import java.io.*;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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
        int mftRecordLength = ntfsInformation.getMFTRecordLength();

        RandomAccessFile diskAccess = new RandomAccessFile(ntfsInformation.getRoot(), "r");
        FileChannel diskChannel = diskAccess.getChannel();

        MFTRecord mft = new MFTRecord(Utility.readMFTRecord(diskChannel, ntfsInformation.getMFTByteLocation()));
        if(mft.isDataResident()) {
            throw new RuntimeException("$MFT has resident data attribute");
        }

        HashMap<Long, Long> dataRunOffsetFiles = new HashMap<>();

        byte[] dataAttribute = mft.getAttribute(Attribute.DATA);
        int dataAttributeDataRunsOffset = dataAttribute[0x20];
        byte[] dataRunBytes = Arrays.copyOfRange(dataAttribute, dataAttributeDataRunsOffset, dataAttribute.length);
        int dataRunOffset = 0;
        while(dataRunOffset < dataRunBytes.length) {
            if (dataRunBytes[dataRunOffset] == 0x00) break;

            int judgementByte = dataRunBytes[dataRunOffset] & 0xFF;
            int startLength = judgementByte / 16;
            int lengthLength = judgementByte % 16;

            long lengthClusters = Utility.byteArrayToLong(Arrays.copyOfRange(dataRunBytes, dataRunOffset+1, dataRunOffset+1+lengthLength), true) & 0xFFFFFFFFL;
            long lengthFiles = lengthClusters * 4;
            long startClusters = Utility.byteArrayToLong(Arrays.copyOfRange(dataRunBytes, dataRunOffset+1+lengthLength, dataRunOffset+1+lengthLength+startLength), true) & 0xFFFFFFFFL;
            long startBytes = startClusters*4096;

            dataRunOffsetFiles.put(startBytes, lengthFiles);
            dataRunOffset += startLength + lengthLength + 1;
        }

        for(Map.Entry<Long, Long> dataRun : dataRunOffsetFiles.entrySet()) {
            long offsetBytes = dataRun.getKey();
            for(int i = 0; i < dataRun.getValue(); i++) {
                byte[] mftRecordBytes = Utility.readMFTRecord(diskChannel, offsetBytes);
                if (mftRecordBytes != null) {
                    MFTRecord mftRecord = new MFTRecord(mftRecordBytes);
                    if(mftRecord.isDeleted()){
                        System.out.println(mftRecord.getFileName());
                    }
                }
                offsetBytes += mftRecordLength;
            }
        }

        diskAccess.close();
    }
}
