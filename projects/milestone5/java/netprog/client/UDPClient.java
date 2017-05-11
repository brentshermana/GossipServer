package netprog.client;

import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.Decoder;


/**
 * A client connection implementation using UDP
 */
public class UDPClient implements ClientConnection {


	private InetAddress server;
	private int port;
	private DatagramSocket socket;

	public UDPClient(String server, int port) {
		try {
            this.server = InetAddress.getByName(server);
            this.port = port;
			socket = new DatagramSocket();
		}
		catch (Exception e) {
			socket = null;
			System.out.println("Failed to connect to the UDP server at the"
					+ " specified port and address.");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean good() {
	    return (socket != null);
    }

	/**
	 * {@inheritDoc}
	 */
    public void close() {
	    if (socket != null) {
	        socket.close();
        }
    }

	/**
	 * {@inheritDoc}
	 */
    public boolean send(byte[] s) {
        try {
            DatagramPacket packet = new DatagramPacket(s, s.length, server, port);
            socket.send(packet);
            return true;
        }
        catch (Exception e) {
            return false;
        }
	}

	/**
	 * {@inheritDoc}
	 */
	public String receive() {
		//currently the only message the client can receive is the PeersAnswer message
		byte[] buffer = new byte[5000];
		int numOfPeers = 0;
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
		try {
			socket.receive(packet);
		} catch (IOException e) {
			System.err.println("Error when waiting to receive response from the server.");
			e.printStackTrace();
			return null;
		}
		byte[] data = packet.getData();
		Decoder dec = new Decoder(data, 0);
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
