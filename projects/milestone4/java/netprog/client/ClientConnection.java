package netprog.client;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * An abstraction of the necessary elements for encapsulating a client connection,
 * independent of connection type
 */
public interface ClientConnection {
    /**
     * Sends a message over this connection instance
     *
     * @param message The message to be sent
     * @return        Whether the message was successfully sent
     */
    boolean send(byte[] message);

    /**
     * @return The status of the connection
     */
    boolean good();

    /**
     * Closes the connection
     */
    void close();

    /**
     * @return A message received over the connection
     * @throws IOException
     */
    String receive() throws IOException;
}
