import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;

public class CommonsCLI_Demo {
    
    public static void main(String [] args) {
    
        Options options = new Options();
        Option Example = Option.builder("e")
                                       .longOpt("Example")
                                       .desc("Example")
                                       .valueSeparator()
                                       .build();
                                      
        Option outputDir = Option.builder("o")
                                         .longOpt("output-dir")
                                         .hasArg()
                                         .desc("Ouput directory")
                                         .valueSeparator()
                                         .build();
                                         
        Option help = Option.builder("h")
                                    .longOpt("help")
                                    .build();
        
        //name of the option, boolean whether it requires an argument,
        //and a description
        options.addOption("a", false, "The letter a");
        options.addOption(Example);
        options.addOption(outputDir);
        options.addOption(help);
        
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
                    formatter.printHelp("CommonsCLI_Demo", options);
                
                } else {
                    System.out.printf("Option: %s %s\n", option.hasLongOpt() ? 
                        option.getLongOpt() : option.getOpt(), 
                        option.hasArg() || option.hasOptionalArg() ? "Argument: " +
                        option.getValue() : "");
                }
            }
            
            //gets the non-option arguments
            for (String nonOption : line.getArgs()) {
                System.out.println("Non-option: " + nonOption);
            }
            
        } catch( ParseException exp ) {
            System.err.println("Parsing failed.  Reason: " + exp.getMessage());
        }
       
    }
}
