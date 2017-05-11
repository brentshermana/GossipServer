package netprog.datatypes;

import java.util.Calendar;
import net.ddp2p.ASN1.Encoder;

/**
 * Encapsulation for a singular gossip message
 */
public class Gossip implements FileFormattable, MessageFormattable {
    private String encoding;
    private String time;
    private String content;

    public Gossip(String encoding, String time, String content) {
        this.encoding = encoding;
        this.time = time;
        this.content = content;
    }

    public Gossip(String[] message) {
        int index = 0;
        if (message.length == 4) {
            index=1;
        }

        encoding  = message[index++];
        time = message[index++];
        content = message[index++];
    }

    /*
    public String getEncoding() {
        return encoding;
    }
    */

    /*
    public String getTime() {
        return time;
    }
    */

    /**
     * @return The textual content of the gossip message
     */
    public String getContent() {
        return content;
    }

    /**
     * {@inheritDoc}
     */
    public String getFileFormat() {
        return encoding + ":" + time + ":" + content + "%";
    }

    /**
     * {@inheritDoc}
     */
    public byte[] getMessageFormat() {
    	Calendar calTime = getCalendarTime();

    	//sequence: {sha256hash OCTET STRING, timestamp GeneralizedTime, message UTF8String}
    	Encoder hash = new Encoder(encoding.getBytes());
    	Encoder timestamp = new Encoder(calTime);
    	Encoder msg = new Encoder(content);
    	
    	Encoder seq = new Encoder().initSequence();
    	seq = seq.setASN1Type(1, 1, (byte)1);
    	
    	seq.addToSequence(hash);
    	seq.addToSequence(timestamp);
    	seq.addToSequence(msg);
    	
    	return seq.getBytes();
    }


    /**
     * @return  An object representation of the time of the gossip message
     */
    private Calendar getCalendarTime () {
    	Calendar calTime = Calendar.getInstance();
    	String[] parts = time.split("[-]");
    	int year = Integer.parseInt(parts[0]);
    	int month = Integer.parseInt(parts[1]) - 1;
    	int day = Integer.parseInt(parts[2]);
    	int hour = Integer.parseInt(parts[3]);
    	int min = Integer.parseInt(parts[4]);
    	int sec = Integer.parseInt(parts[5]);
    	int mil = Integer.parseInt(parts[6].substring(0, parts[6].length() - 1));
    	calTime.set(year, month, day, hour, min, sec);
    	calTime.setTimeInMillis(calTime.getTimeInMillis() + mil);
    	return calTime;
    }
}
