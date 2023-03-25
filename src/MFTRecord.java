import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public class MFTRecord {

    private static final byte[] checksum = new byte[]{0x46, 0x49, 0x4C, 0x45};

    private final byte[] bytes;
    private final HashMap<Attribute, Integer> attributeOffsets = new HashMap<>();

    private boolean isDeleted = false;
    public boolean isDeleted() {
        return isDeleted;
    }

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
            long attributeID = Utility.byteArrayToUnsignedLong(Arrays.copyOfRange(bytes, offset, offset+3), true);
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

        fileName = getFileName();
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

    private final String fileName;

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

    public long getFileLengthBytes(int bytesPerCluster) {
        int offset = attributeOffsets.get(Attribute.DATA);
        long startingVCN = Utility.byteArrayToUnsignedLong(Arrays.copyOfRange(bytes, offset+0x11, offset+0x17), true);
        long endingVCN = Utility.byteArrayToUnsignedLong(Arrays.copyOfRange(bytes, offset+0x18, offset+0x1F), true);
        long fileLengthClusters = endingVCN - startingVCN;
        return fileLengthClusters * bytesPerCluster + bytesPerCluster;
    }

    long fileSizeBytes;
    String bornTime;
    String modifiedTime;
    String fileExtension;

    private static final long WINDOWS_TO_UNIX_EPOCH = -116444736000000000L; //-116444736000000000L

    public void processAdditionalInformation() {
        fileExtension = "";
        int dotIndex = fileName.lastIndexOf('.');
        if(dotIndex >=0) {
            fileExtension = fileName.substring(dotIndex);
        }

        byte[] standardInformationAttribute = getAttribute(Attribute.STANDARD_INFORMATION);
        if(standardInformationAttribute[0x08] == 1) throw new RuntimeException("MFT Record for " + fileName + " has non-resident 0X10 attribute.");
        int standardInformationInternalAttributeOffset = standardInformationAttribute[0x14];

        long bornTimeRaw = Utility.byteArrayToUnsignedLong(Arrays.copyOfRange(standardInformationAttribute, standardInformationInternalAttributeOffset, standardInformationInternalAttributeOffset + 0x08), true);
        long bornTimeUnix = (bornTimeRaw + WINDOWS_TO_UNIX_EPOCH) / 10000L;

        long modTimeRaw = Utility.byteArrayToUnsignedLong(Arrays.copyOfRange(standardInformationAttribute, standardInformationInternalAttributeOffset + 0x08, standardInformationInternalAttributeOffset + 0x10), true);
        long modTimeUnix = (modTimeRaw + WINDOWS_TO_UNIX_EPOCH) / 10000L;

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        dateFormat.setTimeZone(TimeZone.getDefault());

        bornTime = dateFormat.format(bornTimeUnix);
        modifiedTime = dateFormat.format(modTimeUnix);

        byte[] dataAttribute = getAttribute(Attribute.DATA);
        if(isDataResident) {
            fileSizeBytes = Utility.byteArrayToInt(Arrays.copyOfRange(dataAttribute, 0x10, 0x14), true);
        } else {
            fileSizeBytes = Utility.byteArrayToUnsignedLong(Arrays.copyOfRange(dataAttribute, 0x30, 0x38), true);
        }
    }
}