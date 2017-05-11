package netprog.server.networking;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.Semaphore;

/**
 * Handles concurrent UDP connections to a gossip server
 */
public class UDPServer implements Runnable {
    private static int MAX_MSG = 5000;
    private static int MAX_THREADS = 100;
    DatagramSocket serverSocket;
    protected int port;
    protected File gossipFile;
    protected File peersFile;
    protected Semaphore gossipLock;
    protected Semaphore peersLock;
    protected Semaphore logLock;
    protected boolean verbose;

    protected Semaphore maxClientsLock = new Semaphore(MAX_THREADS);
    protected Semaphore clientLock;

    public UDPServer(int port, File gossip, Semaphore gossipLock, File peers, Semaphore peersLock, boolean verbose, Semaphore logLock, Semaphore clientLock) {
        this.port = port;
        this.gossipFile = gossip;
        this.gossipLock = gossipLock;
        this.peersFile = peers;
        this.peersLock = peersLock;
        this.verbose = verbose;
        this.logLock = logLock;
        this.clientLock = clientLock;
    }
    
    public void run() {
        try {
            serverSocket = new DatagramSocket(port);
            clientLock.release();
            System.out.println("UDP Server Running");
            listen();
        }
        catch (IOException e) {
            System.out.println("ioerrer: " + e.toString());
            e.printStackTrace();
        }
    }

    /**
     * Receives a Datagram from a new client and delegates to {@link UDPConnectionHandler}
     *
     * @throws IOException
     */
    private void listen() throws IOException {
        int tCount = 0;
        while (true) {
            try {
                maxClientsLock.acquire();
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            byte[] buffer = new byte[MAX_MSG];
            DatagramPacket recv = new DatagramPacket(buffer, buffer.length);
            serverSocket.receive(recv);
            
            //create and start UDPConnectionHandler thread
            Thread currentConnection = new Thread(new UDPConnectionHandler(port,
                    gossipFile, gossipLock, peersFile, peersLock, verbose, logLock,
                    recv, buffer, serverSocket));

            //TODO: change control flow to avoid busy loop
            boolean threadStarted = false;
            while (!threadStarted) {
                if (tCount < MAX_THREADS) {
                    currentConnection.start();
                    tCount = Thread.activeCount();
                    threadStarted = true;
                }
            }
        } //ends server loop while
    }
}
