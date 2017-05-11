package netprog.client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;


public class UDPClient implements ClientConnection {
	private static final String encoding = "latin1";

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

	public boolean good() {
	    return (socket != null);
    }

    public void close() {
	    if (socket != null) {
	        socket.close();
        }
    }

    public boolean send(String s) {
        try {
            byte[] buffer = s.getBytes(encoding);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, server, port);
            socket.send(packet);
            return true;
        }
        catch (Exception e) {
            return false;
        }
	}

	public String recieve() {
		try {
			byte[] buffer = new byte[1000];
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
			socket.receive(packet);

			byte[] data = packet.getData();

			String message = new String(data, "latin1");

			return message;
		}
		catch (IOException e) {
			return null;
		}
	}
}
