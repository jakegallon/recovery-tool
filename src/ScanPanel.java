import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
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

    private JLabel foundFilesLabel;

    private int deletedFilesFound = 0;
    private DetailedProgressBar readProgressBar;
    private DetailedProgressBar processProgressBar;

    private int deletedFilesProcessed = 0;

    private static final ArrayList<MFTRecord> deletedRecords = new ArrayList<>();
    public static ArrayList<MFTRecord> getDeletedRecords() {
        return deletedRecords;
    }

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

        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        addReadFeedbackUI();
        addProcessFeedbackUI();

        scanTimer = new Timer(200, e -> updateInterface());
        scanTimer.start();
    }

    private void addReadFeedbackUI() {
        NTFSInformation ntfsInformation = NTFSInformation.getInstance();

        readProgressBar = new DetailedProgressBar();
        readProgressBar.setPercentageLabelPrefix("Scanning drive " + ntfsInformation.getRoot().toString().substring(4));
        readProgressBar.setProgressLabelSuffix("files");
        add(readProgressBar);
        add(Box.createVerticalStrut(10));

        foundFilesLabel = new JLabel("0 deleted files found.");
        add(foundFilesLabel);
        add(Box.createVerticalStrut(10));
    }

    private void addProcessFeedbackUI() {
        processProgressBar = new DetailedProgressBar();
        processProgressBar.setPercentageLabelPrefix("Processing deleted records:");
        processProgressBar.setProgressLabelSuffix("files");
        add(processProgressBar);
        add(Box.createVerticalStrut(10));
    }

    private void updateInterface() {
        updateReadInterface();
        updateProcessInterface();

        if(!isReading && updateQueue.isEmpty()) onProcessingEnd();
    }

    private void updateReadInterface() {
        readProgressBar.updateInformationLabels();
        foundFilesLabel.setText(deletedFilesFound + " deleted files found.");
    }

    private void updateProcessInterface() {
        processProgressBar.updateInformationLabels();
        processProgressBar.setValue(deletedFilesProcessed);
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
        updateProcessInterface();
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
