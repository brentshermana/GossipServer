package netprog.server.networking;

import netprog.AppUtils;
import netprog.datatypes.Peer;
import netprog.server.GossipServer;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.Semaphore;

public class TCPConnectionHandler extends GossipServer implements Runnable {
    Socket curSock;
    private static int MAX_MSG = 300;
    
    public TCPConnectionHandler(int port, File gossip, Semaphore gossipLock, File peersFile, Semaphore peersLock,
            boolean verbose, Semaphore logLock) {
        super(port, gossip, gossipLock, peersFile, peersLock, verbose, logLock);
        // should never use this constructor
    }

    public TCPConnectionHandler(int port, File gossip, Semaphore gossipLock, File peersFile, Semaphore peersLock,
            boolean verbose, Semaphore logLock, Socket sock) {
        super(port, gossip, gossipLock, peersFile, peersLock, verbose, logLock);
        curSock = sock;
    }
    
    @Override
    public void run() {
        String message = null;
        try {
            message = recieveMessage();
        } catch (IOException e) {
            log("Error: " + e.getMessage());
            //e.printStackTrace();
        }
        
        if (message != null) {
            String[] fields = message.split(":");
            String reply = this.runCommand(fields);
            if (reply != null) {
                try {
                    OutputStream tcpOut = curSock.getOutputStream();
                    tcpOut.write(reply.getBytes("latin1"));
                    tcpOut.flush();
                } catch (IOException e) {
                    log("Failed to reply to client");
                }
            }
        } else {
            log("Couldn't correctly parse command");
        }
        
        try {
            curSock.close();
        } catch (IOException e) {
            log("Failed to close socket");
            e.printStackTrace();
        }
        
    }

    @Override
    protected String recieveMessage() throws IOException {
        try {
            log("TCP Listening...");

            String parsedMessage = AppUtils.readFromTCPSocket(curSock, MAX_MSG);
            return parsedMessage;
        }
        catch (IOException e) {
            throw e;
        }
        
    }

    @Override
    protected boolean sendMessage(String message, Peer peer) {
        try {
            if (message.charAt(message.length()-1) != '%')
                message = message + "%"; //added for client testing purposes
            Socket socket = new Socket(peer.getIp(), peer.getPort());
            OutputStream os = socket.getOutputStream();
            os.write(message.getBytes("latin1"));
            os.flush();
            os.close();
            socket.close();
            return true;
        } catch (java.net.ConnectException e) {
        	System.out.printf("Failed to connect to peer %s at IP %s and port %d,"
        			+ " not forwarding gossip message.\n", peer.getName(),
        			peer.getIp(), peer.getPort());
        	return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

}
