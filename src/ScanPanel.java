import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.prefs.Preferences;

public class ScanPanel extends StepPanel {

    protected JLabel foundFilesLabel;

    protected static boolean isReading = false;
    protected static DetailedProgressBar readProgressBar = new DetailedProgressBar();
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

    protected static boolean isLogging;
    protected static final LogPanel scanLogPanel = new LogPanel();
    private static final LogPanel processLogPanel = new LogPanel();

    protected ScanPanel(Runnable readMethod) {
        Preferences prefs = Preferences.userNodeForPackage(PartitionPanel.class);
        isLogging = prefs.getBoolean("IS_LOGGING", false);

        Thread readThread = new Thread(readMethod);
        readThread.start();

        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        Font headerFont = new Font("Arial", Font.BOLD, 17);
        Font textFont = new Font("Arial", Font.PLAIN, 14);

        add(Box.createVerticalStrut(10));

        JLabel waitLabel = new JLabel("Step 3: Wait");
        waitLabel.setFont(headerFont);
        waitLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(waitLabel);

        JLabel waitText = new JLabel("<html>The software is now scanning your drive. This may take up to a minute depending on how big the drive is. Refer to the progress bars below to track the progress of the scan.</html>");
        waitText.setFont(textFont);
        waitText.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(waitText);

        add(Box.createVerticalStrut(10));

        addReadFeedbackUI();

        JLabel scanLogPanelLabel = new JLabel("Log:");
        scanLogPanelLabel.setFont(textFont);
        scanLogPanelLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(scanLogPanelLabel);

        add(scanLogPanel);
        scanLogPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        add(Box.createVerticalStrut(10));

        addProcessFeedbackUI();

        JLabel processLogPanelLabel = new JLabel("Log:");
        processLogPanelLabel.setFont(textFont);
        processLogPanelLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(processLogPanelLabel);

        add(processLogPanel);
        processLogPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        add(Box.createVerticalStrut(10));

        scanTimer = new Timer(200, e -> updateInterface());
        scanTimer.start();

        if(!isLogging) {
            scanLogPanel.log("Logging is not currently enabled.");
            processLogPanel.log("Logging is not currently enabled.");
        }
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
        scanLogPanel.shutdown();
        processLogPanel.shutdown();
        updateProcessInterface();
        BottomPanel.setNextButtonEnabled(true);

        if(deletedRecords.isEmpty()) {
            BottomPanel.onIsFinished();
        }
    }

    private void addReadFeedbackUI() {
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
        if(isLogging) {
            processLogPanel.log("Successfully processed " + genericRecord.fileName);
        }
    }
}
