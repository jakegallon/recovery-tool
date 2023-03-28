import javax.swing.*;
import javax.swing.Timer;
import java.io.*;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScanPanel extends StepPanel {

    private int deletedFilesFound = 0;
    private final JProgressBar readProgressBar = new JProgressBar(0, 0);

    @Override
    public void onNextStep() {
        FilterPanel filterPanel = new FilterPanel();
        Frame.setStepPanel(filterPanel);

        BottomPanel.setNextButtonEnabled(false);
        BottomPanel.setBackButtonEnabled(true);
    }

    @Override
    public void onBackStep() {
        PartitionPanel partitionPanel = new PartitionPanel();
        Frame.setStepPanel(partitionPanel);

        BottomPanel.setNextButtonEnabled(true);
        BottomPanel.setBackButtonEnabled(false);
    }

    private final Timer scanTimer;
    private final JLabel progressLabel;
    private final JLabel progressPercentLabel;
    private final JLabel foundFilesLabel;

    private static final ArrayList<MFTRecord> deletedRecords = new ArrayList<>();
    public static ArrayList<MFTRecord> getDeletedRecords() {
        return deletedRecords;
    }

    private static final SpringLayout springLayout = new SpringLayout();


    private void readDrive() {
        try {
            scanRootForDeletedFiles();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ScanPanel() {
        Thread readThread = new Thread(this::readDrive);
        readThread.start();

        setLayout(springLayout);

        NTFSInformation ntfsInformation = NTFSInformation.getInstance();
        JLabel scanningDriveLabel = new JLabel("Scanning drive " + ntfsInformation.getRoot().toString().substring(4) + " ");
        add(scanningDriveLabel);
        add(readProgressBar);

        progressLabel = new JLabel("0 / 0 files");
        add(progressLabel);
        progressPercentLabel = new JLabel("0%");
        add(progressPercentLabel);
        foundFilesLabel = new JLabel("0 deleted files found.");
        add(foundFilesLabel);

        springLayout.putConstraint(SpringLayout.NORTH, scanningDriveLabel, 10, SpringLayout.NORTH, this);
        springLayout.putConstraint(SpringLayout.WEST, scanningDriveLabel, 10, SpringLayout.WEST, this);
        springLayout.putConstraint(SpringLayout.NORTH, progressPercentLabel, 0, SpringLayout.NORTH, scanningDriveLabel);
        springLayout.putConstraint(SpringLayout.WEST, progressPercentLabel, 0, SpringLayout.EAST, scanningDriveLabel);
        springLayout.putConstraint(SpringLayout.NORTH, readProgressBar, 10, SpringLayout.SOUTH, scanningDriveLabel);
        springLayout.putConstraint(SpringLayout.EAST, readProgressBar, -10, SpringLayout.EAST, this);
        springLayout.putConstraint(SpringLayout.WEST, readProgressBar, 0, SpringLayout.WEST, scanningDriveLabel);
        springLayout.putConstraint(SpringLayout.NORTH, progressLabel, 0, SpringLayout.NORTH, scanningDriveLabel);
        springLayout.putConstraint(SpringLayout.EAST, progressLabel, 0, SpringLayout.EAST, readProgressBar);
        springLayout.putConstraint(SpringLayout.NORTH, foundFilesLabel, 10, SpringLayout.SOUTH, readProgressBar);
        springLayout.putConstraint(SpringLayout.WEST, foundFilesLabel, 0, SpringLayout.WEST, scanningDriveLabel);

        scanTimer = new Timer(200, e -> updateScanInterface());
        scanTimer.start();
    }

    private void updateScanInterface() {
        int val = readProgressBar.getValue();
        int max = readProgressBar.getMaximum();

        progressLabel.setText(val + " / " + max  + " objects");

        double percent = (double) val/max;
        int percentInt = (int) (percent * 100);
        progressPercentLabel.setText(percentInt + "%");

        foundFilesLabel.setText(deletedFilesFound + " deleted files found.");
    }

    private void scanRootForDeletedFiles() throws IOException {
        NTFSInformation ntfsInformation = NTFSInformation.getInstance();
        int mftRecordLength = ntfsInformation.getMFTRecordLength();

        RandomAccessFile diskAccess = new RandomAccessFile(ntfsInformation.getRoot(), "r");
        FileChannel diskChannel = diskAccess.getChannel();

        MFTRecord mft = new MFTRecord(Utility.readMFTRecord(diskChannel, ntfsInformation.getMFTByteLocation()));
        mft.parseDataAttribute();
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

            long lengthClusters = Utility.byteArrayToUnsignedLong(Arrays.copyOfRange(dataRunBytes, dataRunOffset+1, dataRunOffset+1+lengthLength), true);
            long lengthFiles = lengthClusters * 4;
            long startClusters = Utility.byteArrayToUnsignedLong(Arrays.copyOfRange(dataRunBytes, dataRunOffset+1+lengthLength, dataRunOffset+1+lengthLength+startLength), true);
            long startBytes = startClusters*4096;

            dataRunOffsetFiles.put(startBytes, lengthFiles);
            dataRunOffset += startLength + lengthLength + 1;
        }

        int totalFilesCounter = 0;
        for(Map.Entry<Long, Long> dataRun : dataRunOffsetFiles.entrySet()) {
            totalFilesCounter += dataRun.getValue();
        }
        readProgressBar.setMaximum(totalFilesCounter);

        int currentFileOffset = 0;

        for(Map.Entry<Long, Long> dataRun : dataRunOffsetFiles.entrySet()) {
            long offsetBytes = dataRun.getKey();
            for(int i = 0; i <= dataRun.getValue(); i++) {
                readProgressBar.setValue(currentFileOffset + i);

                byte[] mftRecordBytes = Utility.readMFTRecord(diskChannel, offsetBytes);
                if (mftRecordBytes != null) {
                    MFTRecord mftRecord = new MFTRecord(mftRecordBytes);
                    if(mftRecord.isDeleted()){
                        deletedFilesFound++;
                        deletedRecords.add(mftRecord);
                        addRecordToUpdateQueue(mftRecord);
                    }
                }
                offsetBytes += mftRecordLength;
            }
            currentFileOffset += dataRun.getValue();
        }
        diskAccess.close();
        onScanEnd();
    }

    private void onScanEnd() {
        scanTimer.stop();
        updateScanInterface();
        BottomPanel.setNextButtonEnabled(true);
    }

    private final List<MFTRecord> updateQueue = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile boolean isProcessing = false;

    public synchronized void addRecordToUpdateQueue(MFTRecord mftRecord) {
        updateQueue.add(mftRecord);
        if (!isProcessing) {
            isProcessing = true;
            startProcessingThread();
        }
    }

    private void startProcessingThread() {
        executor.submit(() -> {
            while (!updateQueue.isEmpty()) {
                process(updateQueue.get(0));
                updateQueue.remove(0);
            }
            isProcessing = false;
        });
    }

    private void process(MFTRecord mftRecord) {
        mftRecord.processAdditionalInformation();
    }
}
