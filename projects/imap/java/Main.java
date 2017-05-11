
import org.apache.commons.cli.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main (final String[] args) {
        Options options = new Options();

        options.addOption(
                Option.builder("S")
                        .longOpt("Server")
                        .desc("Server to connect to")
                        .hasArg()
                        .required()
                        .build()
        );

        options.addOption(
                Option.builder("P")
                        .longOpt("Port Number")
                        .desc("Port number of server")
                        .hasArg()
                        .required()
                        .build()
        );

        options.addOption(
                Option.builder("l")
                        .longOpt("username")
                        .desc("User to log in as")
                        .hasArg()
                        .required()
                        .build()
        );

        options.addOption(
                Option.builder("p")
                        .longOpt("password")
                        .desc("password of user")
                        .hasArgs() //more than one if there's a space in the password
                        .required()
                        .build()
        );

        options.addOption(
                Option.builder("d")
                        .longOpt("deleteafter")
                        .desc("Delete After Downloading")
                        .build()
        );

        options.addOption(
                Option.builder("a")
                        .longOpt("allFolders")
                        .desc("Download all folders")
                        .build()
        );

        options.addOption(
                Option.builder("f")
                        .longOpt("folders")
                        .desc("folders to download messages from")
                        .hasArgs() //multiple arguments allowed in a list
                        .build()
        );

        int port = -1;
        boolean allFolders = false;
        ArrayList<String> folders = null;
        boolean deleteAfter = false;
        String password = null;
        String user = null;
        String server = null;

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine line = parser.parse(options, args);

            // automatically generate the help statement
            HelpFormatter formatter = new HelpFormatter();

            //Goes through all the options and prints the long option if applicable,
            //and also any arguments (again, if applicable). If the help option is
            //specified, print the auto generated help page
            for (Option option : line.getOptions()) {

                if (option.getOpt().equals("h")) {
                    formatter.printHelp("IMAP", options);
                } else {
                    switch (option.getOpt()) {
                        case "P":
                            port = Integer.parseInt(option.getValue());
                            break;
                        case "l":
                            user = option.getValue();
                            if (!user.contains("@")) {
                                //append the domain to the username
                                String domain = server.replace("imap.", "");
                                user = user + "@" + domain;
                            }
                            break;
                        case "S":
                            server = option.getValue();
                            break;
                        case "p":
                            StringBuffer buffer = new StringBuffer();
                            for (int i = 0; i < option.getValues().length; i++) {
                                if (i != 0) buffer.append(' ');
                                buffer.append(option.getValues()[i]);
                            }
                            password = buffer.toString();
                            break;
                        case "d":
                            deleteAfter = true;
                            break;
                        case "a":
                            allFolders = true;
                            break;
                        case "f":
                            folders = new ArrayList<String>();
                            folders.addAll(Arrays.asList(option.getValues()));
                            while (folders.remove("-f")){} //in case someone tries "-f folder1 -f folder2"
                            break;
                        default:
                            System.out.println("unrecognized option " + option.getOpt());
                            break;
                    }
                }
            }
        }
        catch (ParseException e) {
            System.err.println("Parsing failed.  Reason: " + e.getMessage());
        }


        Imap imap = new Imap(server, port, user, password, deleteAfter, allFolders, folders);
        imap.run();
    }
}