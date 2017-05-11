package netprog.server;

import netprog.datatypes.Gossip;
import netprog.datatypes.Peer;

import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

import net.ddp2p.ASN1.Encoder;

/**
 * Connection-agnostic implementation of core gossip server functionality
 */
public abstract class GossipServer {

    protected int port;
    protected File gossipFile;
    protected File peersFile;
    protected Semaphore gossipLock;
    protected Semaphore peersLock;
    protected Semaphore logLock;
    protected boolean verbose;
    static boolean exception;

    public GossipServer(int port, File gossip, Semaphore gossipLock, File peersFile, Semaphore peersLock, boolean verbose, Semaphore logLock) {
        this.port = port;
        this.gossipFile = gossip;
        this.gossipLock = gossipLock;
        this.peersFile = peersFile;
        this.peersLock = peersLock;
        this.verbose = verbose;
        this.logLock = logLock;
    }

    /**
     * Reads a message from an InputStream
     *
     * @param is       The stream to read from
     * @return         The parsed message
     * @throws IOException
     */
    protected abstract String recieveMessage(InputStream is) throws IOException; //returns the String that was received

    /**
     * Sends a message to a peer
     *
     * @param message   The message, formatted for transmission over a network
     * @param peer      The peer to send to
     * @return          The success value of the transmission
     */
    protected abstract boolean sendMessage(byte[] message, Peer peer); //return whether it was successful

    /**
     * Parses a command and delegates to the appropriate helper method for execution
     *
     * @param fields  The fields of the command, first of which is the command name
     * @return        The reply to the sender of the command, if any, otherwise null
     */
    protected byte[] runCommand(String[] fields) {
    	switch (fields[0]) {
            case ("GOSSIP"):
                processGossip(fields);
                break;
            case ("PEER"):
                processPeer(fields);
                break;
            case ("PEERS?"):
                return getPeerListing();
            default:
                log("'" + fields[0] + "'  was not recognized by this server");
                break;
        }
        return null;
    }

    /**
     * Logs a message to stdout. Exists for possible transition to file-based logging
     *
     * @param s  the message to log
     */
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


    /**
     *
     * @return  A listing of all peers, formatted to ASN1 per project specifications
     */
    private byte[] getPeerListing() {
    	Encoder mainseq = new Encoder().initSequence();
        List<Peer> peers = getPeers();
        for (Peer peer : peers) {
        	Encoder thisPeersEncoder = new Encoder();
        	thisPeersEncoder = thisPeersEncoder.setASN1Type(1, 1, (byte)2);
        	thisPeersEncoder.initSequence();
        	Encoder name = new Encoder(peer.getName());
        	Encoder port = new Encoder(peer.getPort());
        	Encoder ip = new Encoder(peer.getIp());
        	thisPeersEncoder.addToSequence(name);
        	thisPeersEncoder.addToSequence(port);
        	thisPeersEncoder.addToSequence(ip);
            mainseq.addToSequence(thisPeersEncoder);
        }
        return mainseq.getBytes();
    }

    /**
     * Processes an incoming gossip message. If it was known, print "DISCARDED" to stderr.
     * Otherwise, records the message, and transmits to all known peers
     *
     * @param fields   the Gossip command's peers
     */
    private void processGossip(String[] fields) {
        log("Processing gossipMode...");

        Gossip gossip = new Gossip(fields);

        String fileFormat = gossip.getFileFormat();

        //Upon receiving this message the server should check if it already new it
        //and simply discard it if it was known, while printing "DISCARDED" on the
        //standard error. Otherwise store it, broadcast it to all known peersFile, and print
        //it on the standard error.
        exception = false;
        boolean wasKnown = withGossip( () -> {
        	try {
        		//check to see if messages is already known or not
        		boolean known = false;
        		BufferedReader gossipReader = new BufferedReader(new FileReader(gossipFile));
        		StringBuffer buffer = new StringBuffer();
        		String line = "";
        		while (!known) {
        			//read a single entry from the file:
        			line = gossipReader.readLine();
        			if (line == null)
        				break; //eof
        			else {
        				buffer.append(line);
        				if (line.charAt(line.length() - 1) == '%') {
        					String completeEntry = buffer.toString();
        					if (fileFormat.equals(completeEntry)) {
        						known = true;
        					}
        					buffer = new StringBuffer();
        				}
        			}
        		}
        		gossipReader.close();
        		return known;
        	} catch (IOException e) {
        		e.printStackTrace();
        		exception = true;
        		return null;
        	}
        });
        
        if (exception) {
            log("...Error processing gossipMode!");
        	return;
        } else if (wasKnown) {
            log("...Was already known!");
        	System.err.println("DISCARDED");
        } else { //wasn't known
            //store the message
            withGossip( () -> {
                try {
                	log("...New Gossip! Writing to file:");
                	System.err.println(gossip.getContent());
                	FileWriter writer = new FileWriter(gossipFile, true);
                    writer.write(fileFormat + "\n");
                    writer.close();
                }
                catch (IOException e) {
                    log("Error appending to file");
                    System.err.println(gossip.getContent());
                }
                return 0;
            });


            //broadcast message to peersFile
            List<Peer> peers = getPeers();
            byte[] messageFormat = gossip.getMessageFormat();
            for (Peer peer : peers) {
                log("Forwarding to peer " + peer.getFileFormat());
                sendMessage(messageFormat, peer);
            }
        }
    }


