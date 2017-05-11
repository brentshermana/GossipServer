package netprog;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class ServerPeerWitness {
    static int abelPort = 56050;
    static int johnPort = 48050;
    public static void main(String[] args) throws UnknownHostException, IOException {
        john(); //receives TCP gossip messages from ClientAppForTCPCTestScript program
    }

    public static void john () throws IOException {
    	System.out.printf("This part of the program simulates a peer (john) listening on TCP port 48050"
    			+ " and receiving two gossip messages (the second two sent by the ClientAppForTCPTestScript"
    			+ " program, run with the scriptClientTCP.sh script; only the second two because the first"
    			+ " is sent before the server is told about the peer John, so it does not know to forward"
    			+ " the first to this port).  To convert this to listen for all"
    			+ " incoming gossip messages sent from the server, it would be simple to enclose the code"
    			+ " that receives the gossip messages in an infinite loop, but for purposes of testing,"
    			+ " and to have a program that exits once it has finished testing, this program only"
    			+ " listens for and accept three gossip messages from the server.\n\nIt should also"
    			+ " be noted that the gossip.txt data file in the script_testing_data folder should be"
    			+ " empty or nonexistant before starting the server for testing, because if it contains"
    			+ " the gossip messages being sent by the ClientAppForTCPTestScript program already,"
    			+ " the server will recognize the messages as duplicates and discard them instead of forwarding"
    			+ " them to peers, such as this one.\n\n");
        //detailed in comments in this method are also instructions to interface with the sever properly
    	
    	//server requires that a client make a serverSocket at the port that the peer is at,
        //or rather the port that the server is told the peer is at, to receive messages sent
        //back to the peer from multiple sources (different threads/sockets)
        ServerSocket peerSocket = new ServerSocket(johnPort);
        
        //sending messages to the server is done from any random port
        
        
        //sending more messages requires selecting a new port/socket to send 
        //the next message(s) from

        
        //the PEERS? command response is given directly back to the socket/port that
        //sent the command since this command can be received from sockets/ports/addresses
        //that are not listed as belonging to any known peer, and so must be recveived
        //directly from that socket that sent the PEERS? command
       
        
       
        
        //to receive commands send/forwarded to peers, such as forwarded GOSSIP messages,
        //the peers serverSocket is called to accept an expected connection
        Socket peerRcv = peerSocket.accept();
        
        InputStreamReader in = new InputStreamReader(peerRcv.getInputStream(), "latin1");
        char[] cbuf = new char[1000];
        for (int i = 0; i < 1000; i++) {
            int cint = in.read();
            if ((char)cint == '%') {
                break;
            } else if (cint == -1) {
                //waiting
                i--;
            } else {
                cbuf[i] = (char) cint;
            }

        }
        String convert = "";
        for (int i = 0; i < cbuf.length; i++) {
            convert += cbuf[i];
        }

        convert = convert.trim();
        System.out.println("First GOSSIP message: " + convert);
        
        
        Socket peerRcv2 = peerSocket.accept();
        
        in = new InputStreamReader(peerRcv2.getInputStream(), "latin1");
        cbuf = new char[1000];
        for (int i = 0; i < 1000; i++) {
            int cint = in.read();
            if ((char)cint == '%') {
                break;
            } else if (cint == -1) {
                //waiting
                i--;
            } else {
                cbuf[i] = (char) cint;
            }

        }
        convert = "";
        for (int i = 0; i < cbuf.length; i++) {
            convert += cbuf[i];
        }

        convert = convert.trim();
        System.out.println("Second GOSSIP message: " + convert);
        

        

        
    }
}
