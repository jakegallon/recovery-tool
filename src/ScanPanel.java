public class ScanPanel extends StepPanel {

    @Override
    public void onNextStep() {

    }

    @Override
    public void onBackStep() {
        PartitionPanel partitionPanel = new PartitionPanel();
        Frame.setStepPanel(partitionPanel);

        BottomPanel.setNextButtonEnabled(true);
        BottomPanel.setBackButtonEnabled(false);
    }

    public ScanPanel() {

    }
}
