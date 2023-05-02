import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public abstract class DriveInformation {
    protected byte[] bootSector;
    protected File root;

    public File getRoot() {
        return root;
    }

    protected int bytesPerSector;
    protected int sectorsPerCluster;

    protected abstract void parseBootSector();

    protected byte[] readBootSector(File root) {
        RandomAccessFile diskAccess;
        try {
            diskAccess = new RandomAccessFile(root, "r");
            byte[] content = new byte[512];
            diskAccess.readFully(content);
            diskAccess.close();
            return content;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public int getBytesPerSector() {
        return bytesPerSector;
    }

    public int getBytesPerCluster() {
        return bytesPerSector * sectorsPerCluster;
    }

    public int getSectorsPerCluster() {
        return sectorsPerCluster;
    }
}
