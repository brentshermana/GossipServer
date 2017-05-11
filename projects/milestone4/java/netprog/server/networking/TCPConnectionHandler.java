package netprog.server.networking;

import netprog.AppUtils;
import netprog.datatypes.Peer;
import netprog.server.GossipServer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.Semaphore;

/**
 * Runnable for handling a single TCP client on a single thread
 */
public class TCPConnectionHandler extends GossipServer implements Runnable {
    private static final int TIMEOUT_IN_MILLISECONDS = 20000;
	Socket curSock;
    private static int MAX_MSG = 3000;

    /*
    public TCPConnectionHandler(int port, File gossip, Semaphore gossipLock, File peersFile, Semaphore peersLock,
            boolean verbose, Semaphore logLock) {
        super(port, gossip, gossipLock, peersFile, peersLock, verbose, logLock);
        // should never use this constructor
    }
    */

    public TCPConnectionHandler(int port, File gossip, Semaphore gossipLock, File peersFile, Semaphore peersLock,
            boolean verbose, Semaphore logLock, Socket sock) {
        super(port, gossip, gossipLock, peersFile, peersLock, verbose, logLock);
        curSock = sock;
    }
    
    @Override
    public void run() {
        String message = null;
        try {
			curSock.setSoTimeout(TIMEOUT_IN_MILLISECONDS);
		} catch (SocketException e2) {
			System.err.println("Error in underlying TCP protocol, discarding connection.");
			return;
		}
        InputStream is = null;
        try {
			is = curSock.getInputStream();
		} catch (IOException e1) {
			System.err.print("Failed to acquire input stream from socket connection,"
					+ " closing and discarding the connection.");
			try {
				curSock.close();
			} catch (IOException e) {}
			e1.printStackTrace();
			return;
		}
        while (true) {
        	try {
        		if (!curSock.isClosed()) {
        			message = recieveMessage(is);
        		}
        	} catch (IOException e) {
        		message = null;
            	log("Error: " + e.getMessage());
            	//e.printStackTrace();
        	}
        
        	if (message != null && !curSock.isClosed()) {
            	String[] fields = message.split(":");
            	byte[] reply = this.runCommand(fields);
            	if (reply != null) {
                	try {
                    	OutputStream tcpOut = curSock.getOutputStream();
                    	tcpOut.write(reply);
                    	tcpOut.flush();
                	} catch (IOException e) {
                    	log("Failed to reply to client");
                	}
            	}
        	} else if (curSock.isClosed()) {
        		break;
        	} else {
            	log("Couldn't correctly parse command");
        	}
        }
        try {
        	if (!curSock.isClosed()) {
        		is.close();
        		curSock.close();
        	}
        } catch (IOException e) {
            log("Failed to close socket");
            e.printStackTrace();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String recieveMessage(InputStream is) throws IOException {
        try {
            log("TCP Listening...");

            String parsedMessage = AppUtils.readFromTCPSocket(is, MAX_MSG, curSock);
            return parsedMessage;
        }
        catch (IOException e) {
            throw e;
        }
        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean sendMessage(byte[] message, Peer peer) {
        try {
            Socket socket = new Socket(peer.getIp(), peer.getPort());
            OutputStream os = socket.getOutputStream();
            os.write(message);
            os.flush();
            os.close();
            socket.close();
            return true;
        } catch (java.net.ConnectException e) {
        	log("Failed to connect to peer " + peer.getName() + " at IP " + peer.getIp()
        			+ " and port " + peer.getPort() + ", not forwarding gossip message.");
        	return false;
        } catch (IllegalArgumentException e) {
        	System.err.println("Peer's port or IP is not a valid port or IP, not sending message.");
        	return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

}
