import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;

public class FAT32ScanPanel extends ScanPanel{

    private final ArrayList<Integer> directoryStartClusters = new ArrayList<>();
    private final ArrayList<Integer> checkedDirectoryStartClusters = new ArrayList<>();
    private int recordsInCluster;
    private File root;

    protected FAT32ScanPanel() {
        super();
        setReadRunnable(FAT32Scanner);

        FAT32Information fat32Information = FAT32Information.getInstance();
        readProgressBar.setPercentageLabelPrefix("Scanning drive " + fat32Information.getRoot().toString().substring(4));
    }

    private final Runnable FAT32Scanner = () -> {
        isReading = true;
        try {
            FAT32Information fat32Information = FAT32Information.getInstance();
            recordsInCluster = (fat32Information.sectorsPerCluster * fat32Information.bytesPerSector) / 32;
            root = fat32Information.getRoot();

            scanRootForDeletedFiles();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    };

    private final int[] LONG_NAME_CHAR_OFFSETS = new int[]{0x01, 0x03, 0x05, 0x07, 0x09, 0x0E, 0x10, 0x12, 0x14, 0x16, 0x18, 0x1C, 0x1E};
    private int fatByteOffset;
    private int bytesPerCluster;
    private int dataOffsetBytes;

    private final byte[] EMPTY_RECORD = new byte[32];

    private void scanRootForDeletedFiles() throws IOException {
        FAT32Information fat32Information = FAT32Information.getInstance();

        fatByteOffset = fat32Information.reservedSectors * fat32Information.bytesPerSector;
        bytesPerCluster = fat32Information.bytesPerSector * fat32Information.sectorsPerCluster;
        dataOffsetBytes = fat32Information.bytesPerSector * fat32Information.dataStartSector;

        Arrays.fill(EMPTY_RECORD, (byte)0);

        int rootDirectoryStartCluster = fat32Information.rootDirectoryStartCluster;
        directoryStartClusters.add(rootDirectoryStartCluster);

        while(!directoryStartClusters.isEmpty()) {
            readDirectory(directoryStartClusters.get(0));
        }
        isReading = false;
    }

    private static final long MAX_CLUSTER_VALUE = 0x0FFFFFEF;

    private void readDirectory(int directoryStartCluster) throws IOException {
        ArrayList<Integer> thisDirectoriesStartClusters = new ArrayList<>();

        RandomAccessFile diskAccess = new RandomAccessFile(root, "r");
        FileChannel diskChannel = diskAccess.getChannel();
        int nextCluster = directoryStartCluster;

        while(nextCluster <= MAX_CLUSTER_VALUE) {
            thisDirectoriesStartClusters.add(nextCluster);

            int internalFatByteOffset = nextCluster * 4;
            long fatClusterTarget = internalFatByteOffset / bytesPerCluster;
            internalFatByteOffset -= fatClusterTarget * bytesPerCluster;

            byte[] thisCluster = Utility.readCluster(diskChannel, fatByteOffset + (fatClusterTarget * bytesPerCluster));
            nextCluster = Utility.byteArrayToUnsignedInt(Arrays.copyOfRange(thisCluster, internalFatByteOffset, internalFatByteOffset + 4), true);
        }

        byte[][] thisDirectoriesData = new byte[thisDirectoriesStartClusters.size()][bytesPerCluster];

        for(int i = 0; i < thisDirectoriesStartClusters.size(); i++) {
            byte[] thisCluster = Utility.readCluster(diskChannel, dataOffsetBytes + (long) (thisDirectoriesStartClusters.get(i) - 2) * bytesPerCluster);
            thisDirectoriesData[i] = thisCluster;
        }
        diskAccess.close();

        parseDirectory(thisDirectoriesData);
        directoryStartClusters.remove(0);
    }

    private void parseDirectory(byte[][] thisDirectoriesData) {
        for(byte[] cluster : thisDirectoriesData) {
            try {
                readDirectoryCluster(cluster);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private int totalFilesFound = 0;
    private void incrementTotalFilesFound() {
        totalFilesFound ++;
        readProgressBar.setMaximum(totalFilesFound);
        readProgressBar.setValue(readProgressBar.getMaximum());
    }

    private void readDirectoryCluster(byte[] cluster) throws IOException {
        StringBuilder accumulatedName = new StringBuilder();

        int internalEntryOffset = 0;
        for(int x = 0; x < recordsInCluster; x++) {
            byte[] thisRecord = Arrays.copyOfRange(cluster, internalEntryOffset, internalEntryOffset+32);
            if(Arrays.equals(thisRecord, EMPTY_RECORD)) {
                continue;
            }

            byte attribute = thisRecord[0x0B];

            if((attribute & 0x10) == 0x10) {
                byte[] startClusterBytes = new byte[]{thisRecord[0x15], thisRecord[0x14], thisRecord[0x1B], thisRecord[0x1A]};
                int startCluster = Utility.byteArrayToUnsignedInt(startClusterBytes, false);
                if(!checkedDirectoryStartClusters.contains(startCluster)) {
                    incrementTotalFilesFound();
                    directoryStartClusters.add(startCluster);
                    checkedDirectoryStartClusters.add(startCluster);
                }
                accumulatedName = new StringBuilder();
                if(isLogging) {
                    scanLogPanel.log("Found directory");
                }
            } else if(attribute == 0x0F) {
                StringBuilder thisRecordNameExtension = new StringBuilder();
                for(int charOffset : LONG_NAME_CHAR_OFFSETS) {
                    int thisChar = Utility.byteArrayToUnsignedInt(Arrays.copyOfRange(thisRecord, charOffset, charOffset+2), true);
                    if(thisChar == 0x0) break;
                    thisRecordNameExtension.append((char) thisChar);
                }
                accumulatedName.insert(0, thisRecordNameExtension);
                if(isLogging) {
                    scanLogPanel.log("Found extended name");
                }
            } else {
                incrementTotalFilesFound();
                if(thisRecord[0] == (byte)0xE5) {
                    deletedFilesFound++;
                    if(!accumulatedName.toString().equals("")) {
                        FAT32Record fat32Record = new FAT32Record(thisRecord, accumulatedName.toString());
                        deletedRecords.add(fat32Record);
                        addRecordToUpdateQueue(fat32Record);
                        accumulatedName = new StringBuilder();
                        if(isLogging) {
                            scanLogPanel.log("Found deleted file with extended name " + fat32Record.fileName);
                        }
                    } else {
                        FAT32Record fat32Record = new FAT32Record(thisRecord);
                        deletedRecords.add(fat32Record);
                        addRecordToUpdateQueue(fat32Record);
                        if(isLogging) {
                            scanLogPanel.log("Found deleted file " + fat32Record.fileName);
                        }
                    }
                }
            }
            internalEntryOffset += 32;
        }
    }
}
