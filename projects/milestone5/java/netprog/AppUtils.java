package netprog;

import netprog.datatypes.FileFormattable;
import netprog.datatypes.Gossip;
import netprog.datatypes.Leave;


import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.net.SocketTimeoutException;

import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.Decoder;


//TODO: go back into Main (utility mode) and GossipServer and use these functions!

/**
 * Provides helper methods for various functions used in the codebase
 */
public class AppUtils {
    /**
     *
     * @param time The time to format
     * @return     The time, formatted to the format specified by the project description
     */
	public static String formatGenTime (String time) {
    	String formatted = "";
    	String year = time.substring(0, 4);
    	String month = time.substring(4, 6);
    	String day = time.substring(6, 8);
    	String hour = time.substring(8, 10);
    	String min = time.substring(10, 12);
    	String sec = time.substring(12, 14);
    	String miliZ = time.substring(15);
    	formatted = year + "-" + month + "-" + day + "-" + hour + "-" + min + "-" + sec + "-" + miliZ;
    	return formatted;
    }

    /**
     * Parses an ASN1 message into formats used for previous project milestones
     *
     * @param dec  The decoder containing the ASN1 message
     * @return     The parsed message
     */
	public static String parseReadyDecoder (Decoder dec) {
		String message = "";
		if (dec.typeClass() == 1) {
         	//it is application class, so gossip, peer, or peersQuerry
         	if (dec.tagVal() == 1) {
         		//gossip
         		try {
 					dec = dec.getContent();
 					byte[] hash = dec.getFirstObject(true).getBytes();
 	        		String strHash = new String(hash);
 	        		String time = dec.getFirstObject(true).getGeneralizedTimeAnyType();
 	        		String formattedTime = formatGenTime(time);
 	        		String msg= dec.getFirstObject(true).getString();
 	        		//assumes that the msg is encoded by the client with a % at the end already
 	        		message = "GOSSIP:" + strHash + ":" + formattedTime + ":" + msg;
 				} catch (ASN1DecoderFail e) {
 					System.err.print("Decoding of the ASN.1 message failed unexpectedly, discarding message.");
 					//e.printStackTrace();
 					return null;
 				}
         	} else if (dec.tagVal() == 2) {
         		//peer
         		try {
 					dec = dec.getContent();
 					String name = dec.getFirstObject(true).getString();
 					BigInteger port = dec.getFirstObject(true).getInteger();
 					String ip = dec.getFirstObject(true).getString();
 					message = "PEER:" + name + ":PORT=" + port.toString() + ":IP=" + ip;
 				} catch (ASN1DecoderFail e) {
 					System.err.print("Decoding of the ASN.1 message failed unexpectedly, discarding message.");
 					//e.printStackTrace();
 					return null;
 				}
         	} else if (dec.tagVal() == 3) {
         		//peersQuery
         		message = "PEERS?";
         	} else if (dec.tagVal() == 4) {
         		//leave message
         		String name = Leave.decode(dec);
         		message = "LEAVE:" + name;
         	} else {
         		System.err.println("Unrecognized application specific tag value, discarding message.");
         		return null;
         	}
         } else {
         	System.err.println("Message does not have class type of application, discarding message.");
         	return null;
         }
		 return message;
    }

    /**
     * Reads an ASN1 message from a TCP stream. Delegates to {@link #parseReadyDecoder}
     *
     * @param is            A TCP stream to read from
     * @param maxByteSize   The maximum number of bytes to be read
     * @param socket        The TCP socket
     * @return              The incoming message, parsed from ASN1 format
     * @throws IOException
     */
    public static String readFromTCPSocket(InputStream is, int maxByteSize, Socket socket) throws IOException {
    	byte[] buffer = new byte[maxByteSize];
        int msglen = 0;
        try {
        	msglen = is.read(buffer);
        } catch (SocketTimeoutException e) {
        	System.out.println("TCP Client Connection timed out, closing connection.");
        	is.close();
        	socket.close();
        	return null;
        } catch (IOException e) {
        	System.err.println("Error when reading from client connection, possibly due to client closing the connection.");
        	socket.close();
        	is.close();
        }
        if (msglen == 0) {
        	System.err.println("Failed to read the message, or the message was empty.");
        	return null;
        } else if (msglen < 0) {
        	System.out.println("Client closed the TCP connection, closing server side of it now.");
        	is.close();
        	socket.close();
        	return null;
        }
        Decoder dec = new Decoder(buffer, 0, msglen);
        if (!dec.fetchAll(is)) {
        	System.err.println("Message was too large or the input stream was closed before all of the message was read. Discarding message.");
        	return null;
        } 
        return parseReadyDecoder(dec);
    }

    /*
    public static List<Peer> getPeers(File peersFile) {
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
                //else?
                line = reader.readLine();
            }
            reader.close();
            return list;
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    */

    /*
    public static List<Gossip> getGossip(File gossipFile) {
        List<Gossip> list = new LinkedList<Gossip>();
        try {
            BufferedReader gossipReader = new BufferedReader(new FileReader(gossipFile));
            StringBuffer buffer = new StringBuffer();
            String line = "";
            while (line != null) {
                //read a single entry from the file:
                line = gossipReader.readLine();
                if (line == null)
                    break; //eof
                else {
                    buffer.append(line);
                    if (line.charAt(line.length() - 1) == '%') {
                        String completeEntry = buffer.toString();
                        completeEntry = completeEntry.substring(0, completeEntry.length()-1); //remove percent symbol
                        Gossip nextGossip = new Gossip(completeEntry.split(":"));
                        list.add(nextGossip);

                        buffer = new StringBuffer();
                    }
                }
            }
            gossipReader.close();
            return list;
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    */

    /*
    public static boolean appendToFile(File file, String str) {
        try {
            BufferedWriter dataFileWriter = new BufferedWriter(new FileWriter(file));
            dataFileWriter.write(str);
            dataFileWriter.flush();
            dataFileWriter.close();
            return true;
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    */

    /*
    public static boolean fillFile(File file, Collection<FileFormattable> content) {
        try {
            FileWriter writer = new FileWriter(file, false);
            for (FileFormattable thing : content) {
                writer.write(thing.getFileFormat());
            }
            writer.close();
            return true;
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    */
}
