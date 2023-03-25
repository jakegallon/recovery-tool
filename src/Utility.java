import java.io.*;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;

public class Utility {

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

    public static byte[] readMFTRecord(FileChannel diskChannel, long offset) throws IOException {
        NTFSInformation ntfsInformation = NTFSInformation.getInstance();
        byte[] mftRecord = new byte[ntfsInformation.getMFTRecordLength()];
        ByteBuffer buffer = ByteBuffer.wrap(mftRecord);

        diskChannel.position(offset);
        diskChannel.read(buffer);

        if(!Arrays.equals(Arrays.copyOf(mftRecord, 4), new byte[]{0x46, 0x49, 0x4C, 0x45})) {
            return null;
        }
        return mftRecord;
    }
}