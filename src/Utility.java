import java.io.*;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class Utility {

    public static Pair<String, Long> getUnitAndUnitSize(long bytes) {
        String[] storageUnits = {" B", " KB", " MB", " GB", " TB", " PB", " EB"};
        long[] bytesPerUnit = {0, 1024, 1048576, 1073741824, 1099511627776L, 1125899906842624L, 1152921504606846976L};

        int i = 0;
        while(bytes >= bytesPerUnit[i]) {
            i++;
            if(i + 1 > bytesPerUnit.length) break;
        }

        return new Pair<>(storageUnits[i-1], bytesPerUnit[i-1]);
    }

    public static int byteArrayToUnsignedInt(byte[] bytes, boolean isReversed) {
        if(isReversed) {
            for(int i = 0; i < bytes.length / 2; i++) {
                byte b = bytes[i];
                bytes[i] = bytes[bytes.length - i - 1];
                bytes[bytes.length - i - 1] = b;
            }
        }
        return new BigInteger(1, bytes).intValueExact();
    }

    public static long byteArrayToUnsignedLong(byte[] bytes, boolean isReversed) {
        if (isReversed) {
            for (int i = 0; i < bytes.length / 2; i++) {
                byte b = bytes[i];
                bytes[i] = bytes[bytes.length - i - 1];
                bytes[bytes.length - i - 1] = b;
            }
        }
        return new BigInteger(1, bytes).longValue();
    }

    public static byte[] readCluster(FileChannel diskChannel, long clusterOffsetBytes) throws IOException {
        FAT32Information fat32Information = FAT32Information.getInstance();
        byte[] fat32Record = new byte[fat32Information.getSectorsPerCluster() * fat32Information.getBytesPerSector()];
        ByteBuffer buffer = ByteBuffer.wrap(fat32Record);

        diskChannel.position(clusterOffsetBytes);
        diskChannel.read(buffer);

        return fat32Record;
    }
}