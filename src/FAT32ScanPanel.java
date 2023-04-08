import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;

public class FAT32ScanPanel extends ScanPanel{

    private static ArrayList<Long> directoryStartClusters = new ArrayList<>();

    protected FAT32ScanPanel() {
        super(FAT32Scanner);

        FAT32Information fat32Information = FAT32Information.getInstance();
        readProgressBar.setPercentageLabelPrefix("Scanning drive " + fat32Information.getRoot().toString().substring(4));
    }

    private static final Runnable FAT32Scanner = () -> {
        isReading = true;
        try {
            scanRootForDeletedFiles();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    };

    private static final int ENTRY_SIZE = 32;
    private static final int[] LONG_NAME_CHAR_OFFSETS = new int[]{0x01, 0x03, 0x05, 0x07, 0x09, 0x0E, 0x10, 0x12, 0x14, 0x16, 0x18, 0x1C, 0x1E};

    private static void scanRootForDeletedFiles() throws IOException {
        FAT32Information fat32Information = FAT32Information.getInstance();
        int rootDirectoryOffset = fat32Information.dataStartSector * fat32Information.bytesPerSector;

        int currentOffset = rootDirectoryOffset;
        int internalEntryOffset = 0;

        RandomAccessFile diskAccess = new RandomAccessFile(fat32Information.getRoot(), "r");
        FileChannel diskChannel = diskAccess.getChannel();

        byte[] thisCluster = Utility.readCluster(diskChannel, currentOffset + internalEntryOffset);
        int recordsInCluster = (fat32Information.sectorsPerCluster * fat32Information.bytesPerSector) / 32;

        StringBuilder accumulatedName = new StringBuilder();

        for(int x = 0; x < recordsInCluster; x++) {
            byte[] thisRecord = Arrays.copyOfRange(thisCluster, internalEntryOffset, internalEntryOffset+32);

            byte attribute = thisRecord[0x0B];

            if((attribute & 0x10) == 0x10) {
                byte[] startClusterBytes = new byte[]{thisRecord[0x15], thisRecord[0x14], thisRecord[0x1B], thisRecord[0x1A]};
                long startCluster = Utility.byteArrayToUnsignedLong(startClusterBytes, false);
                directoryStartClusters.add(startCluster);
                accumulatedName = new StringBuilder();
            } else if(attribute == 0x0F) {
                StringBuilder thisRecordNameExtension = new StringBuilder();
                for(int charOffset : LONG_NAME_CHAR_OFFSETS) {
                    int thisChar = Utility.byteArrayToUnsignedInt(Arrays.copyOfRange(thisRecord, charOffset, charOffset+2), true);
                    if(thisChar == 0x0) break;
                    thisRecordNameExtension.append((char) thisChar);
                }
                accumulatedName.insert(0, thisRecordNameExtension);
            } else {
                if(!accumulatedName.toString().equals("")) {
                    FAT32Record fat32Record = new FAT32Record(thisRecord, accumulatedName.toString());
                    deletedRecords.add(fat32Record);
                    accumulatedName = new StringBuilder();
                } else {
                    FAT32Record fat32Record = new FAT32Record(thisRecord);
                    deletedRecords.add(fat32Record);
                }
            }
            internalEntryOffset += 32;
        }
        diskAccess.close();
    }
}
