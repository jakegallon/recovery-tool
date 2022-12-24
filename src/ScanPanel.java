import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

public class ScanPanel extends StepPanel {

    private final String assignedLetter;

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

    public ScanPanel(String assignedLetter) {
        this.assignedLetter = assignedLetter;

        printFirstKilobyte();
    }

    private void printFirstKilobyte(){
        File diskRoot = new File ("\\\\.\\" + assignedLetter +":");
        RandomAccessFile diskAccess;
        try {
            diskAccess = new RandomAccessFile(diskRoot, "r");
            byte[] content = new byte[1024];
            diskAccess.readFully(content);
            diskAccess.close();

            System.out.println(Arrays.toString(content));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
