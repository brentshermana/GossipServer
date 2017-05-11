package netprog.client;

import netprog.AppUtils;

import java.io.*;
import java.net.*;

public class TCPClient implements ClientConnection {
	private Socket socket;
	private static String encoding = "latin1";

	public TCPClient(String server, int port) {
		try {
			socket = new Socket(server, port);
		}
		catch (IOException e) {
			socket = null;
		}
	}

	public boolean send(String s) {
		try {
			socket.getOutputStream().write(s.getBytes(encoding));
			socket.getOutputStream().flush();
			return true;
		}
		catch (Throwable e) {
			return false;
		}
	}

	public boolean good() {
		return (socket != null && socket.isConnected());
	}

	public void close() {
		try {
			if (socket != null)
				socket.close();
		}
		catch (IOException e) {
			//not much to do
		}
	}


	public String recieve() throws IOException {
		InputStreamReader in = new InputStreamReader(socket.getInputStream(), "latin1");
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
		return convert;
	}

}
