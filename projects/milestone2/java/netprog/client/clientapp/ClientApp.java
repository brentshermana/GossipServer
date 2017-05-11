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

public class ClientApp {

    private static String mainPrompt = "'p', 'p?', 'g', or 'gt' (h for help)";
    private static String help = "Help:\n'p' to add a addPeer\n'p?' to get a list of registered peers\n'g' to send gossip\n'gt' to send gossip with a custom (not current time) timestamp";

    private static String sendFail = "Message failed to send";

    public static void main (final String[] args) {
        //todo: parse arguments
        // -m message optional
        // -t timestamp optional
        // -T -U TCP UDP optional
        // -s server required
        // -p port required

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
            }

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

    private static String run(ClientConnection connection, boolean udp, String server, int port) {
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

    private static String currentFormattedTime() {
        return formattedTime(new Date());
    }
    private static String formattedTime(Date date) {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS'Z'");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new Date());
    }
}