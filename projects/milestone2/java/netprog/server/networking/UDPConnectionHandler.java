package netprog.server.networking;

import netprog.datatypes.Peer;
import netprog.server.GossipServer;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.Semaphore;

public class UDPConnectionHandler extends GossipServer implements Runnable {
    private static int MAX_MSG = 1000;
    DatagramPacket currentPacket;
    DatagramSocket serverSocket;
    byte[] curBuf;
    
    public UDPConnectionHandler(int port, File gossip, Semaphore gossipLock, File peersFile,
            Semaphore peersLock, boolean verbose, Semaphore logLock, DatagramPacket connectionPacket,
            byte[] buffer, DatagramSocket server) {
        
        super(port, gossip, gossipLock, peersFile, peersLock, verbose, logLock);
        currentPacket = connectionPacket;
        curBuf = buffer;
        serverSocket = server;
    }

    @Override
    public void run() {
        boolean messageDone = false;
        char[] msgCharBuf = new char[MAX_MSG];
        curBuf = currentPacket.getData();
        BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(curBuf)));
        while (!messageDone) {
            int counter = 0;
            while (counter < msgCharBuf.length) {
                int charInt = 0;
                try {
                    charInt = br.read();
                } catch (IOException e) {
                    System.err.println("I/O error occured when reading from"
                            + " the buffer of received packet data.");
                    e.printStackTrace();
                }
                char c = (char) charInt;
                if (c == '%' || counter == 6 && new String(msgCharBuf, 0, 6).equals("PEERS?")) {
                    messageDone = true;
                    break; //complete command read
                }
                msgCharBuf[counter++] = c;
            } // end while (counter < msgBuf.length)
        } // end while (!messageDone)
        //message is done and contained in msgCarBuf[]
        String line = new String(msgCharBuf);
        line = line.trim();
        String[] fields = line.split(":");

        
        
        String response = this.runCommand(fields);
        if (response != null) {
            Peer curPeer = new Peer("PEERS? COMMAND SENDER",
                    currentPacket.getPort(), currentPacket.getAddress().getHostAddress());
            this.sendMessage(response, curPeer);
        }
        
    }

    @Override
    protected String recieveMessage() {
        //TODO: confirm if this is unnecessary in this class
        return null;
    }

    @Override
    protected boolean sendMessage(String message, Peer peer) {
        try {
            DatagramPacket snd;
            if (peer.getIp().equals("127.0.0.1") || peer.getIp().equals(".0.0.1")) {
                snd = new DatagramPacket(message.getBytes("latin1"), message.length(), InetAddress.getLocalHost(), peer.getPort());
            } else {
                snd = new DatagramPacket(message.getBytes("latin1"), message.length(), InetAddress.getByName(peer.getIp()), peer.getPort());
            }
            serverSocket.send(snd);
        } catch (SocketException e) {
            e.printStackTrace();
            return false;
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return false;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true; // message send successful
    }

}
