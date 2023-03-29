import javax.swing.*;
import javax.swing.Timer;
import java.io.*;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ScanPanel extends StepPanel {

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

    boolean isReading = false;
    private final Timer scanTimer;

    private final JProgressBar readProgressBar = new JProgressBar(0, 0);
    private JLabel readProgressLabel;
    private JLabel readProgressPercentLabel;
    private JLabel foundFilesLabel;

    private int deletedFilesFound = 0;

    private final JProgressBar processProgressBar = new JProgressBar(0, 0);
    private JLabel processProgressLabel;
    private JLabel processProgressPercentLabel;
    private JLabel processedFilesLabel;

    private int deletedFilesProcessed = 0;

    private static final ArrayList<MFTRecord> deletedRecords = new ArrayList<>();
    public static ArrayList<MFTRecord> getDeletedRecords() {
        return deletedRecords;
    }

    private static final SpringLayout springLayout = new SpringLayout();


    private void readDrive() {
        isReading = true;
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

        addReadFeedbackUI();
        addProcessFeedbackUI();

        scanTimer = new Timer(200, e -> updateInterface());
        scanTimer.start();
    }

    private void addReadFeedbackUI() {
        NTFSInformation ntfsInformation = NTFSInformation.getInstance();
        JLabel scanningDriveLabel = new JLabel("Scanning drive " + ntfsInformation.getRoot().toString().substring(4) + " ");
        add(scanningDriveLabel);
        add(readProgressBar);

        readProgressLabel = new JLabel("0 / 0 files");
        add(readProgressLabel);
        readProgressPercentLabel = new JLabel("0%");
        add(readProgressPercentLabel);
        foundFilesLabel = new JLabel("0 deleted files found.");
        add(foundFilesLabel);

        springLayout.putConstraint(SpringLayout.NORTH, scanningDriveLabel, 10, SpringLayout.NORTH, this);
        springLayout.putConstraint(SpringLayout.WEST, scanningDriveLabel, 10, SpringLayout.WEST, this);
        springLayout.putConstraint(SpringLayout.NORTH, readProgressPercentLabel, 0, SpringLayout.NORTH, scanningDriveLabel);
        springLayout.putConstraint(SpringLayout.WEST, readProgressPercentLabel, 0, SpringLayout.EAST, scanningDriveLabel);
        springLayout.putConstraint(SpringLayout.NORTH, readProgressBar, 10, SpringLayout.SOUTH, scanningDriveLabel);
        springLayout.putConstraint(SpringLayout.EAST, readProgressBar, -10, SpringLayout.EAST, this);
        springLayout.putConstraint(SpringLayout.WEST, readProgressBar, 0, SpringLayout.WEST, scanningDriveLabel);
        springLayout.putConstraint(SpringLayout.NORTH, readProgressLabel, 0, SpringLayout.NORTH, scanningDriveLabel);
        springLayout.putConstraint(SpringLayout.EAST, readProgressLabel, 0, SpringLayout.EAST, readProgressBar);
        springLayout.putConstraint(SpringLayout.NORTH, foundFilesLabel, 10, SpringLayout.SOUTH, readProgressBar);
        springLayout.putConstraint(SpringLayout.WEST, foundFilesLabel, 0, SpringLayout.WEST, scanningDriveLabel);
    }

    private void addProcessFeedbackUI() {
        JLabel processRecordsLabel = new JLabel("Processing deleted records: ");
        add(processRecordsLabel);
        add(processProgressBar);

        processProgressLabel = new JLabel("0 / 0 files");
        add(processProgressLabel);
        processProgressPercentLabel = new JLabel("0%");
        add(processProgressPercentLabel);
        processedFilesLabel = new JLabel("0 deleted files processed.");
        add(processedFilesLabel);

        springLayout.putConstraint(SpringLayout.NORTH, processRecordsLabel, 25, SpringLayout.SOUTH, foundFilesLabel);
        springLayout.putConstraint(SpringLayout.WEST, processRecordsLabel, 10, SpringLayout.WEST, this);
        springLayout.putConstraint(SpringLayout.NORTH, processProgressPercentLabel, 0, SpringLayout.NORTH, processRecordsLabel);
        springLayout.putConstraint(SpringLayout.WEST, processProgressPercentLabel, 0, SpringLayout.EAST, processRecordsLabel);
        springLayout.putConstraint(SpringLayout.NORTH, processProgressBar, 10, SpringLayout.SOUTH, processRecordsLabel);
        springLayout.putConstraint(SpringLayout.EAST, processProgressBar, -10, SpringLayout.EAST, this);
        springLayout.putConstraint(SpringLayout.WEST, processProgressBar, 0, SpringLayout.WEST, processRecordsLabel);
        springLayout.putConstraint(SpringLayout.NORTH, processProgressLabel, 0, SpringLayout.NORTH, processRecordsLabel);
        springLayout.putConstraint(SpringLayout.EAST, processProgressLabel, 0, SpringLayout.EAST, processProgressBar);
        springLayout.putConstraint(SpringLayout.NORTH, processedFilesLabel, 10, SpringLayout.SOUTH, processProgressBar);
        springLayout.putConstraint(SpringLayout.WEST, processedFilesLabel, 0, SpringLayout.WEST, processRecordsLabel);
    }

    private void updateInterface() {
        updateReadInterface();
        updateProcessInterface();

        if(!isReading && updateQueue.isEmpty()) onProcessingEnd();
    }

    private void updateReadInterface() {
        int val = readProgressBar.getValue();
        int max = readProgressBar.getMaximum();

        readProgressLabel.setText(val + " / " + max  + " files");

        double percent = (double) val/max;
        int percentInt = (int) (percent * 100);
        readProgressPercentLabel.setText(percentInt + "%");

        foundFilesLabel.setText(deletedFilesFound + " deleted files found.");
    }

    private void updateProcessInterface() {
        int val = deletedFilesProcessed;
        processProgressBar.setValue(val);
        int max = processProgressBar.getMaximum();

        processProgressLabel.setText(val + " / " + max  + " files");

        double percent = (double) val/max;
        int percentInt = (int) (percent * 100);
        processProgressPercentLabel.setText(percentInt + "%");

        processedFilesLabel.setText(deletedFilesProcessed + " deleted files processed.");
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
        isReading = false;
    }

    private void onProcessingEnd() {
        scanTimer.stop();
        BottomPanel.setNextButtonEnabled(true);
    }

    private final ConcurrentLinkedQueue<MFTRecord> updateQueue = new ConcurrentLinkedQueue<>();
    private volatile boolean isProcessing = false;

    public synchronized void addRecordToUpdateQueue(MFTRecord mftRecord) {
        updateQueue.add(mftRecord);
        if (!isProcessing) {
            isProcessing = true;
            Thread processingThread = new Thread(this::processQueue);
            processingThread.setDaemon(true);
            processingThread.start();
        }
        processProgressBar.setMaximum(deletedFilesFound);
    }

    private void processQueue() {
        while (!updateQueue.isEmpty()) {
            process(updateQueue.poll());
        }
        isProcessing = false;
    }

    private void process(MFTRecord mftRecord) {
        mftRecord.processAdditionalInformation();
        deletedFilesProcessed ++;
    }
}
