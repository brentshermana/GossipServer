package netprog.datatypes;

import java.util.Scanner;

import net.ddp2p.ASN1.Encoder;

/**
 * Encapsulation of a single peer in a gossip server
 */
public class Peer implements FileFormattable, MessageFormattable {

    private String name;
    private int port;
    private String ip;

    public Peer(String fileLine) {
        Scanner scanner = new Scanner(fileLine);
        name = scanner.next();
        port = Integer.parseInt(scanner.next());
        ip = scanner.next();
        scanner.close();
    }

    public Peer(String[] message) {
        if (message.length == 4) { //we're parsing raw input

            this.name = message[1];
            //port field begins: 'PORT='
            this.port = Integer.parseInt(message[2].substring(5));
            //ip field begins "IP="
            this.ip = message[3].substring(3);
        }
        else if (message.length == 3) { //we're parsing simple input
            this.name = message[0];
            this.port = Integer.parseInt(message[1]);
            this.ip = message[2];
        }
        else {
            throw new RuntimeException();
        }
    }

    public Peer(String name, int port, String ip) {
        this.name = name;
        this.port = port;
        this.ip = ip;
    }

    /**
     *
     * @return The name of the peer
     */
    public String getName() {
        return name;
    }

    /**
     *
     * @return The port the peer is listening on
     */
    public int getPort() {
        return port;
    }

    /**
     *
     * @return The peer's IP address
     */
    public String getIp() {
        return ip;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Peer) {
            Peer p = (Peer) other;
            return (p.getName().equals(this.name) && p.getIp().equals(this.ip) && p.getPort() == this.port);
        }
        return false;
    }

    /*
    public String toListingFragment() {
        return name + ":" + "PORT=" + port + ":IP=" + ip + "|";
    }
    */

    /**
     * {@inheritDoc}
     */
    public byte[] getMessageFormat() {
    	Encoder thisPeersEncoder = new Encoder().initSequence();
    	thisPeersEncoder = thisPeersEncoder.setASN1Type(1, 1, (byte)2);
    	
    	Encoder pname = new Encoder(name);
    	Encoder pport = new Encoder(port);
    	Encoder pip = new Encoder(ip);
    	
    	thisPeersEncoder.addToSequence(pname);
    	thisPeersEncoder.addToSequence(pport);
    	thisPeersEncoder.addToSequence(pip);
    	
    	return thisPeersEncoder.getBytes();
    }

    /**
     * {@inheritDoc}
     */
    public String getFileFormat() {
        return name + " " + port + " " + ip;
    }
}
