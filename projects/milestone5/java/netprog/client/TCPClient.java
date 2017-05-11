package netprog.client;

import java.io.*;
import java.math.BigInteger;
import java.net.*;

import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.Decoder;

/**
 * A client connection implementation using TCP
 */
public class TCPClient implements ClientConnection {
	private Socket socket;
	private String server;
	private int port;
	
	public TCPClient(String s, int p) {
		try {
			socket = new Socket(s, p);
			server = s;
			port = p;
		}
		catch (IOException e) {
			socket = null;
		}
	}

    /**
     * Renews the connection to the server
     */
	public void renew () {
		try {
			socket = new Socket(server, port);
		} catch (IOException e) {
			System.err.println("Could not renew connection to server.");
			e.printStackTrace();
		}
	}

    /**
     * {@inheritDoc}
     */
	public boolean send(byte[] s) {
		try {
			//first, check to make sure the server has not closed the connection
			socket.setSoTimeout(1000);
			byte[] buf = new byte[10];
			if (socket.getInputStream().read(buf) < 0) {
				System.out.println("Server closed connection, renewing connection"
						+ " and trying to send again.");
				this.renew();
			}
			socket.getOutputStream().write(s);
			socket.getOutputStream().flush();
			return true;
		} catch (SocketTimeoutException e) {
			try {
				socket.getOutputStream().write(s);
				socket.getOutputStream().flush();
				return true;
			} catch (IOException e1) {
				return false;
			}
		}
		catch (Throwable e) {
			return false;
		}
	}

    /**
     * {@inheritDoc}
     */
	public boolean good() {
		return (socket != null && socket.isConnected());
	}

    /**
     * {@inheritDoc}
     */
	public void close() {
		try {
			if (socket != null)
				socket.close();
		}
		catch (IOException e) {
			//not much to do
		}
	}

    /**
     * {@inheritDoc}
     */
	public String receive() throws IOException {
		InputStream is = socket.getInputStream();
		int numOfPeers = 0;
        byte[] buffer = new byte[5000];
        int msglen = is.read(buffer);
        if (msglen <= 0) {
        	System.err.println("Failed to read the server response, or it was empty.");
        	return null;
        }
        
        Decoder dec = new Decoder(buffer, 0, msglen);
        if (!dec.fetchAll(is)) {
        	System.err.println("Server response was too large or the input stream was"
        			+ " closed before all of the response was read. Discarding server response.");
        	return null;
        } 
        
        try {
			dec = dec.getContent();
		} catch (ASN1DecoderFail e) {
			System.err.print("Decoding of the ASN.1 server response failed unexpectedly,"
					+ " discarding server response.");
			e.printStackTrace();
			return null;
		}

		String fullMsg = "PEERS|#|";
        while (!dec.isEmptyContainer()) {
        	Decoder onePeer = dec.getFirstObject(true);
        	try {
				onePeer = onePeer.getContent();
			} catch (ASN1DecoderFail e) {
				System.err.print("Decoding of the ASN.1 server response peer data failed"
						+ " unexpectedly, discarding server response.");
				e.printStackTrace();
				return null;
			}
			String name = onePeer.getFirstObject(true).getString();
			BigInteger port = onePeer.getFirstObject(true).getInteger();
			String ip = onePeer.getFirstObject(true).getString();
			numOfPeers++;
			String peerData = String.format("%s:PORT=%d:IP=%s|", name, port, ip);
			fullMsg += peerData;
        }
        fullMsg += "%";
        fullMsg = fullMsg.replaceFirst("#", Integer.toString(numOfPeers));
        return fullMsg;
		
		
	}
}
