import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ScanPanel extends StepPanel {

    protected JLabel foundFilesLabel;

    protected static boolean isReading = false;
    protected static DetailedProgressBar readProgressBar;
    protected static int deletedFilesFound = 0;

    protected static volatile boolean isProcessing = false;
    protected static DetailedProgressBar processProgressBar = new DetailedProgressBar();
    protected static int deletedFilesProcessed = 0;

    private final Timer scanTimer;
    protected static final ConcurrentLinkedQueue<GenericRecord> updateQueue = new ConcurrentLinkedQueue<>();

    protected static final ArrayList<GenericRecord> deletedRecords = new ArrayList<>();
    public static ArrayList<GenericRecord> getDeletedRecords() {
        return deletedRecords;
    }

    protected ScanPanel(Runnable readMethod) {
        Thread readThread = new Thread(readMethod);
        readThread.start();

        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        addReadFeedbackUI();
        addProcessFeedbackUI();

        scanTimer = new Timer(200, e -> updateInterface());
        scanTimer.start();
    }

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

    protected void updateInterface() {
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

    private void onProcessingEnd() {
        scanTimer.stop();
        updateProcessInterface();
        BottomPanel.setNextButtonEnabled(true);
    }

    private void addReadFeedbackUI() {
        readProgressBar = new DetailedProgressBar();
        readProgressBar.setProgressLabelSuffix("files");
        add(readProgressBar);
        add(Box.createVerticalStrut(10));

        foundFilesLabel = new JLabel("0 deleted files found.");
        add(foundFilesLabel);
        add(Box.createVerticalStrut(10));
    }

    private void addProcessFeedbackUI() {
        processProgressBar.setPercentageLabelPrefix("Processing deleted records:");
        processProgressBar.setProgressLabelSuffix("files");
        add(processProgressBar);
        add(Box.createVerticalStrut(10));
    }

    public synchronized static void addRecordToUpdateQueue(GenericRecord genericRecord) {
        updateQueue.add(genericRecord);
        if (!isProcessing) {
            isProcessing = true;
            Thread processingThread = new Thread(ScanPanel::processQueue);
            processingThread.setDaemon(true);
            processingThread.start();
        }
        processProgressBar.setMaximum(deletedFilesFound);
    }

    private static void processQueue() {
        while (!updateQueue.isEmpty()) {
            process(updateQueue.poll());
        }
        isProcessing = false;
    }

    private static void process(GenericRecord genericRecord) {
        genericRecord.process();
        deletedFilesProcessed ++;
    }
}
