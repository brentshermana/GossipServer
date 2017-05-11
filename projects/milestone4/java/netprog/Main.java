package netprog;

import netprog.datatypes.Gossip;
import netprog.encoding.Hashing;
import netprog.server.networking.TCPServer;
import netprog.server.networking.UDPServer;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.concurrent.Semaphore;
import java.util.function.BiFunction;

/**
 * Runnable which takes relevant command line arguments, and launches a {@link TCPServer} and {@link UDPServer}
 */
public class Main implements Runnable {
	static File file = null;
	static boolean verbose = false;
    static int port = -1;
    static String outputDir = null;

    private Semaphore clientLock;
    
	@Override
	public void run() {
		boolean err = false;
        if (port == -1) {
            System.out.println("Port number?");
            err = true;
        }
        if (outputDir == null) {
            System.out.println("Output directory?");
            err = true;
        }
        if (!err) {
            //no error in taking in input, so launch the server
            boolean ready = true;

            File dir = null;
            File gossip = null;
            File peers = null;

            dir = new File(outputDir);
            boolean madeDir = true;
            if (!dir.exists()) {
                System.out.println(dir + " Doesn't exist. Creating...");
                madeDir = dir.mkdir();
                if(madeDir) {
                	System.out.println("Success!");
                } else {
                	System.out.println("Failed to create directory.");
                    ready = false;
                }
            }
                if (madeDir) {
                    System.out.println("Directory specified exists.");
                    BiFunction<String, File, File> makeFile = (String filePrefix, File enclosingDir) -> {
                        //int counter = 1;

                        System.out.println("Creating a new data file...");
                        boolean good = false;
                        

                        file = new File(enclosingDir, filePrefix + ".txt");
                        if (file.exists()) {
                            good = true;
                        }
                        else {
                            try {
                                good = file.createNewFile();
                            } catch (IOException e) {
                                good = false;
                            }
                        }

                        if (good) {
                            System.out.println("Success! file name is: " + file.getName());
                            return file;
                        }
                        else {
                            System.out.println("Failed.");
                            return null;
                        }
                    };
                    gossip = makeFile.apply("gossipFile", dir);
                    peers = makeFile.apply("peersFile", dir);

                    if (gossip == null || peers == null) {
                        ready = false;
                    }

                }

            if (ready) {
                System.out.println("Entering server mode");
                Semaphore logLock = new Semaphore(1);
                Semaphore gossipLock = new Semaphore(1);
                Semaphore peersLock = new Semaphore(1);
                Thread tcpThread = new Thread(new TCPServer(port, gossip, gossipLock, peers, peersLock, verbose, logLock, clientLock), "TCPServer");
                tcpThread.start();
                Thread udpThread = new Thread(new UDPServer(port, gossip, gossipLock, peers, peersLock, verbose, logLock, clientLock), "UDPServer");
                udpThread.start();
            }
            else {
                System.out.println("Could not launch server in specified directory.");
            }
        }
		
	}
	
	public Main (boolean v, int p, String d, Semaphore clientLock) {
		verbose = v;
		port = p;
		outputDir = d;
		this.clientLock = clientLock;
	}

	/*
    static void utilityMode(File gossip, File peers) {
        Scanner in = new Scanner(System.in);

        System.out.println("Gossip, or Peer?");

        String line = in.nextLine();
        line = line.toLowerCase();

        switch (line) {
            case "gossip":
                gossipMode(in, gossip, peers);
                break;
            case "peer":
                peerMode(in, peers);
                break;
            default:
                System.out.println("Unsupported command.");
                break;
        }
    }
    */
	/*
    static void gossipMode(Scanner in, File gossipFile, File peersFile) {
        System.out.println("Enter your gossipMode message, terminated by a line containing only a period");
        StringBuffer buffer = new StringBuffer();

        String line = in.nextLine();
        while (!line.equals(".")) {
            buffer.append(line);
        }

        //CITED SOURCES for hashing:
        //https://github.com/BaconRemovalUnit/SHA256/blob/master/src/SHA256.java
        //https://github.com/yoharel23/FileBase64
        String message = buffer.toString();
        Gossip gossip = new Gossip(Hashing.hash(message), formattedTime(), message);



        //TODO: do something with the gossip message
    }
    */
	/*
    static String formattedTime() {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS'Z'");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new Date());
    }
    static void peerMode(Scanner in, File peers) {
        //TODO
    }
    */


}
