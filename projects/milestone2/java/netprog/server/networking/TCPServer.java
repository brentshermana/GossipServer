package netprog.server.networking;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.Semaphore;

public class TCPServer implements Runnable {
    private static int MAX_THREADS = 100;
    private ServerSocket tcpSocket;
    protected int port;
    protected File gossipFile;
    protected File peersFile;
    protected Semaphore gossipLock;
    protected Semaphore peersLock;
    protected Semaphore logLock;
    protected boolean verbose;


    public TCPServer(int port, File gossip, Semaphore gossipLock, File peers,
            Semaphore peersLock,  boolean verbose, Semaphore logLock) {
        this.port = port;
        this.gossipFile = gossip;
        this.gossipLock = gossipLock;
        this.peersFile = peers;
        this.peersLock = peersLock;
        this.verbose = verbose;
        this.logLock = logLock;
    }
    
    protected void log(String s) {
        if (verbose) {
            try {
                logLock.acquire();
                System.out.println(s);
                logLock.release();
            }
            catch (InterruptedException e) {
                //don't care
            }
        }
    }

    public void run() {
        try {
            tcpSocket = new ServerSocket(port);
            log("TCP Server Running");
            listen();
        }
        catch (IOException e) {
            log("IO Error: " + e.toString());
            e.printStackTrace();
        }
    }

    private void listen() throws IOException {
        //Used to be that tcpSocket was recreated here. Testing has shown that isn't necessary
        int tCount = 0;
        Socket socket = null;
        while (true) {
            try {
                socket = tcpSocket.accept();  
                log("Created new socket");
                //socket accepted here, so start a thread and go back to listening
                Thread currentConnection = new Thread( new TCPConnectionHandler(port,
                        gossipFile, gossipLock, peersFile, peersLock, verbose, logLock,
                       socket));
                boolean threadStarted = false;
                while (!threadStarted) {
                    if (tCount < MAX_THREADS) {
                        currentConnection.start();
                        tCount = Thread.activeCount();
                        threadStarted = true;
                    }
                }
            }
            catch (SocketTimeoutException e) {
                log("Timed out");
            }
            catch (SocketException e) {
                log("Socket Exception " + e.toString());
                StackTraceElement[] trace = e.getStackTrace();
                for (StackTraceElement te : trace)
                    log(te.toString());
            }
            catch (IOException e) {
                log("Error: " + e.getMessage());
            }


        } //ends server continuation loop
    }
}
