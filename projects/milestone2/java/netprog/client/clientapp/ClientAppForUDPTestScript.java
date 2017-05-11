package netprog.client.clientapp;

import netprog.client.ClientConnection;
import netprog.client.TCPClient;
import netprog.client.UDPClient;
import netprog.datatypes.Gossip;
import netprog.datatypes.Peer;
import netprog.encoding.Hashing;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Scanner;
import java.util.TimeZone;

public class ClientAppForUDPTestScript {

    private static String mainPrompt = "'p', 'p?', 'g', or 'gt' (h for help)";
    private static String help = "Help:\n'p' to add a addPeer\n'p?' to get a list of registered peers\n'g' to send gossip\n'gt' to send gossip with a custom (not current time) timestamp";

    private static String sendFail = "Message failed to send";

    private static String encoding = "latin1";

    public static void main (final String[] args) {
        //todo: parse arguments
        // -m message optional
        // -t timestamp optional
        // -T -U TCP UDP optional
        // -s server required
        // -p port required
    	
    	System.out.println("This is the UDP test client.  The actual client takes in commands"
    			+ " from the keyboard with a scanner, but this test client will automatically"
    			+ " procede as if it received certain commands from the keyboard.  This does"
    			+ " require the server to be started in a sperate terminal to run, as it"
    			+ " connects to our actual server.  For more automated testing with these scripts,"
    			+ " start the server with the \"scriptServer1.sh\" script, which will automatically"
    			+ " start the server on the port that this UDP test client will automaticlaly attempt"
    			+ " to connect to (port 2356).\nThe IP address that the test client will attempt to"
    			+ " connect to is 127.0.0.1, so the server must also be started on a separate terminal"
    			+ " on the same machine for this testing.\nThe commands, and order of those commands,"
    			+ " that this test client"
    			+ " will simulate are as follows:\nIt begins by taking in the date/time from command"
    			+ " line with a gossip message to test that function\nh (to show the help)\n"
    			+ "p? (to request and display the list of known peers)\np (to add a peer to the"
    			+ " server's known peers list)\np? (to request and display the list of known peers,"
    			+ " with the new peer added\ng (to send a gossip message to the server without"
    			+ " specifying date and time)\ngt (to specify the date/time of the gossip message to"
    			+ " send to the server)\n\nThe UDP test client will then exit after demonstrating"
    			+ " that every command works.\n\n");

        Options options = new Options();

        options.addOption(
                Option.builder("p")
                        .longOpt("port")
                        .hasArg()
                        .desc("Server port")
                        .required()
                        .build()
        );

        options.addOption(
                Option.builder("s")
                        .longOpt("server")
                        .hasArg()
                        .desc("Server Address")
                        .required()
                        .build()
        );

        options.addOption(
                Option.builder("T")
                        .longOpt("tcp")
                        .desc("Connect through TCP")
                        .build()
        );

        options.addOption(
                Option.builder("U")
                        .longOpt("udp")
                        .desc("Connect through UDP")
                        .build()
        );

        options.addOption(
                Option.builder("m")
                        .longOpt("message")
                        .hasArg()
                        .desc("gossip message to initially send")
                        .build()
        );

        options.addOption(
                Option.builder("t")
                        .longOpt("timestamp")
                        .hasArg()
                        .desc("timestamp for optional message")
                        .build()
        );

        CommandLineParser parser = new DefaultParser();

        int port = -1;
        String server = null;

        boolean tcp = false;
        boolean udp = false;

        String message = null;
        String timeStamp = null;
        try {
            CommandLine line = parser.parse(options, args);

            // automatically generate the help statement
            HelpFormatter formatter = new HelpFormatter();

            //Goes through all the options and prints the long option if applicable,
            //and also any arguments (again, if applicable). If the help option is
            //specified, print the auto generated help page
            for (Option option : line.getOptions()) {

                if (option.getOpt().equals("h")) {
                    formatter.printHelp("Basic Server", options);
                } else {
                    switch (option.getOpt()) {
                        case "p":
                            port = Integer.parseInt(option.getValue());
                            break;
                        case "T":
                            tcp = true;
                            break;
                        case "U":
                            udp = true;
                            break;
                        case "s":
                            server = option.getValue();
                            break;
                        case "m":
                            message = option.getValue();
                            break;
                        case "t":
                            timeStamp = option.getValue();
                            break;
                        default:
                            System.out.println("Unrecognized option " + option.getOpt());
                            break;
                    }
                }
            }


            if (tcp && udp) {
                System.err.println("Cannot use udp and tcp at the same time. Defaulting to tcp...");
                udp = false;
            }
            else if (!tcp && !udp) {
                tcp = true; //TCP by default
            } //else (udp && !tcp) is true

            //begin:
            System.out.println("Server utility Begin.");

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
        catch (org.apache.commons.cli.ParseException e) {
            System.err.println("Parsing failed.  Reason: " + e.getMessage());
            System.exit(1);
        }


    }

    private static void run(ClientConnection connection, boolean udp, String server, int port) {
        Scanner in = new Scanner(System.in);
        boolean needNewSocket = false;
        int commandCounter = 0;
        String[] commandsList = {"h", "p?", "p", "p?", "g", "gt"};
        while (true && commandCounter < 6) {
        	System.out.printf("\n%s\n", mainPrompt);
            //String input = in.next(); //usually what takes in input
            String input = commandsList[commandCounter++];
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
                default:
                    System.out.println("Input '" + input + "' was not recognized");
                    break;
            }
            if (needNewSocket) {
            	if (udp) {
                    connection = new UDPClient(server, port);
                }
                else { //tcp
                    connection = new TCPClient(server, port);
                }
            	needNewSocket = false;
            }
        }
        System.out.printf("\nThis testing program has now demonstrated all aspects of the UDP client,"
        		+ " and will now close.\nYou may have to scroll up to read through the actions taken"
        		+ " by this script testing program.\n");
    }
    private static void peerQuery(ClientConnection connection) {
        connection.send("PEERS?\n");
        String peerListing = null;
		try {
			peerListing = connection.recieve();
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
    
    private static void gossip(Scanner in, ClientConnection connection, Date date) {
        //String clearLine = in.nextLine(); //commented out for script testing
        boolean initdated = true; //added for client testing
    	if (date == null) {
    		initdated = false;
            System.out.println("Would you like to specify the time of day? (y/n)");
            boolean valid = false;
            boolean timeOfDay = false;
            String response;
            while (!valid) {
                //response = in.nextLine().toLowerCase(); //usually takes in whether user wants to specify
                										//date and time or just date
            	response = "y";
            	System.out.println("For this testing, time of day will be specified.");
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
                //String input = in.nextLine(); //usually takes in date/time
            	System.out.printf("For this testing, the date/time string will be:\n"
            			+ "01/18/2010 07:38:15:238\n");
            	String input = "01/18/2010 07:38:15:238";
                try {
                    date = dateFormat.parse(input);
                    valid = true;
                }
                catch (ParseException e) {
                    System.out.println(e.toString());
                    System.out.println("Try again");
                }
            }
        } //end if for getting date

        System.out.println("Type your gossip message below, terminating with a newline character");

        //String message = in.nextLine(); //usually what takes in gossip message
        String message = "";
        if (initdated) {
        	message = "First UDP test gossip; message with unspecified date/time";
        } else {
        	message = "Second UDP test gossip; message with specified date/time";
        }
        System.out.printf("The gossip message being sent at this time is:\n%s\n", message);
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

    private static void addPeer(Scanner in, ClientConnection connection) {
        //String clearLine = in.nextLine(); //commented out for script testing
    	System.out.println("What's the name of Peer to add?");
        //String name = in.nextLine(); //usually what takes in peer name
    	String name = "Abel";
    	System.out.println("Peers name for testing is \"Abel\"");
        System.out.println("What port are they listening on?");
        //int port = Integer.parseInt(in.nextLine()); //usually what takes in peer port
        int port = 56050;
        System.out.println("Peers port for testing is \"56050\"");
        System.out.println("What's their ip address?");
        //String ip = in.nextLine();  //usually what takes in peer IP address
        String ip = "127.0.0.1";
        System.out.println("Peers IP address for testing is \"127.0.0.1\"");
        Peer peer = new Peer(name, port, ip);
        System.out.println("Sending Peer");
        if (connection.send(peer.getMessageFormat())) {
            System.out.println("Success");
        }
        else {
            System.out.println("Failed to send");
        }
    }

    private static String currentFormattedTime() {
        return formattedTime(new Date());
    }
    private static String formattedTime(Date date) {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS'Z'");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new Date());
    }
}