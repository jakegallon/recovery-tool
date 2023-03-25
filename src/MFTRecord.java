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

        parseIsDeleted();
        parseAttributeOffsets();
        parseFileName();
    }

    public void parseDataAttribute() {
        if(!attributeOffsets.containsKey(Attribute.DATA)) {
            throw new RuntimeException("MFT Record for " + fileName + " has no 0x80 attribute");
        }
        isDataResident = (bytes[attributeOffsets.get(Attribute.DATA) + 0x08] & 0xFF) != 1;
    }

    private void parseAttributeOffsets() {
        int offset = Utility.byteArrayToUnsignedInt(Arrays.copyOfRange(bytes, 0x14, 0x15), true);
        int recordSize = Utility.byteArrayToUnsignedInt(Arrays.copyOfRange(bytes, 0x18, 0x1B), true) - 8;

        while (offset < recordSize){
            long attributeID = Utility.byteArrayToUnsignedLong(Arrays.copyOfRange(bytes, offset, offset+3), true);
            Attribute attribute = getAttributeByID(attributeID);
            attributeOffsets.put(attribute, offset);
            if(attribute == Attribute.EA) {
                offset += (Utility.byteArrayToUnsignedInt(Arrays.copyOfRange(bytes, offset+0x4, offset+0x5), true));
            } else {
                offset += (Utility.byteArrayToUnsignedInt(Arrays.copyOfRange(bytes, offset+0x4, offset+0x7) , true));
            }
        }
    }

    private void parseIsDeleted() {
        int allocatedFlag = Utility.byteArrayToUnsignedInt(Arrays.copyOfRange(bytes, 0x16, 0x17), true);
        if(allocatedFlag == 0x0000 || allocatedFlag == 0x0200) {
            isDeleted = true;
        }
    }

    public byte[] getAttribute(Attribute attribute) {
        if(!attributeOffsets.containsKey(attribute)) throw new RuntimeException("An MFTRecord received request for attribute '" + attribute + "', which does not exist in that MFTRecord");
        int startPos = attributeOffsets.get(attribute);
        int endPos;
        if(attribute == Attribute.EA) endPos = startPos + (Utility.byteArrayToUnsignedInt(Arrays.copyOfRange(bytes, startPos+0x4, startPos+0x5), true));
        else endPos = startPos + (Utility.byteArrayToUnsignedInt(Arrays.copyOfRange(bytes, startPos+0x4, startPos+0x7) , true));
        return(Arrays.copyOfRange(bytes, startPos, endPos));
    }

    private Attribute getAttributeByID(Long id) {
        for(Attribute attribute : Attribute.values()) {
            if(attribute.value == id) return attribute;
        }
        throw new IllegalArgumentException("Invalid attribute ID: " + id);
    }

    private String fileName;
    public String getFileName() {
        return fileName;
    }

    public void parseFileName() {
        if(!attributeOffsets.containsKey(Attribute.FILE_NAME)){
            fileName = "";
            return;
        }
        int offset = attributeOffsets.get(Attribute.FILE_NAME);
        int attributeSize = Utility.byteArrayToUnsignedInt(Arrays.copyOfRange(bytes, offset + 0x10, offset + 0x13), true);
        int attributeOffset = Utility.byteArrayToUnsignedInt(Arrays.copyOfRange(bytes, offset + 0x14, offset + 0x15), true);
        offset += attributeOffset;

        byte[] attribute = Arrays.copyOfRange(bytes, offset, offset + attributeSize);
        int nameLength = attribute[0x40] & 0xFF;
        byte[] targetText = Arrays.copyOfRange(attribute, 0x42, 0x42 + (nameLength*2));

        fileName = new String(targetText, StandardCharsets.UTF_16LE);
    }

    private String fileExtension = "";
    public String getFileExtension() {
        return fileExtension;
    }

    private String creationTime;
    public String getCreationTime() {
        return creationTime;
    }

    private String modifiedTime;
    public String getModifiedTime() {
        return modifiedTime;
    }

    private long fileSizeBytes;
    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void processAdditionalInformation() {
        parseDataAttribute();

        fileExtension = parseFileExtension();
        fileSizeBytes = parseFileSizeBytes();

        byte[] standardInformationAttribute = getAttribute(Attribute.STANDARD_INFORMATION);
        if(standardInformationAttribute[0x08] == 1) throw new RuntimeException("MFT Record for " + fileName + " has non-resident 0X10 attribute");
        int standardInfoAttrOffset = standardInformationAttribute[0x14];

        long bornTimeRaw = Utility.byteArrayToUnsignedLong(Arrays.copyOfRange(standardInformationAttribute, standardInfoAttrOffset, standardInfoAttrOffset + 0x08), true);
        creationTime = parseWindowsFileTime(bornTimeRaw);

        long modTimeRaw = Utility.byteArrayToUnsignedLong(Arrays.copyOfRange(standardInformationAttribute, standardInfoAttrOffset + 0x08, standardInfoAttrOffset + 0x10), true);
        modifiedTime = parseWindowsFileTime(modTimeRaw);
    }

    private String parseFileExtension() {
        int dotIndex = fileName.lastIndexOf('.');
        if(dotIndex >=0) {
            fileExtension = fileName.substring(dotIndex);
        }
        return fileExtension;
    }

    private long parseFileSizeBytes() {byte[] dataAttribute = getAttribute(Attribute.DATA);
        if(isDataResident) {
            return Utility.byteArrayToUnsignedInt(Arrays.copyOfRange(dataAttribute, 0x10, 0x14), true);
        } else {
            return Utility.byteArrayToUnsignedLong(Arrays.copyOfRange(dataAttribute, 0x30, 0x38), true);
        }
    }

    private static final long WINDOWS_TO_UNIX_EPOCH = -116444736000000000L; //-116444736000000000L
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    private String parseWindowsFileTime(long fileTimeRaw) {
        long fileTimeUnix = (fileTimeRaw + WINDOWS_TO_UNIX_EPOCH) / 10000L;
        return DATE_FORMAT.format(fileTimeUnix);
    }
}