import java.math.BigInteger;

public class Utility {

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
}