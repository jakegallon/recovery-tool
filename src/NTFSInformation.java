import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;

public class NTFSInformation {
    private static NTFSInformation instance = null;

    public static NTFSInformation createInstance(File root) {
        instance = new NTFSInformation(root);
        return instance;
    }

    public static NTFSInformation getInstance() {
        return instance;
    }

    private final File root;

    private long totalSectors;
    private int bytesPerSector;
    private int bytesPerCluster;

    private long MFTClusterLocation;
    private int MFTRecordLength;

    private long MFTSizeBytes;

    private NTFSInformation(File root) {
        this.root = root;

        readBootSector();

        try {
            getMFTInformation();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void readBootSector(){
        byte[] bootSector = Utility.readBootSector(root);

        bytesPerSector = ((bootSector[0x0C] & 0xff) << 8) | (bootSector[0x0B] & 0xff);
        bytesPerCluster = bootSector[0x0D] * bytesPerSector;

        byte[] totalSectorsField = new byte[8];
        System.arraycopy(bootSector, 0x28, totalSectorsField, 0, 8);
        totalSectors = Utility.byteArrayToLong(totalSectorsField, true);

        byte[] MFTClusterLocationField = new byte[8];
        System.arraycopy(bootSector, 0x30, MFTClusterLocationField, 0, 8);

        MFTClusterLocation = Utility.byteArrayToLong(MFTClusterLocationField, true);

        if(bootSector[0x40] >= 0) MFTRecordLength = (bootSector[0x40] & 0xFF) * bytesPerCluster;
        else MFTRecordLength = (int) Math.pow(2.0, Math.abs(bootSector[0x40]));
    }

    private void getMFTInformation() throws IOException {
        byte[] mftRecord = readFirstMFTRecord();
        MFTRecord mftInformation = new MFTRecord(mftRecord);
        MFTSizeBytes = mftInformation.getFileLengthBytes(bytesPerCluster);
    }

    private byte[] readFirstMFTRecord() throws IOException {
        byte[] mftRecord = new byte[MFTRecordLength];

        RandomAccessFile diskAccess = new RandomAccessFile(root, "r");
        FileChannel diskChannel = diskAccess.getChannel();

        ByteBuffer buffer = ByteBuffer.wrap(mftRecord);

        diskChannel.position(getMFTByteLocation());
        diskChannel.read(buffer);

        diskAccess.close();

        if(!Arrays.equals(Arrays.copyOf(mftRecord, 4), new byte[]{0x46, 0x49, 0x4C, 0x45})) {
            throw new IllegalArgumentException("MFT Record does not start with FILE");
        }
        return mftRecord;
    }


    public File getRoot() {
        return root;
    }

    public int getBytesPerSector() {
        return bytesPerSector;
    }

    public int getBytesPerCluster() {
        return bytesPerCluster;
    }

    public long getTotalSectors() {
        return totalSectors;
    }

    public long getMFTByteLocation() {
        return MFTClusterLocation * bytesPerCluster;
    }

    public int getMFTRecordLength() {
        return MFTRecordLength;
    }

    public long getMFTSizeBytes() {
        return MFTSizeBytes;
    }
}
