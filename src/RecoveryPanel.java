import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

public class RecoveryPanel extends StepPanel {

    DetailedProgressBar recoveryProgressBar = new DetailedProgressBar();

    @Override
    public void onNextStep() {

    }

    @Override
    public void onBackStep() {

    }

    private final ArrayList<MFTRecord> mftRecords;
    public RecoveryPanel(ArrayList<MFTRecord> mftRecords) {
        this.mftRecords = mftRecords;

        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        initializeRecoveryProgressBar();
        add(recoveryProgressBar);

        recoverFiles();
    }

    private void initializeRecoveryProgressBar() {
        recoveryProgressBar.setPercentageLabelPrefix("Recovering Deleted Files:");
        recoveryProgressBar.setProgressLabelSuffix("files");
        recoveryProgressBar.setMaximum(mftRecords.size());
        recoveryProgressBar.updateInformationLabels();
    }

    private void recoverFiles() {
        for(MFTRecord mftRecord : mftRecords) {
            mftRecord.parseDataAttribute();
            if(mftRecord.isDataResident()) {
                recoverResidentFile(mftRecord);
            } else {
                recoverNonResidentFile(mftRecord);
            }
        }
    }

    private void recoverResidentFile(MFTRecord mftRecord) {
        byte[] fullBytes = mftRecord.getAttribute(Attribute.DATA);
        byte[] dataBytes = Arrays.copyOfRange(fullBytes, 0x18, 0x18 + (int) mftRecord.getFileSizeBytes());
        String fileName = mftRecord.getFileName();

        try {
            File newFile = new File(PartitionPanel.getOutput(), fileName);
            FileOutputStream fos = new FileOutputStream(newFile);
            fos.write(dataBytes);
            fos.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void recoverNonResidentFile(MFTRecord mftRecord) {
    }
}
