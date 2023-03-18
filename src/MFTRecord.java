import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;

public class MFTRecord {

    private static final byte[] checksum = new byte[]{0x46, 0x49, 0x4C, 0x45};

    public boolean isDeleted() {
        return isDeleted;
    }

    private enum ATTRIBUTE {
        STANDARD_INFORMATION(0x10),
        ATTRIBUTE_LIST(0x20),
        FILE_NAME(0x30),
        OBJECT_ID(0x40),
        SECURITY_DESCRIPTOR(0x50),
        VOLUME_NAME(0x60),
        VOLUME_INFORMATION(0x70),
        DATA(0x80),
        INDEX_ROOT(0x90),
        INDEX_ALLOCATION(0xA0),
        BITMAP(0xB0),
        REPARSE_POINT(0xC0),
        EA_INFORMATION(0xD0),
        EA(0xE0),
        logged_utility_stream(0x100);

        private final int value;

        ATTRIBUTE(int hex) {
            value = hex;
        }
    }

    private final byte[] bytes;
    private final HashMap<ATTRIBUTE, Integer> attributeOffsets = new HashMap<>();

    private boolean isDeleted = false;

    public MFTRecord(byte[] bytes) {
        if(!Arrays.equals(Arrays.copyOf(bytes, 4), checksum)) {
            throw new IllegalArgumentException("Not an MFT Record - MFT Record does not start with FILE");
        }
        this.bytes = bytes;

        int allocatedFlag = Utility.byteArrayToInt(Arrays.copyOfRange(bytes, 0x16, 0x17), true);
        if(allocatedFlag == 0x0000 || allocatedFlag == 0x0200) {
            isDeleted = true;
        }
        int offset = Utility.byteArrayToInt(Arrays.copyOfRange(bytes, 0x14, 0x15), true);
        int recordSize = Utility.byteArrayToInt(Arrays.copyOfRange(bytes, 0x18, 0x1B), true) - 8;

        while (offset < recordSize){
            long attributeID = Utility.byteArrayToLong(Arrays.copyOfRange(bytes, offset, offset+3), true);
            ATTRIBUTE attribute = getAttributeByID(attributeID);
            if(attribute == null) {
                break;
            }
            attributeOffsets.put(attribute, offset);
            if(attribute == ATTRIBUTE.EA) {
                offset += (Utility.byteArrayToInt(Arrays.copyOfRange(bytes, offset+0x4, offset+0x5), true) & 0xff);
            }
            offset += (Utility.byteArrayToInt(Arrays.copyOfRange(bytes, offset+0x4, offset+0x7) , true) & 0xffff);
        }
    }

    private ATTRIBUTE getAttributeByID(Long id) {
        for(ATTRIBUTE attribute : ATTRIBUTE.values()) {
            if(attribute.value == id) return attribute;
        }
        return null;
    }

    public String getFileName() {
        if(!attributeOffsets.containsKey(ATTRIBUTE.FILE_NAME)){
            return "";
        }
        int offset = attributeOffsets.get(ATTRIBUTE.FILE_NAME);
        int attributeSize = Utility.byteArrayToInt(Arrays.copyOfRange(bytes, offset + 0x10, offset + 0x13), true);
        int attributeOffset = Utility.byteArrayToInt(Arrays.copyOfRange(bytes, offset + 0x14, offset + 0x15), true);
        attributeOffset += offset;
        byte[] attribute = Arrays.copyOfRange(bytes, attributeOffset, attributeOffset + attributeSize);
        int nameLength = attribute[0x40] & 0xFF;
        byte[] targetText = Arrays.copyOfRange(attribute, 0x42, 0x42 + (nameLength*2));

        return new String(targetText, StandardCharsets.UTF_16LE);
    }

    public long getFileLengthBytes() {
        return getFileLengthBytes(NTFSInformation.getInstance().getBytesPerCluster());
    }

    public long getFileLengthBytes(int bytesPerCluster) {
        int offset = attributeOffsets.get(ATTRIBUTE.DATA);
        long startingVCN = Utility.byteArrayToLong(Arrays.copyOfRange(bytes, offset+0x11, offset+0x17), true);
        long endingVCN = Utility.byteArrayToLong(Arrays.copyOfRange(bytes, offset+0x18, offset+0x1F), true);
        long fileLengthClusters = endingVCN - startingVCN;
        return fileLengthClusters * bytesPerCluster + bytesPerCluster;
    }
}