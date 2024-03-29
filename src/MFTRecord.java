import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public class MFTRecord extends GenericRecord {

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
            Attribute attribute;
            try {
                attribute = getAttributeByID(attributeID);
            } catch (IllegalArgumentException e) {
                break;
            }
            attributeOffsets.put(attribute, offset);

            int targetOffset;
            if(attribute == Attribute.EA)
                targetOffset = Utility.byteArrayToUnsignedInt(Arrays.copyOfRange(bytes, offset+0x4, offset+0x5), true);
            else
                targetOffset = Utility.byteArrayToUnsignedInt(Arrays.copyOfRange(bytes, offset+0x4, offset+0x7) , true);

            if (targetOffset == 0)
                break;
            offset += targetOffset;
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

    @Override
    public void process() {
        try {
            parseDataAttribute();
        } catch (RuntimeException e) {
            return;
        }

        fileExtension = parseFileExtension();
        fileSizeBytes = parseFileSizeBytes();

        byte[] standardInformationAttribute;
        try {
            standardInformationAttribute = getAttribute(Attribute.STANDARD_INFORMATION);
        } catch (RuntimeException e) {
            return;
        }

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

    private long parseFileSizeBytes() {
        byte[] dataAttribute = getAttribute(Attribute.DATA);
        if(isDataResident) {
            return Utility.byteArrayToUnsignedInt(Arrays.copyOfRange(dataAttribute, 0x10, 0x14), true);
        } else {
            return Math.min(
                    Utility.byteArrayToUnsignedLong(Arrays.copyOfRange(dataAttribute, 0x30, 0x38), true),
                    Utility.byteArrayToUnsignedLong(Arrays.copyOfRange(dataAttribute, 0x38, 0x46), true)
            );
        }
    }

    private static final long WINDOWS_TO_UNIX_EPOCH = -116444736000000000L;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    private String parseWindowsFileTime(long fileTimeRaw) {
        long fileTimeUnix = (fileTimeRaw + WINDOWS_TO_UNIX_EPOCH) / 10000L;
        return DATE_FORMAT.format(fileTimeUnix);
    }

    private LinkedHashMap<Long, Long> dataRunOffsetClusters;

    public LinkedHashMap<Long, Long> getDataRunOffsetClusters() {
        if(dataRunOffsetClusters == null) {
            try {
                parseDataRuns();
            } catch (RuntimeException e) {
                return null;
            }
        }
        return dataRunOffsetClusters;
    }

    private void parseDataRuns() {
        dataRunOffsetClusters = new LinkedHashMap<>();

        byte[] dataAttribute = getAttribute(Attribute.DATA);

        int dataAttributeDataRunsOffset = dataAttribute[0x20];
        byte[] dataRunBytes = Arrays.copyOfRange(dataAttribute, dataAttributeDataRunsOffset, dataAttribute.length);
        int dataRunOffset = 0;
        while(dataRunOffset < dataRunBytes.length) {
            if (dataRunBytes[dataRunOffset] == 0x00) break;

            int judgementByte = dataRunBytes[dataRunOffset] & 0xFF;
            int startLength = judgementByte / 16;
            int lengthLength = judgementByte % 16;

            long lengthClusters = Utility.byteArrayToUnsignedLong(Arrays.copyOfRange(dataRunBytes, dataRunOffset+1, dataRunOffset+1+lengthLength), true);
            long startClusters = Utility.byteArrayToUnsignedLong(Arrays.copyOfRange(dataRunBytes, dataRunOffset+1+lengthLength, dataRunOffset+1+lengthLength+startLength), true);
            long startBytes = startClusters*4096;

            TreeSet<Long> sortedKeys = new TreeSet<>(dataRunOffsetClusters.keySet());
            if(!sortedKeys.isEmpty()) {
                Long lastKey = sortedKeys.last();
                startBytes += lastKey;
            }

            dataRunOffsetClusters.put(startBytes, lengthClusters);
            dataRunOffset += startLength + lengthLength + 1;
        }
    }

    public String getPath() {
        try {
            MFTRecord parentRecord = getParentRecord();
            if(parentRecord == null) return "";

            LinkedList<MFTRecord> parents = new LinkedList<>();
            parents.add(parentRecord);

            while(parents.getLast().getParentRecord() != null) {
                parents.add(parents.getLast().getParentRecord());
            }

            StringBuilder pathBuilder = new StringBuilder();
            for (int i = parents.size() - 1; i >= 0; i--) {
                MFTRecord parent = parents.get(i);
                String fileName = parent.fileName;
                if (fileName.charAt(0) == '$') {
                    fileName = fileName.substring(1);
                }
                pathBuilder.append(fileName);
                pathBuilder.append("/");
            }
            return pathBuilder.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private MFTRecord parentRecord;

    private MFTRecord getParentRecord() throws IOException {
        if(parentRecord != null) return parentRecord;

        byte[] nameAttribute = getAttribute(Attribute.FILE_NAME);
        long parentRecordNumber = Utility.byteArrayToUnsignedLong(Arrays.copyOfRange(nameAttribute, 0x18, 0x1E), true);
        if(parentRecordNumber == 5) return null;

        NTFSInformation ntfsInformation = NTFSInformation.getInstance();
        int recordsPerCluster = ntfsInformation.getBytesPerCluster() / ntfsInformation.getMFTRecordLength();
        int recordOffset = 0;
        for(Map.Entry<Long, Long> dataRun : NTFSInformation.getMFTDataRuns().entrySet()) {
            long recordsInDataRun = dataRun.getValue() * recordsPerCluster;

            if(recordsInDataRun + recordOffset < parentRecordNumber) {
                recordOffset += recordsInDataRun;
            } else {
                long localRecordNumber = parentRecordNumber - recordOffset;
                parentRecord = new MFTRecord(readMFTRecord(dataRun.getKey() + (localRecordNumber * ntfsInformation.getMFTRecordLength())));
                return parentRecord;
            }
        }
        return null;
    }

    private byte[] readMFTRecord(long offset) throws IOException {
        RandomAccessFile diskAccess = new RandomAccessFile(NTFSInformation.getRoot(), "r");
        FileChannel diskChannel = diskAccess.getChannel();
        NTFSInformation ntfsInformation = NTFSInformation.getInstance();
        byte[] mftRecord = new byte[ntfsInformation.getMFTRecordLength()];
        ByteBuffer buffer = ByteBuffer.wrap(mftRecord);

        diskChannel.position(offset);
        diskChannel.read(buffer);

        if(!Arrays.equals(Arrays.copyOf(mftRecord, 4), new byte[]{0x46, 0x49, 0x4C, 0x45})) {
            return null;
        }
        diskAccess.close();
        return mftRecord;
    }
}