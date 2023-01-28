import java.io.File;

public class NTFSInformation {

    private final int bytesPerSector;
    private final int bytesPerCluster;

    private final long totalSectors;

    private final long MFTClusterLocation;
    private final int MFTRecordLength;

    public NTFSInformation(File root) {
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

    public int getBytesPerSector() {
        return bytesPerSector;
    }

    public int getBytesPerCluster() {
        return bytesPerCluster;
    }

    public long getTotalSectors() {
        return totalSectors;
    }

    public long getMFTClusterLocation() {
        return MFTClusterLocation * bytesPerCluster;
    }

    public int getMFTRecordLength() {
        return MFTRecordLength;
    }
}
