package netprog.client.clientapp;

import netprog.client.ClientConnection;
import netprog.client.TCPClient;
import netprog.client.UDPClient;
import netprog.datatypes.Gossip;
import netprog.datatypes.Leave;
import netprog.datatypes.Peer;
import netprog.encoding.Hashing;

import net.ddp2p.ASN1.Encoder;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.concurrent.Semaphore;

/**
 * A client application which connects to a gossip server, allowing the user to
 * easily send messages to and query the server
 */
public class ClientApp implements Runnable {
    private static String mainPrompt = "'l'(lowercase L), 'p', 'p?', 'g', or 'gt' (h for help)";
    private static String help = "Help:\n'l' (lowercase L) to LEAVE\n'p' to add a addPeer\n'p?' to get a list of registered peers\n'g' to send gossip\n'gt' to send gossip with a custom (not current time) timestamp";
    private static String sendFail = "Message failed to send";

    static boolean udp;
    static String server;
    static int port;
    static String message;
    static String timeStamp;

    private Semaphore clientLock;

	@Override
	public void run () {
        try {
            clientLock.acquire();
        }
        catch (InterruptedException e) {
            System.err.println("Client interrupted while waiting on servers");
        }
		//begin:
        System.out.println("Client utility Begin.");

        //establish connection
        ClientConnection connection;
        if (udp) {
            connection = new UDPClient(server, port);
        }
        else { //tcp
            connection = new TCPClient(server, port);
        }

        if (connection.good()) {
            System.out.println("Connection Established");
        }
        else {
            System.out.println("Connection Failed. Exiting...");
            System.exit(2);
        }

        //send first message if it exists
        if (message != null) {
        	
            if (timeStamp == null) {
                timeStamp = currentFormattedTime();
            }

            String hash = Hashing.hash(message);

            Gossip gossip = new Gossip(hash, timeStamp, message);

            boolean sent = connection.send(gossip.getMessageFormat());

            if (!sent)
                System.out.println(sendFail);
            
            //refresh and create new socket only if this was done, so it is needed
            if (udp) {
                connection = new UDPClient(server, port);
            }
            else { //tcp
                connection = new TCPClient(server, port);
            }
        }

        run(connection, udp, server, port);
		
	}
	
	public ClientApp (boolean U, int p, String m, String t, String s, Semaphore clientLock) {
		udp = U;
		port = p;
        message = m;
        timeStamp = t;
        server = s;
        this.clientLock = clientLock;
	}
    

    private static void run(ClientConnection connection, boolean udp, String server, int port) {
        Scanner in = new Scanner(System.in);
        boolean needNewSocket = false;
        while (true) {
            System.out.printf("\n%s\n", mainPrompt);
            String input = in.next();

            switch (input.toLowerCase()) {
                case "h":
                    System.out.println(help);
                    break;
                case "g":
                    gossip(in, connection, new Date());
                    //need a new socket
                    needNewSocket = true;
                    break;
                case "gt":
                    gossip(in,connection, null);
                    //need a new socket
                    needNewSocket = true;
                    break;
                case "p":
                    addPeer(in, connection);
                    //need a new socket
                    needNewSocket = true;
                    break;
                case "p?":
                    peerQuery(connection);
                    needNewSocket = true;
                    break;
                case "l":
                	leave(in, connection);
                	needNewSocket = true;
                	break;
                default:
                    System.out.println("Input '" + input + "' was not recognized");
                    break;
            }
            if (needNewSocket) {
            	if (udp) {
            		connection.close();
                    connection = new UDPClient(server, port);
                }
            	needNewSocket = false;
            }
        }
    }
    
    /**
     * Handles a user's request to send a "LEAVE" message to the server
     * 
     * @param input           Std input to read for user input from
     * @param connection      The connection to the server
     */
    private static void leave (Scanner input, ClientConnection connection) {
    	String clearLine = input.nextLine();
    	System.out.printf("Please enter the name of the peer to tell the server to forget about: ");
    	String name = input.next();
    	if (connection.send(Leave.getEncoding(name))) {
            System.out.println("Success");
        }
        else {
            System.out.println("Failed to send");
        }
    }

