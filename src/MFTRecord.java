import java.nio.charset.StandardCharsets;
import java.util.*;

public class MFTRecord {

    private static final byte[] checksum = new byte[]{0x46, 0x49, 0x4C, 0x45};

    private boolean isDeleted = false;

    public boolean isDeleted() {
        return isDeleted;
    }

    private final byte[] bytes;
    private final HashMap<Attribute, Integer> attributeOffsets = new HashMap<>();

    private boolean isDataResident = true;
    public boolean isDataResident() {
        return isDataResident;
    }

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
            Attribute attribute = getAttributeByID(attributeID);
            if(attribute == null) {
                break;
            }
            attributeOffsets.put(attribute, offset);
            if(attribute == Attribute.EA) {
                offset += (Utility.byteArrayToInt(Arrays.copyOfRange(bytes, offset+0x4, offset+0x5), true) & 0xff);
            }
            offset += (Utility.byteArrayToInt(Arrays.copyOfRange(bytes, offset+0x4, offset+0x7) , true) & 0xffff);
        }

        if(attributeOffsets.containsKey(Attribute.DATA)){
            isDataResident = (bytes[attributeOffsets.get(Attribute.DATA) + 0x08] & 0xFF) != 1;
        }
    }

    public byte[] getAttribute(Attribute attribute) {
        if(!attributeOffsets.containsKey(attribute)) throw new RuntimeException("An MFTRecord received request for attribute '" + attribute + "', which does not exist in that MFTRecord.");
        int startPos = attributeOffsets.get(attribute);
        int endPos;
        if(attribute == Attribute.EA) endPos = startPos + (Utility.byteArrayToInt(Arrays.copyOfRange(bytes, startPos+0x4, startPos+0x5), true) & 0xff);
        else endPos = startPos + (Utility.byteArrayToInt(Arrays.copyOfRange(bytes, startPos+0x4, startPos+0x7) , true) & 0xffff);
        return(Arrays.copyOfRange(bytes, startPos, endPos));
    }

    private Attribute getAttributeByID(Long id) {
        for(Attribute attribute : Attribute.values()) {
            if(attribute.value == id) return attribute;
        }
        return null;
    }

    public String getFileName() {
        if(!attributeOffsets.containsKey(Attribute.FILE_NAME)){
            return "";
        }
        int offset = attributeOffsets.get(Attribute.FILE_NAME);
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
        int offset = attributeOffsets.get(Attribute.DATA);
        long startingVCN = Utility.byteArrayToLong(Arrays.copyOfRange(bytes, offset+0x11, offset+0x17), true);
        long endingVCN = Utility.byteArrayToLong(Arrays.copyOfRange(bytes, offset+0x18, offset+0x1F), true);
        long fileLengthClusters = endingVCN - startingVCN;
        return fileLengthClusters * bytesPerCluster + bytesPerCluster;
    }
}