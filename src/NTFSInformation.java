import java.io.File;

public class NTFSInformation extends DriveInformation {
    private static NTFSInformation instance = null;

    public static NTFSInformation createInstance(File root) {
        if(instance == null) instance = new NTFSInformation(root);
        return instance;
    }

    public static NTFSInformation getInstance() {
        return instance;
    }

    public long getTotalSectors() {
        byte[] totalSectorsField = new byte[8];
        System.arraycopy(bootSector, 0x28, totalSectorsField, 0, 8);
        return Utility.byteArrayToUnsignedLong(totalSectorsField, true);
    }

    public int getMFTRecordLength() {
        if(bootSector[0x40] >= 0){
            return (bootSector[0x40] & 0xFF) * getBytesPerCluster();
        }
        return (int) Math.pow(2.0, Math.abs(bootSector[0x40]));
    }

    private long getMFTClusterLocation() {
        byte[] MFTClusterLocationField = new byte[8];
        System.arraycopy(bootSector, 0x30, MFTClusterLocationField, 0, 8);
        return Utility.byteArrayToUnsignedLong(MFTClusterLocationField, true);
    }

    public long getMFTByteLocation() {
        return getMFTClusterLocation() * getBytesPerCluster();
    }

    private NTFSInformation(File root) {
        this.root = root;
        parseBootSector();
    }

    @Override
    protected void parseBootSector(){
        bootSector = readBootSector(root);
        bytesPerSector = ((bootSector[0x0C] & 0xff) << 8) | (bootSector[0x0B] & 0xff);
        sectorsPerCluster = bootSector[0x0D];
    }
}