    /**
     * Handles a user request to view all peers from the server
     *
     * @param connection the connection to the server
     */
    private static void peerQuery(ClientConnection connection) {
    	Encoder peerQuerry = Encoder.getNullEncoder();
    	peerQuerry = peerQuerry.setASN1Type(1, 1, (byte)3);
    	
        connection.send(peerQuerry.getBytes());
        String peerListing = null;
		try {
			peerListing = connection.receive();
		} catch (UnsupportedEncodingException e) {
			System.out.println("Encoding literal in code not accepted.");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Error when attempting to receive server response.");
			e.printStackTrace();
		}
        if (peerListing == null) {
            System.out.println("Couldn't parse response from server.");
        }
        else {
            Scanner scanner = new Scanner(peerListing);
            scanner.useDelimiter("\\|");

            String header = scanner.next();
            if (header.equals("PEERS")) {
                int numPeers = scanner.nextInt();
                System.out.println(numPeers + " Peers:");
                for (int i = 0; i < numPeers; i++) {
                    String temp = scanner.next();
                    String[] fields = temp.split(":");

                    String name = fields[0].substring(0);
                    int port = Integer.parseInt(fields[1].substring(5));
                    String ip = fields[2].substring(3);

                    Peer peer = new Peer(name, port, ip);

                    System.out.println(peer.getFileFormat());
                }


            } else {
            	System.out.printf("Unexpected or incorrectly formatted message"
            			+ " received from server...:\n%s\nDiscarding it.", peerListing);
            }
            scanner.close();
        }
    }

    /**
     * handles a user request to insert a gossip message into the server
     *
     * @param in              Std input to read for user input from
     * @param connection      The connection to the server
     * @param date            The date of the gossip message. If null, will be queried from user.
     */
    private static void gossip(Scanner in, ClientConnection connection, Date date) {
        String clearLine = in.nextLine();
    	if (date == null) {
            System.out.println("Would you like to specify the time of day? (y/n)");
            boolean valid = false;
            boolean timeOfDay = false;
            String response;
            while (!valid) {
                response = in.nextLine().toLowerCase();
                if (response.equals("y") || response.equals("n")) {
                    valid = true;
                    timeOfDay = response.equals("y");
                }
                else {
                    System.out.println("(y/n)?");
                }
            }


            String formatString;
            if (!timeOfDay) {
                formatString = "MM/dd/yyyy";
            }
            else {
                formatString = "MM/dd/yyyy HH:mm:ss:SSS";
            }

            System.out.println("Enter the time as follows: " + formatString);
            valid = false;
            DateFormat dateFormat = new SimpleDateFormat(formatString);
            while (!valid) {
                String input = in.nextLine();
                try {
                    date = dateFormat.parse(input);
                    valid = true;
                }
                catch (ParseException e) {
                    System.out.println(e.toString());
                    System.out.println("Try again");
                }
            }
        }

        System.out.println("Type your gossip message below, terminating with a newline character");

        String message = in.nextLine();

        String hash = Hashing.hash(message);
        Gossip gossip = new Gossip(hash, formattedTime(date), message);
        System.out.println("Sending Gossip...");

        if (connection.send(gossip.getMessageFormat())) {
            System.out.println("Success");
        }
        else {
            System.out.println("Failed to send");
        }
    }

    /**
     * Handles a user request to add a peer to the server
     *
     * @param in           std input from user
     * @param connection   connection to server
     */
    private static void addPeer(Scanner in, ClientConnection connection) {
        String clearLine = in.nextLine();
    	System.out.println("What's the name of Peer to add?");
        String name = in.nextLine();
        System.out.println("What port are they listening on?");
        int port = Integer.parseInt(in.nextLine());
        System.out.println("What's their ip?");
        String ip = in.nextLine();

        Peer peer = new Peer(name, port, ip);
        System.out.println("Sending Peer");
        if (connection.send(peer.getMessageFormat())) {
            System.out.println("Success");
        }
        else {
            System.out.println("Failed to send");
        }
    }

    /**
     * @return The current time according to the system, formatted per requirements
     */
    private static String currentFormattedTime() {
        return formattedTime(new Date());
    }

    /**
     * @param  date The date to format
     * @return      The date given, formatted per requirements
     */
    private static String formattedTime(Date date) {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS'Z'");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(date);
    }
}
