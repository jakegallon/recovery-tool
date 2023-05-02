import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.FileChannel;
import java.util.*;

public class NTFSScanPanel extends ScanPanel {

    public NTFSScanPanel() {
        super();
        setReadRunnable(NTFSScanner);

        NTFSInformation ntfsInformation = NTFSInformation.getInstance();
        readProgressBar.setPercentageLabelPrefix("Scanning drive " + ntfsInformation.getRoot().toString().substring(4));
    }

    private final Runnable NTFSScanner = () -> {
        isReading = true;
        try {
            scanRootForDeletedFiles();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    };

    private void scanRootForDeletedFiles() throws IOException {
        NTFSInformation ntfsInformation = NTFSInformation.getInstance();
        int mftRecordLength = ntfsInformation.getMFTRecordLength();

        RandomAccessFile diskAccess = new RandomAccessFile(ntfsInformation.getRoot(), "r");
        FileChannel diskChannel = diskAccess.getChannel();

        MFTRecord mft = new MFTRecord(readMFTRecord(diskChannel, ntfsInformation.getMFTByteLocation()));
        mft.parseDataAttribute();
        if(mft.isDataResident()) {
            throw new RuntimeException("$MFT has resident data attribute");
        }

        LinkedHashMap<Long, Long> dataRunOffsetClusters = mft.getDataRunOffsetClusters();

        int totalFilesCounter = 0;
        for(Map.Entry<Long, Long> dataRun : dataRunOffsetClusters.entrySet()) {
            totalFilesCounter += dataRun.getValue();
        }
        readProgressBar.setMaximum(totalFilesCounter*4);

        int currentFileOffset = 0;

        for(Map.Entry<Long, Long> dataRun : dataRunOffsetClusters.entrySet()) {
            long offsetBytes = dataRun.getKey();
            long fileLength = dataRun.getValue() * 4;
            for(int i = 0; i <= fileLength; i++) {
                if(!isReading) return;
                try {
                    readProgressBar.setValue(currentFileOffset + i);

                    byte[] mftRecordBytes = readMFTRecord(diskChannel, offsetBytes);
                    if (mftRecordBytes != null) {
                        MFTRecord mftRecord = new MFTRecord(mftRecordBytes);
                        if(mftRecord.isDeleted()){
                            deletedFilesFound++;
                            deletedRecords.add(mftRecord);
                            if(isLogging) {
                                scanLogPanel.log("Found deleted file " + mftRecord.getFileName());
                            }
                            addRecordToUpdateQueue(mftRecord);
                        } else {
                            if(isLogging) {
                                if(!mftRecord.getFileName().equals("")) {
                                    scanLogPanel.log("Found present file " + mftRecord.getFileName());
                                } else {
                                    scanLogPanel.log("Found nameless present file");
                                }
                            }
                        }
                    }
                    offsetBytes += mftRecordLength;
                } catch (ClosedByInterruptException e) {
                    return;
                }
            }
            currentFileOffset += fileLength;
        }
        diskAccess.close();
        isReading = false;
    }

    private byte[] readMFTRecord(FileChannel diskChannel, long offset) throws IOException {
        NTFSInformation ntfsInformation = NTFSInformation.getInstance();
        byte[] mftRecord = new byte[ntfsInformation.getMFTRecordLength()];
        ByteBuffer buffer = ByteBuffer.wrap(mftRecord);

        diskChannel.position(offset);
        diskChannel.read(buffer);

        if(!Arrays.equals(Arrays.copyOf(mftRecord, 4), new byte[]{0x46, 0x49, 0x4C, 0x45})) {
            return null;
        }
        return mftRecord;
    }
}