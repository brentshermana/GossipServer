package netprog.datatypes;

/**
 * An Object with a specified format for transmission over a network
 */
public interface MessageFormattable {
    /**
     * @return The object instance, formatted for transmission over a network
     */
    byte[] getMessageFormat();
}
