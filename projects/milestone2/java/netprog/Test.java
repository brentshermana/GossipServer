import netprog.datatypes.Gossip;
import netprog.datatypes.Peer;
import netprog.server.networking.TCPServer;
import netprog.server.networking.UDPServer;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Semaphore;

/**
 * Created by admin on 3/5/17.
 */
public class Test {
    static int serverPort = 19220;
    static int peerPort1 = 19222;
    static int peerPort2 = 19223;

    public static void main (final String[] args) throws Exception {
        File gossipFile = File.createTempFile("gossipMode", ".dat");
        File peersFile = File.createTempFile("peers", ".dat");

        System.out.println(gossipFile == null);
        System.out.println(peersFile == null);

        Semaphore gLock = new Semaphore(1);
        Semaphore pLock = new Semaphore(1);

        Semaphore lLock = new Semaphore(1);

        TCPServer tcp = new TCPServer(serverPort, gossipFile, gLock, peersFile, pLock, false, lLock);
        Thread tcpThread = new Thread(tcp);

        UDPServer udp = new UDPServer(serverPort, gossipFile, gLock, peersFile, pLock, false, lLock);
        Thread udpThread = new Thread(udp);

        tcpThread.start();
        udpThread.start();

        Thread.sleep(50);
        test1();
        Thread.sleep(50);
        test2();
        Thread.sleep(50);
        test3();
        Thread.sleep(50);
        test4();
        Thread.sleep(50);

        udpThread.join();
        tcpThread.join();
    }

    static void test1() throws Exception {
        System.out.println("TEST 1 - ADDING A PEER");

        Peer peer = new Peer("Name", peerPort1, "127.0.0.1");

        Socket socket = new Socket("127.0.0.1", serverPort);

        socket.getOutputStream().write( (peer.getMessageFormat()).getBytes("latin1"));
        socket.getOutputStream().flush();

        Thread.sleep(50);

        socket.getOutputStream().write("PEERS?\n".getBytes("latin1"));
        socket.getOutputStream().flush();

        Thread.sleep(50);

        String reply = readUntil(socket.getInputStream(), '%');


        if (reply.toString().equals("PEERS|1|" + peer.getName() + ":PORT=" + peer.getPort() +  ":IP=" + peer.getIp() + "|%")) {
            System.out.println("SUCCESS");
        }
        else {
            System.out.println("FAIL");
        }

        socket.close();
    }

    static void test2() throws Exception {
        System.out.println("TEST 2 - ALTERING INFORMATION");

        Peer peer = new Peer("Name", peerPort2, "127.0.0.1");

        Socket socket = new Socket("127.0.0.1", serverPort);

        socket.getOutputStream().write(peer.getMessageFormat().getBytes("latin1"));
        socket.getOutputStream().flush();

        Thread.sleep(50);

        socket.getOutputStream().write("PEERS?\n".getBytes("latin1"));

        String reply = readUntil(socket.getInputStream(), '%');

        if (reply.equals("PEERS|1|" + peer.getName() + ":PORT=" + peer.getPort() +  ":IP=" + peer.getIp() + "|%")) {
            System.out.println("SUCCESS");
        }
        else {
            System.out.println("FAIL");
        }

        socket.close();
    }

    static void test3() throws Exception {
        System.out.println("TEST 3: SENDING GOSSIP TO PEERS");

        Gossip gossip = new Gossip("encoding", "time", "message!");

        Socket socket = new Socket("127.0.0.1", serverPort);

        Thread listener = new Thread( () -> {
            try {
                ServerSocket ss = new ServerSocket(peerPort2);
                Socket s = ss.accept();
                if (readUntil(s.getInputStream(), '%').equals(gossip.getMessageFormat())) {
                    System.out.println("SUCCESS");
                }
                else {
                    System.out.println("FAIL");
                }
            }
            catch (Exception e) {

            }
        });
        listener.start();

        socket.getOutputStream().write(gossip.getMessageFormat().getBytes("latin1"));
        socket.getOutputStream().flush();

        Thread.sleep(50);

        socket.getOutputStream().write("PEERS?\n".getBytes("latin1"));

        listener.join();

        socket.close();
    }

    static void test4() throws Exception {
        System.out.println("TEST 4 - REDUNDANT GOSSIP (success if DISCARDED)");

        Gossip gossip = new Gossip("encoding", "time", "message!");

        Socket socket = new Socket("127.0.0.1", serverPort);


        Thread.sleep(50);

        socket.getOutputStream().write(gossip.getMessageFormat().getBytes("latin1"));
        socket.getOutputStream().flush();

        socket.close();
    }

    static String readUntil (InputStream stream, char end) throws Exception {
        InputStreamReader in = new InputStreamReader(stream, "latin1");

        char[] buff = new char[200];
        int counter = 0;
        while (counter < 200) {
            int chars = in.read(buff, counter, buff.length - counter);
            if (chars == -1) return null;
            int cap = counter + chars;
            while (counter < cap) {
                char c = buff[counter++];
                if (c == end) {
                    return new String(buff, 0, counter);
                }
            }
        }

        return null;
    }
}
