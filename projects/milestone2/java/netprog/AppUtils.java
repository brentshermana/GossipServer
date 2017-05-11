package netprog;

import netprog.datatypes.FileFormattable;
import netprog.datatypes.Gossip;
import netprog.datatypes.Peer;

import java.io.*;
import java.net.Socket;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

/**
 * Created by admin on 3/14/17.
 */

//TODO: go back into Main (utility mode) and GossipServer and use these functions!
public class AppUtils {
    public static String readFromTCPSocket(Socket socket, int maxByteSize) throws IOException {
        InputStreamReader tcpIn = new InputStreamReader(socket.getInputStream(), "latin1");

        char[] buffer = new char[maxByteSize];
        int counter = 0;
        while (counter < maxByteSize) {
            int charsRead = 0;
            charsRead = tcpIn.read(buffer, counter, maxByteSize - counter);
            if (charsRead == -1) {
                throw new IOException("Input Stream Broken");
            }
            int newThreshold = counter + charsRead;
            while (counter < newThreshold) {
                char c = buffer[counter];
                if (c == '%' || counter == 6 && new String(buffer, 0, 6).equals("PEERS?")) {
                    String completeMessage = new String(buffer, 0, counter);
                    return completeMessage;
                }
                counter++;
            }
        }

        if (counter >= maxByteSize) {
            throw new IOException("Max Message Size Reached. Discarded.");
        }
        return null; //didn't parse a recognizable command
    }

    /*
    public static List<Peer> getPeers(File peersFile) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(peersFile));
            LinkedList<Peer> list = new LinkedList<>();
            String[] fields = new String[3];
            String line = reader.readLine();
            Scanner scanner;
            while (line != null) {
                scanner = new Scanner(line);
                int i = 0;
                while (i < 3 && scanner.hasNext()) {
                    fields[i++] = scanner.next();
                }
                if (i == 3)
                    list.addLast(new Peer(fields));
                //else?
                line = reader.readLine();
            }
            reader.close();
            return list;
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    */

    /*
    public static List<Gossip> getGossip(File gossipFile) {
        List<Gossip> list = new LinkedList<Gossip>();
        try {
            BufferedReader gossipReader = new BufferedReader(new FileReader(gossipFile));
            StringBuffer buffer = new StringBuffer();
            String line = "";
            while (line != null) {
                //read a single entry from the file:
                line = gossipReader.readLine();
                if (line == null)
                    break; //eof
                else {
                    buffer.append(line);
                    if (line.charAt(line.length() - 1) == '%') {
                        String completeEntry = buffer.toString();
                        completeEntry = completeEntry.substring(0, completeEntry.length()-1); //remove percent symbol
                        Gossip nextGossip = new Gossip(completeEntry.split(":"));
                        list.add(nextGossip);

                        buffer = new StringBuffer();
                    }
                }
            }
            gossipReader.close();
            return list;
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    */

    /*
    public static boolean appendToFile(File file, String str) {
        try {
            BufferedWriter dataFileWriter = new BufferedWriter(new FileWriter(file));
            dataFileWriter.write(str);
            dataFileWriter.flush();
            dataFileWriter.close();
            return true;
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    */

    /*
    public static boolean fillFile(File file, Collection<FileFormattable> content) {
        try {
            FileWriter writer = new FileWriter(file, false);
            for (FileFormattable thing : content) {
                writer.write(thing.getFileFormat());
            }
            writer.close();
            return true;
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    */
}
