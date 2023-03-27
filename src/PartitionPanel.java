import javax.swing.*;

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

        PartitionSelectionComponent partitionSelectionComponent = new PartitionSelectionComponent();
        springLayout.putConstraint(SpringLayout.NORTH, partitionSelectionComponent, 0, SpringLayout.NORTH, this);
        springLayout.putConstraint(SpringLayout.WEST, partitionSelectionComponent, 10, SpringLayout.WEST, this);
        springLayout.putConstraint(SpringLayout.EAST, partitionSelectionComponent, -10, SpringLayout.EAST, this);
        add(partitionSelectionComponent);
    }
}
