import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.prefs.Preferences;

public class ScanPanel extends StepPanel {

    protected JLabel foundFilesLabel;

    protected boolean isReading = false;
    protected DetailedProgressBar readProgressBar = new DetailedProgressBar();
    protected int deletedFilesFound = 0;

    protected volatile boolean isProcessing = false;
    protected DetailedProgressBar processProgressBar = new DetailedProgressBar();
    protected int deletedFilesProcessed = 0;

    private final Timer scanTimer;
    protected final ConcurrentLinkedQueue<GenericRecord> updateQueue = new ConcurrentLinkedQueue<>();

    protected final ArrayList<GenericRecord> deletedRecords = new ArrayList<>();

    protected boolean isLogging;
    protected final LogPanel scanLogPanel = new LogPanel();
    private final LogPanel processLogPanel = new LogPanel();

    private Thread readThread;
    private boolean isInterrupted = false;

    protected void setReadRunnable(Runnable runnable) {
        readThread = new Thread(runnable);
        readThread.start();
    }

    protected ScanPanel(ArrayList<GenericRecord> deletedRecords) {
        this();
        this.deletedRecords.addAll(deletedRecords);

        int size = deletedRecords.size();
        deletedFilesFound = size;
        deletedFilesProcessed = size;

        readProgressBar.setMaximum(100);
        readProgressBar.setValue(100);
        processProgressBar.setMaximum(size);
        processProgressBar.setValue(size);

        foundFilesLabel.setText(size + " deleted files found.");

        scanLogPanel.log("Restored " + size + " deleted files from previous scan.");
        processLogPanel.log("Restored " + size + " deleted files from previous scan.");
        scanLogPanel.log("Press next to continue to filter, or back to scan a different partition.");
        processLogPanel.log("Press next to continue to filter, or back to scan a different partition.");

        onProcessingEnd();
    }

    protected ScanPanel() {
        Preferences prefs = Preferences.userNodeForPackage(PartitionPanel.class);
        isLogging = prefs.getBoolean("IS_LOGGING", false);

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
        FilterPanel filterPanel = new FilterPanel(deletedRecords);
        Frame.setStepPanel(filterPanel);

        BottomPanel.setNextButtonEnabled(false);
        BottomPanel.setBackButtonEnabled(true);
    }

    @Override
    public void onBackStep() {
        isReading = false;
        isInterrupted = true;
        if(readThread != null) {
            readThread.interrupt();
        }

        PartitionPanel partitionPanel = new PartitionPanel();
        Frame.setStepPanel(partitionPanel);

        BottomPanel.setNextButtonEnabled(false);
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
        if(deletedRecords.isEmpty() && !isInterrupted) {
            processLogPanel.log("No deleted records were found on this drive.", "<font color='#FF382E'>");
            processLogPanel.log("Press \"Back\" to scan a different drive, or press \"Exit\" to exit the program.", "<font color='#FF382E'>");
            BottomPanel.onIsFinished();
        }

        scanTimer.stop();
        scanLogPanel.shutdown();
        processLogPanel.shutdown();
        updateProcessInterface();
        BottomPanel.setNextButtonEnabled(true);
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

    public synchronized void addRecordToUpdateQueue(GenericRecord genericRecord) {
        updateQueue.add(genericRecord);
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

    private void process(GenericRecord genericRecord) {
        genericRecord.process();
        deletedFilesProcessed ++;
        if(isLogging) {
            processLogPanel.log("Successfully processed " + genericRecord.fileName);
        }
    }
}
