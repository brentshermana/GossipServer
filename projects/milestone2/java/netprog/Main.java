

import netprog.datatypes.Gossip;
import netprog.encoding.Hashing;
import org.apache.commons.cli.*;
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


public class Main {
    //CommonsCLI code adapted from Dmitry's provided example
    public static void main(String[] args) {
        Options options = new Options();

        Option portOpt = Option.builder("p")
                .longOpt("port")
                .hasArg()
                .desc("TCPServer port")
                .required()
                .valueSeparator()
                .build();

        Option output = Option.builder("d")
                .longOpt("output-dir")
                .hasArg()
                .desc("Output directory")
                .required()
                .valueSeparator()
                .build();

        Option verboseOpt = Option.builder("v")
                .longOpt("verbose")
                .desc("print debug messages")
                .build();

        Option help = Option.builder("h")
                .longOpt("help")
                .build();

        //name of the option, boolean whether it requires an argument,
        //and a description
        options.addOption(portOpt);
        options.addOption(output);
        options.addOption(help);
        options.addOption(verboseOpt);

        CommandLineParser parser = new DefaultParser();

        boolean verbose = false;
        int port = -1;
        String outputDir = null;
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
                        case "d":
                            outputDir = option.getValue();
                            break;
                        case "v":
                            verbose = true;
                            break;
                        default:
                            break;
                    }
                }
            }

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
                            

                            File file = new File(enclosingDir, filePrefix + ".txt");
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
                    Thread tcpThread = new Thread(new TCPServer(port, gossip, gossipLock, peers, peersLock, verbose, logLock), "TCPServer");
                    tcpThread.start();
                    Thread udpThread = new Thread(new UDPServer(port, gossip, gossipLock, peers, peersLock, verbose, logLock), "UDPServer");
                    udpThread.start();
                }
                else {
                    System.out.println("Could not launch server in specified directory.");
                }
            }
        } catch (ParseException exp) {
            System.err.println("Parsing failed.  Reason: " + exp.getMessage());
        }

    }
}
