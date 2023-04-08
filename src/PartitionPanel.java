import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.io.File;

public class PartitionPanel extends StepPanel {

    private boolean hasPartitionSelected = false;
    private boolean hasOutputLocationSelected = false;

    private int errorMessagePanelIndex = 0;
    private String partitionType = "";

    @Override
    public void onNextStep() {
        BottomPanel.setNextButtonEnabled(false);
        BottomPanel.setBackButtonEnabled(true);

        if(partitionType.equals("NTFS")) {
            NTFSScanPanel scanPanel = new NTFSScanPanel();
            Frame.setStepPanel(scanPanel);
        } else if (partitionType.equals("FAT32")) {
            FAT32ScanPanel scanPanel = new FAT32ScanPanel();
            Frame.setStepPanel(scanPanel);
        }
    }

    @Override
    public void onBackStep() {

    }

    public PartitionPanel() {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        Font headerFont = new Font("Arial", Font.BOLD, 17);
        Font textFont = new Font("Arial", Font.PLAIN, 14);

        JLabel introductoryInformationHeader = new JLabel("Introductory Information");
        introductoryInformationHeader.setFont(headerFont);
        introductoryInformationHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(introductoryInformationHeader);

        JLabel introductoryInformation = new JLabel("<html>This software will guide you through the process of recovering deleted files from NTFS and FAT drives. Not all deleted files are recoverable, and the longer it has been since the file was deleted, the less likely that the file is recoverable.</html>");
        introductoryInformation.setFont(textFont);
        introductoryInformation.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(introductoryInformation);

        add(Box.createVerticalStrut(10));

        JLabel recoveryTips = new JLabel("<html><p>Here are some ways you can improve the chance to recover deleted files from a drive:</p><ul style='margin-top:0px;'><li>Stop using the drive immediately, including saving, downloading, creating, accessing, or editing.</li><li>Try multiple recovery programs if this one doesn't work. Remember to not install or run them on the target drive.</li><li>Create backups of valuable data frequently in the future.</li><li>Seek professional data recovery services.</li></ul></html>");
        recoveryTips.setFont(textFont);
        recoveryTips.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(recoveryTips);

        add(Box.createVerticalStrut(10));

        JLabel outputLocationLabel = new JLabel("Step 1: Select Output Location:");
        outputLocationLabel.setFont(headerFont);
        outputLocationLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(outputLocationLabel);

        SelectDirectoryComponent selectDirectoryComponent = new SelectDirectoryComponent(this);
        selectDirectoryComponent.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(selectDirectoryComponent);
        add(Box.createVerticalStrut(10));
        add(Box.createVerticalStrut(10));

        JLabel partitionSelectionLabel = new JLabel("Step 2: Select Partition:");
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
    public void notifyPartitionSelected(File f, String partitionType) {
        partition = f;
        this.partitionType = partitionType;
        hasPartitionSelected = true;
        checkNextStepAllowed();
    }

    private static File output;
    public static File getOutput() {
        return output;
    }
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

            JLabel errorMessage = new JLabel("<html>The output location cannot be on the same partition that you are recovering files from.<br>This is because recovered files will overwrite the data of files which have not yet been recovered, making them irrecoverable. Consider using a USB Flash Drive.</html>");
            errorMessage.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));

            errorMessagePanel.add(Box.createHorizontalStrut(4));
            errorMessagePanel.add(exclamationMark);
            errorMessagePanel.add(Box.createHorizontalStrut(4));
            errorMessagePanel.add(errorMessage);
            errorMessagePanel.add(Box.createHorizontalStrut(4));

            int componentHeight = (int) errorMessage.getPreferredSize().getHeight();
            errorMessagePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, componentHeight));
            exclamationMark.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 48));

            errorMessagePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            add(errorMessagePanel, errorMessagePanelIndex);
        }
    }
}