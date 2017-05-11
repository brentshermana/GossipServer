import gnu.getopt.LongOpt;
import gnu.getopt.Getopt;

public class GetoptDemo {

    public static void main(String[] args) {
        
        int c;
        String name, arg;
        
        //a poorly phrased flag which is passed into the Getopt constructor to
        //indicate that a single dash can be used instead of the double dash
        //for long options
        boolean long_only = true;
        
        //Double colons indicate that the option before it has an optional argument
        //Single colon indiates that the option before it has a a required argument
        String options = "abc::d:h";
        StringBuffer flag = new StringBuffer();
        
        String help = "usage: java GetoptDemo [-a] [-b] [-c[optional_arg]]\n" +
                      "[-d required_arg] [-h] [--help] [--outputdir=directory]\n" +
                      "[--example]";
        
        //Long options are more verbose instead of a single character
        LongOpt[] longopts = new LongOpt[3];
        longopts[0] = new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h');
        
        //outputdir and example will be caught in case 0 since the flag argument
        //is non-null. They will be distinguished by the contents of the flag,
        //which will be either 'o' or 'e'
        longopts[1] = new LongOpt("outputdir", LongOpt.REQUIRED_ARGUMENT, flag, 'o'); 
        longopts[2] = new LongOpt("example", LongOpt.OPTIONAL_ARGUMENT, flag, 'e');

        Getopt g = new Getopt("GetoptDemo", args, options, longopts, long_only);
 
        while ((c = g.getopt()) != -1) {
        
            switch (c) {
                case 0:
                    //getLongind returns the index of the option in the longOpts,
                    //while getName returns the name of the option
                    name = longopts[g.getLongind()].getName();
                    arg = g.getOptarg();
                    System.out.printf("The user called %s with flag %s and arg %s\n",
                        name, (char)(new Integer(flag.toString())).intValue(), arg);
                    break;

                case 'a':
                case 'b':
                    System.out.println("You picked option " + (char)c);
                    break;
                    
                case 'c':
                case 'd':
                    arg = g.getOptarg();
                    System.out.printf("You picked option '%c' with argument %s\n", 
                        (char) c, (arg != null) ? arg : "null");
                    break;

                case 'h':
                    System.out.println(help);
                    break;


                default:
                    System.out.println("getopt() returned " + c);
                    break;
            }
        }

        for (int i = g.getOptind(); i < args.length ; i++) {
            System.out.println("Non option argv element: " + args[i]);
        }
    }
}

