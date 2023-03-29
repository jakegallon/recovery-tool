import javax.swing.*;
import java.awt.*;
import java.io.File;

public class PartitionPanel extends StepPanel {

    private final SpringLayout springLayout = new SpringLayout();
    private static boolean hasPartitionSelected = false;
    private boolean hasOutputLocationSelected = false;

    @Override
    public void onNextStep() {
        ScanPanel scanPanel = new ScanPanel();
        Frame.setStepPanel(scanPanel);

        BottomPanel.setNextButtonEnabled(false);
        BottomPanel.setBackButtonEnabled(true);
    }

    @Override
    public void onBackStep() {

    }

    public PartitionPanel() {
        setLayout(springLayout);
        Font headerFont = new Font("Arial", Font.PLAIN, 16);

        JLabel outputLocationLabel = new JLabel("Select Output Location:");
        outputLocationLabel.setFont(headerFont);
        add(outputLocationLabel);
        springLayout.putConstraint(SpringLayout.NORTH, outputLocationLabel, 10, SpringLayout.NORTH, this);
        springLayout.putConstraint(SpringLayout.WEST, outputLocationLabel, 10, SpringLayout.WEST, this);

        SelectDirectoryComponent selectDirectoryComponent = new SelectDirectoryComponent(this);
        springLayout.putConstraint(SpringLayout.NORTH, selectDirectoryComponent, 3, SpringLayout.SOUTH, outputLocationLabel);
        springLayout.putConstraint(SpringLayout.WEST, selectDirectoryComponent, 0, SpringLayout.WEST, outputLocationLabel);
        springLayout.putConstraint(SpringLayout.EAST, selectDirectoryComponent, -10, SpringLayout.EAST, this);
        add(selectDirectoryComponent);

        JLabel partitionSelectionLabel = new JLabel("Select Partition:");
        partitionSelectionLabel.setFont(headerFont);
        add(partitionSelectionLabel);
        springLayout.putConstraint(SpringLayout.NORTH, partitionSelectionLabel, 3, SpringLayout.SOUTH, selectDirectoryComponent);
        springLayout.putConstraint(SpringLayout.WEST, partitionSelectionLabel, 0, SpringLayout.WEST, outputLocationLabel);

        PartitionSelectionComponent partitionSelectionComponent = new PartitionSelectionComponent(this);
        springLayout.putConstraint(SpringLayout.NORTH, partitionSelectionComponent, 3, SpringLayout.SOUTH, partitionSelectionLabel);
        springLayout.putConstraint(SpringLayout.WEST, partitionSelectionComponent, 0, SpringLayout.WEST, outputLocationLabel);
        springLayout.putConstraint(SpringLayout.EAST, partitionSelectionComponent, -10, SpringLayout.EAST, this);
        add(partitionSelectionComponent);
    }

    private File partition;
    public void notifyPartitionSelected(File f) {
        partition = f;
        hasPartitionSelected = true;
        checkNextStepAllowed();
    }

    private File output;
    public void notifyOutputLocationSelected(File f) {
        output = f;
        hasOutputLocationSelected = true;
        checkNextStepAllowed();
    }

    private void checkNextStepAllowed() {
        if(!(hasPartitionSelected && hasOutputLocationSelected)) return;

        String outputPath = output.getAbsolutePath();
        String outputLetter = outputPath.substring(0, 1);

        String partitionPath = partition.getAbsolutePath();
        String partitionLetter = partitionPath.substring(4, 5);

        if(!outputLetter.equals(partitionLetter)) {
            BottomPanel.setNextButtonEnabled(true);
        } else {
            BottomPanel.setNextButtonEnabled(false);
        }
    }
}
