package netprog.datatypes;

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

    public String getContent() {
        return content;
    }

    public String getFileFormat() {
        return encoding + ":" + time + ":" + content + "%";
    }
    public String getMessageFormat() {
        return "GOSSIP:" + getFileFormat();
    }
}
