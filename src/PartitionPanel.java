import javax.swing.*;
import java.awt.*;

public class PartitionPanel extends StepPanel {

    private final SpringLayout springLayout = new SpringLayout();

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

        SelectDirectoryComponent selectDirectoryComponent = new SelectDirectoryComponent();
        springLayout.putConstraint(SpringLayout.NORTH, selectDirectoryComponent, 3, SpringLayout.SOUTH, outputLocationLabel);
        springLayout.putConstraint(SpringLayout.WEST, selectDirectoryComponent, 0, SpringLayout.WEST, outputLocationLabel);
        springLayout.putConstraint(SpringLayout.EAST, selectDirectoryComponent, -10, SpringLayout.EAST, this);
        add(selectDirectoryComponent);

        JLabel partitionSelectionLabel = new JLabel("Select Partition:");
        partitionSelectionLabel.setFont(headerFont);
        add(partitionSelectionLabel);
        springLayout.putConstraint(SpringLayout.NORTH, partitionSelectionLabel, 3, SpringLayout.SOUTH, selectDirectoryComponent);
        springLayout.putConstraint(SpringLayout.WEST, partitionSelectionLabel, 0, SpringLayout.WEST, outputLocationLabel);

        PartitionSelectionComponent partitionSelectionComponent = new PartitionSelectionComponent();
        springLayout.putConstraint(SpringLayout.NORTH, partitionSelectionComponent, 3, SpringLayout.SOUTH, partitionSelectionLabel);
        springLayout.putConstraint(SpringLayout.WEST, partitionSelectionComponent, 0, SpringLayout.WEST, outputLocationLabel);
        springLayout.putConstraint(SpringLayout.EAST, partitionSelectionComponent, -10, SpringLayout.EAST, this);
        add(partitionSelectionComponent);
    }
}
