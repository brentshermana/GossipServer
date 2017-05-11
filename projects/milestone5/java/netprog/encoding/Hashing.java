package netprog.encoding;

import java.util.Base64;

/**
 * Class providing helper methods to implement the gossip hashing procedure
 * described in the project description
 */
public class Hashing {

    /**
     * @param original The string to hash
     * @return         The hashed string
     */
    public static String hash(String original) {
        String hex = Hashing.sha256sum(original); //sha256 sum, in hex string
        String binary = Hashing.toBinary(hex); //convert to binary string
        String result = Hashing.encodeBase64(binary); //encode in base 64

        return result;
    }

    private static String toBinary(String hexString) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < hexString.length(); i+=2) {
            buffer.append((char)Integer.parseInt(hexString.substring(i, i+2), 16));
        }
        return buffer.toString();
    }

    private static String encodeBase64(String str) { //works like uuencode
        try {
            byte[] stringBytes = str.getBytes("latin1");
            byte[] encoded = Base64.getEncoder().encode(stringBytes);
            return new String(encoded);
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String sha256sum (String str) {
        return new SHA256(str).getHash();
    }


}
