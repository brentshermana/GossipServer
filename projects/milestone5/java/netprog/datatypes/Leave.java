package netprog.datatypes;

import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.ASN1.Encoder;

/**
 * encapsulation for ASN.1 decoding and encoding of "LEAVE" messages
 *
 */
public class Leave {

	/**
	 * Retrieves the byte array, ready to send over the network, of the leave message
	 * 
	 * @param peerName  The name of the peer to tell the server to forget about
	 * @return          The byte array of the ASN.1 encoding
	 */
	public static byte[] getEncoding (String peerName) {
		
		Encoder seq = new Encoder().initSequence();
    	seq = seq.setASN1Type(1, 1, (byte)4);
		
    	Encoder name = new Encoder(peerName);
    	seq.addToSequence(name);
		
		
		return seq.getBytes();
	}
	
	/**
	 * Decodes decoded encapsulating an ASN.1 formatted message retrieved from the network and known to
	 * be a leave message
	 * 
	 * @param dec   The decode to extract information from
	 * @return      The name of the peer given by the leave message for the server to forget
	 */
	public static String decode (Decoder dec) {
		try {
			dec = dec.getContent();
		} catch (ASN1DecoderFail e) {
			System.err.print("Decoding of the ASN.1 message failed unexpectedly, discarding message.");
			//e.printStackTrace();
			return null;
		}
		String name = dec.getFirstObject(true).getString();
		return name;
	}
	
}
