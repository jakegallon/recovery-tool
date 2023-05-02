import java.util.Arrays;

public class FAT32Information extends DriveInformation {
    private static FAT32Information instance = null;

    public static FAT32Information getInstance() {
        if(instance == null) instance = new FAT32Information();
        return instance;
    }

    private FAT32Information() {
        parseBootSector();
    }

    public int getSectorsPerFat() {
        return Utility.byteArrayToUnsignedInt(Arrays.copyOfRange(bootSector, 0x24, 0x28), true);
    }

    public int getFatCount() {
        return bootSector[0x10];
    }

    public int getReservedSectors() {
        return Utility.byteArrayToUnsignedInt(Arrays.copyOfRange(bootSector, 0x0E, 0x10), true);
    }

    public int getDataStartSector() {
        return getReservedSectors() + (getFatCount() * getSectorsPerFat());
    }

    public int getRootDirectoryStartCluster() {
        return Utility.byteArrayToUnsignedInt(Arrays.copyOfRange(bootSector, 0x2C, 0x30), true);
    }

    @Override
    protected void parseBootSector() {
        bootSector = readBootSector(getRoot());
        bytesPerSector = Utility.byteArrayToUnsignedInt(Arrays.copyOfRange(bootSector, 0x0B, 0x0D), true);
        sectorsPerCluster = bootSector[0x0D];
    }
}
