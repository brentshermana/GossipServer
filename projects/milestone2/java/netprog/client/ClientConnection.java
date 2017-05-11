package netprog.client;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Created by admin on 3/20/17.
 */
public interface ClientConnection {
    boolean send(String message);
    boolean good();
    void close();
    String recieve() throws IOException;
}
