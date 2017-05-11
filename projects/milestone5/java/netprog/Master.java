package netprog;

import netprog.server.GossipServer;
import org.apache.commons.cli.*;

import netprog.client.clientapp.ClientApp;

import java.util.concurrent.Semaphore;

/**
 * Entry point of program. Parses command line arguments and launches both
 * a gossip server and a client program for server interaction
 */
public class Master {
	private static final int NUMBER_OF_SECONDS_IN_TWO_DAYS = 172800;

	//CommonsCLI code adapted from Dmitry's provided example
	public static void main (String[] args) {
		Options options = new Options();

        Option delayOpt = Option.builder("D")
                .longOpt("delay")
                .hasArg()
                .desc("delay for peer removal, in seconds")
                .valueSeparator()
                .build();

        Option portOpt = Option.builder("p")
                .longOpt("port")
                .hasArg()
                .desc("Server port")
                .required()
                .valueSeparator()
                .build();

        Option output = Option.builder("d")
                .longOpt("output-dir")
                .hasArg()
                .desc("Server data directory")
                .required()
                .valueSeparator()
                .build();

        Option verboseOpt = Option.builder("v")
                .longOpt("verbose")
                .desc("print server debug messages")
                .build();
        
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

        Option help = Option.builder("h")
                .longOpt("help")
                .build();

        //name of the option, boolean whether it requires an argument,
        //and a description
        options.addOption(portOpt);
        options.addOption(output);
        options.addOption(help);
        options.addOption(verboseOpt);
        options.addOption(delayOpt);

        CommandLineParser parser = new DefaultParser();

        int delay = NUMBER_OF_SECONDS_IN_TWO_DAYS; //two days by default
        boolean verbose = false;
        int port = -1;
        String outputDir = null;

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
                        case "D":
                            delay = Integer.parseInt(option.getValue());
                            break;
                        case "d":
                            outputDir = option.getValue();
                            break;
                        case "v":
                            verbose = true;
                            break;
                        case "T":
                            tcp = true;
                            break;
                        case "U":
                            udp = true;
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
        }  catch (ParseException exp) {
			System.err.println("Parsing failed.  Reason: " + exp.getMessage());
			return;
    	}


        if (tcp && udp) {
            System.err.println("Cannot use udp and tcp at the same time. Defaulting to tcp...");
            udp = false;
        }
        else if (!tcp && !udp) {
            tcp = true; //TCP by default
        } //else (udp && !tcp) is true


        //prevent the client from creating a connection before the server boots
        Semaphore clientLock = new Semaphore(-1);

        //start server
        GossipServer.DELAY = delay;
        Main server = new Main(verbose, port, outputDir, clientLock);
        Thread serverThread = new Thread(server);
        serverThread.start();
        
        //start client
        ClientApp client = new ClientApp(udp, port, message, timeStamp, "127.0.0.1", clientLock);
        Thread clientThread = new Thread(client);
        clientThread.start();
	}
}
