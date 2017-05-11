package netprog.datatypes;

/**
 * An Object that has a specified protocol for file storage
 */
public interface FileFormattable {
    /**
     * @return The object instance, formatted for writing to a file
     */
    String getFileFormat();
}
