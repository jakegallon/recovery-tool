public enum Attribute {
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

    public final int value;

    Attribute(int hex) {
        value = hex;
    }
}