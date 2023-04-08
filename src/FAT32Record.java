import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;

public class FAT32Record extends GenericRecord {

    private final byte[] bytes;
    int startCluster;

    public FAT32Record(byte[] fat32record) {
        this(fat32record, "");
    }

    public FAT32Record(byte[] fat32record, String name) {
        bytes = fat32record;
        fileName = name;
    }

    @Override
    public void process() {
        byte[] startClusterBytes = new byte[]{bytes[0x15], bytes[0x14], bytes[0x1B], bytes[0x1A]};
        startCluster = Utility.byteArrayToUnsignedInt(startClusterBytes, false);

        if(fileName.equals("")) {
            fileName = parseSmallFileName();
        }
        fileExtension = parseFileExtension();
        fileSizeBytes = Utility.byteArrayToUnsignedLong(Arrays.copyOfRange(bytes, 0x1C, 0x20), true);

        creationTime = parseFileCreationTime();
        modifiedTime = parseFileModifiedTime();
    }

    private String parseSmallFileName() {
        byte[] filenameBytes = Arrays.copyOfRange(bytes, 0x0, 0x8);
        String filenameRaw = new String(filenameBytes, StandardCharsets.US_ASCII).trim();
        byte[] extensionBytes = Arrays.copyOfRange(bytes, 0x8, 0xB);
        String extension = new String(extensionBytes, StandardCharsets.US_ASCII).trim();

        String filename = filenameRaw + extension;
        if (filename.length() > 0) {
            filename = "_" + filename.substring(1);
        } else {
            filename = "";
        }

        return filename;
    }

    private String parseFileExtension() {
        int dotIndex = fileName.lastIndexOf('.');
        if(dotIndex >=0) {
            fileExtension = fileName.substring(dotIndex);
        }
        return fileExtension;
    }

    private String parseFileCreationTime() {
        return parseDateAndTime(0x10, 0xE);
    }

    private String parseFileModifiedTime() {
        return parseDateAndTime(0x18, 0x16);
    }

    private String parseDateAndTime(int dateOffset, int timeOffset) {
        int timeRaw = Utility.byteArrayToUnsignedInt(Arrays.copyOfRange(bytes, timeOffset, timeOffset + 2), true);

        int hour = (timeRaw >> 11) & 0x1F;
        int minute = (timeRaw >> 5) & 0x3F;

        int dateRaw = Utility.byteArrayToUnsignedInt(Arrays.copyOfRange(bytes, dateOffset, dateOffset + 2), true);

        int year = ((dateRaw >> 9) & 0x7F) + 1980;
        int month = ((dateRaw >> 5) & 0x0F) - 1;
        int day = dateRaw & 0x1F;

        Calendar calendar = Calendar.getInstance();
        //noinspection MagicConstant
        calendar.set(year, month, day, hour, minute);

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        return dateFormat.format(calendar.getTime());
    }
}