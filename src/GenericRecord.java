public abstract class GenericRecord {

    protected String fileName;
    public String getFileName() {
        return fileName;
    }

    protected String fileExtension = "";
    public String getFileExtension() {
        return fileExtension;
    }

    protected String creationTime;
    public String getCreationTime() {
        return creationTime;
    }

    protected String modifiedTime;
    public String getModifiedTime() {
        return modifiedTime;
    }

    protected long fileSizeBytes;
    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public abstract void process();
}
