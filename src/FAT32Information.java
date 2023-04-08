import java.io.File;
import java.util.Arrays;

public class FAT32Information {
    private static FAT32Information instance = null;

    public static FAT32Information createInstance(File root) {
        instance = new FAT32Information(root);
        return instance;
    }

    public static FAT32Information getInstance() {
        return instance;
    }

    private final File root;

    private FAT32Information(File root) {
        this.root = root;
        readBootSector();
    }

    int bytesPerSector;
    int sectorsPerCluster;
    int reservedSectors;
    int fatCount;
    int sectorsPerFat;
    int dataStartSector;

    private void readBootSector(){
        byte[] bootSector = Utility.readBootSector(root);

        bytesPerSector = Utility.byteArrayToUnsignedInt(Arrays.copyOfRange(bootSector, 0x0B, 0x0D), true);
        sectorsPerCluster = bootSector[0x0D];

        reservedSectors = Utility.byteArrayToUnsignedInt(Arrays.copyOfRange(bootSector, 0x0E, 0x10), true);
        fatCount = bootSector[0x10];
        sectorsPerFat = Utility.byteArrayToUnsignedInt(Arrays.copyOfRange(bootSector, 0x24, 0x28), true);

        dataStartSector = (reservedSectors+(fatCount * sectorsPerFat));
    }

    public File getRoot() {
        return root;
    }
}
