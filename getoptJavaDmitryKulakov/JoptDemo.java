import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import java.util.Arrays;

public class JoptDemo {

    public static void main (String[] args) {
                
        try {   //parser can be declared exactly like in Getopt Demo
            OptionParser parser = new OptionParser();
            
            parser.accepts("a", "a letter");
            parser.accepts("b", "another letter");
            
            //Treats all the options in the list as synonyms
            parser.acceptsAll(Arrays.asList("h", "help"), 
                "help option").forHelp();
            
            //can use OptionSpec to retrieve arguments in a type safe manner
            OptionSpec<String> path = parser.accepts("output-dir", 
                "specify output directory").withRequiredArg();
            
            //Storing the result in OptionSpec in order to ensure safe type conversion
            //when printing the argument
            OptionSpec<Integer> exampleNumber = parser.accepts("example", "example")
                                            .withOptionalArg().ofType(Integer.class);
                                            
            OptionSet options = parser.parse(args);

            //Does not have the same for each loop capability as Commons CLI
            if (options.has("h") || options.has("help")) {
                parser.printHelpOn(System.out);
                
            } else if (options.has("a")) {
                System.out.println("argument a");
            
            } else if (options.has("output-dir")) {
                System.out.printf ("Option: %s Argument: %s\n", "output-dir",
                    options.valueOf("output-dir"));
                    
              //example of using OptionSpec to print an integer
            } else if (options.has(exampleNumber)) {
                System.out.printf("Option: %s Argument %d\n", "Example", 
                    exampleNumber.value(options)); 
            }
                   
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        
    }
}