    /**
     *
     * @return All peers known by the gossip server
     */
    protected LinkedList<Peer> getPeers() {
    	return withPeers(() -> {
    		try {
                BufferedReader reader = new BufferedReader(new FileReader(peersFile));
                LinkedList<Peer> list = new LinkedList<>();
                String[] fields = new String[3];
                String line = reader.readLine();
                Scanner scanner;
                while (line != null) {
                    scanner = new Scanner(line);
                    int i = 0;
                    while (i < 3 && scanner.hasNext()) {
                        fields[i++] = scanner.next();
                    }
                    if (i == 3)
                        list.addLast(new Peer(fields));
                    else
                        log("WARN - possible error reading file?");
                    line = reader.readLine();
                }
                reader.close();
                return list;
            }
            catch (FileNotFoundException e) {
            	e.printStackTrace();
                return null;
            }
            catch (IOException e) {
            	e.printStackTrace();
                return null;
            }
    	});
    }


    /**
     * Helper method for ensuring the Gossip file isn't accessed concurrently
     *
     * @param fileAction The action to perform on the file
     * @return           The return value of fileAction
     */
    private <T> T withGossip(Supplier<T> fileAction) {
        boolean acquired = false;
    	while (!acquired) {
    		try {
            	gossipLock.acquire();
            	acquired = true;
        	}
        	catch (InterruptedException e) {
        		acquired = false;
        	}
    	}
        T result = fileAction.get();

        gossipLock.release();

        return result;
    }


    /**
     * Helper method for ensuring the Peers file isn't accessed concurrently
     *
     * @param fileAction The action to perform on the file
     * @return           The return value of fileAction
     */
    private <T> T withPeers(Supplier<T> fileAction) {
    	boolean acquired = false;
    	while (!acquired) {
    		try {
            	peersLock.acquire();
            	acquired = true;
        	}
        	catch (InterruptedException e) {
        		acquired = false;
        	}
    	}

        T result = fileAction.get();

        peersLock.release();

        return result;
    }


    /**
     * Describes the actions to take upon receiving a peer message
     */
    private enum PeerAction {replace, append, nothing};

    /**
     * Processes an incoming peer message.
     * If known, appends the peer to a file.
     * If the name is known, but the port or IP is different from what is known, the peer entry is updated.
     * Otherwise, no action is taken
     *
     * @param fields  The fields for the Peer command
     */
    private void processPeer(String[] fields) {
        log("Processing peer...");

        Peer peer = new Peer(fields);

        withPeers( () -> {
            try {
                List<Peer> peersInFile = new LinkedList<>();
                BufferedReader dataFileReader = new BufferedReader(new FileReader(peersFile));
                String line = "";
                PeerAction action = PeerAction.append; //we will append by default
                while ((line = dataFileReader.readLine()) != null) {
                    //assuming both ip and port must be updated
                    Peer filePeer = new Peer(line);
                    if (filePeer.getName().equals(peer.getName())) {
                        if (!filePeer.equals(peer)) {
                            action = PeerAction.replace;
                            peersInFile.add(peer);
                        }
                        else {
                            action = PeerAction.nothing;
                            break;
                        }
                    } else {
                        peersInFile.add(filePeer);
                    }
                }
                dataFileReader.close();


                FileWriter writer;
                switch (action) {
                    case append:
                        log("Was not known. Appending...");
                        writer = new FileWriter(peersFile, true);
                        writer.write(peer.getFileFormat() + "\n");
                        writer.close();
                        log("Peer appended to file successfully.");
                        break;
                    case replace:
                        log("New information for peer " + peer.getName());
                        writer = new FileWriter(peersFile, false);
                        for (Peer peerToWrite : peersInFile) {
                            writer.write(peerToWrite.getFileFormat() + "\n");
                        }
                        writer.close();
                        log("Peer information updated successfully.");
                        break;
                    case nothing:
                        log("Peer " + peer.getName() + " is already logged with this information");
                }
            }
            catch (IOException e) {
                log("Error processing peer");
            }
            return 0;
        });
    }
}
