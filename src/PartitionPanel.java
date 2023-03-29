import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.io.File;

public class PartitionPanel extends StepPanel {

    private static boolean hasPartitionSelected = false;
    private boolean hasOutputLocationSelected = false;

    private int errorMessagePanelIndex = 0;

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
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        Font headerFont = new Font("Arial", Font.PLAIN, 16);

        JLabel outputLocationLabel = new JLabel("Select Output Location:");
        outputLocationLabel.setFont(headerFont);
        outputLocationLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(outputLocationLabel);

        SelectDirectoryComponent selectDirectoryComponent = new SelectDirectoryComponent(this);
        selectDirectoryComponent.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(selectDirectoryComponent);
        add(Box.createVerticalStrut(5));
        add(Box.createVerticalStrut(5));

        JLabel partitionSelectionLabel = new JLabel("Select Partition:");
        partitionSelectionLabel.setFont(headerFont);
        partitionSelectionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(partitionSelectionLabel);

        PartitionSelectionComponent partitionSelectionComponent = new PartitionSelectionComponent(this);
        partitionSelectionComponent.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(partitionSelectionComponent);

        for (int i = 0; i < getComponents().length; i++) {
            if(getComponents()[i] == selectDirectoryComponent)
                errorMessagePanelIndex = i + 2;
        }
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
            if(errorMessagePanel != null) remove(errorMessagePanel);
            errorMessagePanel = null;
        } else {
            BottomPanel.setNextButtonEnabled(false);
            addErrorMessage();
        }
    }

    private JPanel errorMessagePanel;
    private void addErrorMessage() {
        if (errorMessagePanel != null) {
            add(errorMessagePanel, errorMessagePanelIndex);
        } else {
            errorMessagePanel = new JPanel();
            errorMessagePanel.setBorder(new LineBorder(Color.red, 1));
            BoxLayout boxLayout = new BoxLayout(errorMessagePanel, BoxLayout.X_AXIS);
            errorMessagePanel.setLayout(boxLayout);

            JLabel exclamationMark = new JLabel("!");
            exclamationMark.setForeground(Color.RED);

            JLabel errorMessage = new JLabel("<html>The output location cannot be on the same partition that you are recovering files from.<br>This is because recovering files can may overwrite the data being used to recover files which have not been recovered yet.</html>");
            errorMessage.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));

            errorMessagePanel.add(Box.createHorizontalStrut(4));
            errorMessagePanel.add(exclamationMark);
            errorMessagePanel.add(Box.createHorizontalStrut(4));
            errorMessagePanel.add(errorMessage);
            errorMessagePanel.add(Box.createHorizontalStrut(4));

            int componentHeight = (int) errorMessage.getPreferredSize().getHeight();
            errorMessagePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, componentHeight));
            exclamationMark.setFont(new Font(Font.SANS_SERIF, Font.BOLD, componentHeight+10));

            errorMessagePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            add(errorMessagePanel, errorMessagePanelIndex);
        }
    }
}