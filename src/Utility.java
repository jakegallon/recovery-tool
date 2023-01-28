import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;

public class Utility {

    public static int byteArrayToInt(byte[] bytes, boolean isReversed) {
        if(isReversed) {
            for(int i = 0; i < bytes.length / 2; i++) {
                byte b = bytes[i];
                bytes[i] = bytes[bytes.length - i - 1];
                bytes[bytes.length - i - 1] = b;
            }
        }
        return new BigInteger(bytes).intValueExact();
    }

    public static long byteArrayToLong(byte[] bytes, boolean isReversed) {
        if(isReversed) {
            for(int i = 0; i < bytes.length / 2; i++) {
                byte b = bytes[i];
                bytes[i] = bytes[bytes.length - i - 1];
                bytes[bytes.length - i - 1] = b;
            }
        }
        return new BigInteger(bytes).longValueExact();
    }

    public static byte[] readBootSector(File root) {
        RandomAccessFile diskAccess;
        try {
            diskAccess = new RandomAccessFile(root, "r");
            byte[] content = new byte[512]; //todo check for case where the sector size is not 512.
            diskAccess.readFully(content);
            diskAccess.close();
            return content;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}