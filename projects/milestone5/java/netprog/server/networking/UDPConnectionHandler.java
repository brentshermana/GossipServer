package netprog.server.networking;

import netprog.AppUtils;
import netprog.datatypes.Peer;
import netprog.server.GossipServer;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.Semaphore;

import net.ddp2p.ASN1.Decoder;

/**
 * Runnable for handling a single UDP client on a single thread
 */
public class UDPConnectionHandler extends GossipServer implements Runnable {
    DatagramPacket currentPacket;
    DatagramSocket serverSocket;

    //Semaphore maxClientsLock;
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
        curBuf = currentPacket.getData();

        
        Decoder dec = new Decoder(curBuf, 0);
        String line = AppUtils.parseReadyDecoder(dec);
        if (line == null) {
        	log("Couldn't correctly parse command");
        } else {
        	line = line.trim();
        	String[] fields = line.split(":");

        	byte[] response = this.runCommand(fields);
        	if (response != null) {
        		Peer curPeer = new Peer("PEERS? COMMAND SENDER",
        				currentPacket.getPort(), currentPacket.getAddress().getHostAddress());
        		this.sendMessage(response, curPeer);
        	}
        }
        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String recieveMessage(InputStream is) {
        //this is unnecessary in this class
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean sendMessage(byte[] message, Peer peer) {
        try {
            DatagramPacket snd;
            if (peer.getIp().equals("127.0.0.1") || peer.getIp().equals(".0.0.1")) {
                snd = new DatagramPacket(message, message.length, InetAddress.getLocalHost(), peer.getPort());
            } else {
                snd = new DatagramPacket(message, message.length, InetAddress.getByName(peer.getIp()), peer.getPort());
            }
            serverSocket.send(snd);
        } catch (IllegalArgumentException e) {
        	log("Peer's port or IP is not a valid port or IP, not sending message.");
        	return false;
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
