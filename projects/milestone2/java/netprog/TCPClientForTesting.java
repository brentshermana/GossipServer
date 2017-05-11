import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class TCPClientForTesting {
    static int abelPort = 50001;
    public static void main(String[] args) throws UnknownHostException, IOException {
        //john(); //probably has errors on the client side now
        abel();
        //bob(); //probably has errors on the client side now
        
    }
    
    
    //currently unused, left in for future testing purposes
    public static void bob () throws UnsupportedEncodingException, IOException {
        Socket sock = new Socket(InetAddress.getByName("127.0.0.1"), 2356);
        OutputStream out = sock.getOutputStream();
        int port = sock.getLocalPort();
        String peerMsg = "PEER:Bob:PORT=" + Integer.toString(port) + ":IP=127.0.0.1%";
        out.write(peerMsg.getBytes("latin1"));
        out.flush();
        
        String peers = "PEERS?\n";
        out.write(peers.getBytes("latin1"));
        out.flush();
        
        InputStreamReader in = new InputStreamReader(sock.getInputStream(), "latin1");
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
        System.out.println("PEERS? response: " + convert);
        

        sock.close();
        
    }
    
    public static void abel () throws IOException {
        //server requires that a client make a serverSocket at the port that the peer is at,
        //or rather the port that the server is told the peer is at, to receive messages sent
        //back to the peer from multiple sources (different threads/sockets)
        ServerSocket peerSocket = new ServerSocket(abelPort);
        
        //sending messages to the server is done from any random port
        Socket sock = new Socket(InetAddress.getByName("127.0.0.1"), 2356);
        OutputStream out = sock.getOutputStream();

        String peerMsg = "PEER:Abel:PORT=" + Integer.toString(abelPort) + ":IP=127.0.0.1%";
        out.write(peerMsg.getBytes("latin1"));
        out.flush();
        
        //sending more messages requires selecting a new port/socket to send 
        //the next message(s) from
        sock = new Socket(InetAddress.getByName("127.0.0.1"), 2356);
        out = sock.getOutputStream();
        String peers = "PEERS?\n";
        out.write(peers.getBytes("latin1"));
        out.flush();
        
        //the PEERS? command response is given directly back to the socket/port that
        //sent the command since this command can be received from sockets/ports/addresses
        //that are not listed as belonging to any known peer, and so must be recveived
        //directly from that socket that sent the PEERS? command
        InputStreamReader in = new InputStreamReader(sock.getInputStream(), "latin1");
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
        System.out.println("PEERS? response: " + convert);
        
        sock = new Socket(InetAddress.getByName("127.0.0.1"), 2356);
        out = sock.getOutputStream();
                
        String gossip = "GOSSIP:FIkIdMdGrKn0/pQqhxoYntXDBjUZlcfPLmpo2MRkUe0:2017-01-09-16-18-20-001Z:Third thing%";
        out.write(gossip.getBytes());
        out.flush();
        
        //to receive commands send/forwarded to peers, such as forwarded GOSSIP messages,
        //the peers serverSocket is called to accept an expected connection
        Socket peerRcv = peerSocket.accept();
        
        in = new InputStreamReader(peerRcv.getInputStream(), "latin1");
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
        System.out.println("GOSSIP response: " + convert);
        
        //sending PEERS? command again without sending any more information about
        //additional peers; this also happens to (most of the time) demonstrate that
        //the multithreading of the TCP side of the server is working properly, since
        //the initial information about peer Abel is sent after the first PEERS? command,
        //but the first PEERS? command response gives 0 known peers since the server has
        // (usually) not yet finished processing the information about peer Abel in the
        //first thread while the second thread is already handling the PEERS? command
        //this PEERS? command should (and does) say that Abel is a known peer, since by
        //this time, the first thread has finished and exited
        sock = new Socket(InetAddress.getByName("127.0.0.1"), 2356);
        out = sock.getOutputStream();
        out.write(peers.getBytes("latin1"));
        out.flush();
        
        in = new InputStreamReader(sock.getInputStream(), "latin1");
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
        System.out.println("PEERS? response: " + convert);

        
    }
    
    
    //currently unused, left in for future testing purposes
    public static void john () throws UnknownHostException, IOException {
        Socket sock = new Socket(InetAddress.getByName("127.0.0.1"), 2356);
        
        
        
        OutputStream out = sock.getOutputStream();
        int port = sock.getLocalPort();
        String peerMsg = "PEER:John:PORT=" + Integer.toString(port) + ":IP=127.0.0.1%";
        out.write(peerMsg.getBytes("latin1"));
        out.flush();
        
        String peers = "PEERS?\n";
        out.write(peers.getBytes("latin1"));
        out.flush();
        
        InputStreamReader in = new InputStreamReader(sock.getInputStream(), "latin1");
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
        System.out.println("PEERS? john response: " + convert);
        

        String gossip = "GOSSIP:k20YjVBh7w8q01IloOYq7z45Kqyh4s35ullNNq3v6uA:2017-01-09-16-18-20-001Z:I like cheese%";
        out.write(gossip.getBytes());
        out.flush();
        
        
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
        System.out.println("GOSSIP john response: " + convert);
        
        
        //sock.close();
        
    }
    
    public static void attemptReuse (int port) throws IOException {
        Socket sock3 = new Socket(InetAddress.getByName("127.0.0.1"), 2356);
        sock3.setReuseAddress(true);
        sock3.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), port));
        InputStreamReader in3 = new InputStreamReader(sock3.getInputStream(), "latin1");
        char[] cbuf3 = new char[1000];
        for (int i = 0; i < 1000; i++) {
            int cint3 = in3.read();
            //System.out.print(cint);
            if ((char)cint3 == '%') {
                break;
            } else if (cint3 == -1) {
                //waiting
                i--;
            } else {
                cbuf3[i] = (char) cint3;
            }

        }
        String convert3 = "";
        for (int i = 0; i < cbuf3.length; i++) {
            System.out.print(cbuf3[i]);
            convert3 += cbuf3[i];
        }
        convert3 = convert3.trim();
        System.out.println(convert3);
    }
}
