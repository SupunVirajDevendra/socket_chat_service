package com.supundevendra.chat.client;

import com.supundevendra.chat.util.StreamUtil;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

// Listener mode: connects to the server and prints every broadcast field dump it receives
public class ListenerMode {

    private static final String HOST = "localhost"; // server hostname
    private static final int    PORT = 8583;        // server port

    // Type byte that indicates the server is sending a text field dump
    private static final int TYPE_TEXT_DUMP     = 0x02;
    // Handshake byte that tells the server this is a listener connection
    private static final int HANDSHAKE_LISTENER = 0x00;

    // Prevent instantiation — all methods are static entry points
    private ListenerMode() {}

    // Connects to the server, sends the listener handshake, then reads and prints broadcast dumps
    public static void run() throws IOException {
        System.out.println("=== ISO 8583 Listener — " + HOST + ":" + PORT + " ===");

        try (Socket      socket = new Socket(HOST, PORT);      // connect to the server
             InputStream in     = socket.getInputStream()) {   // stream for receiving broadcasts

            socket.getOutputStream().write(HANDSHAKE_LISTENER); // identify as a listener
            socket.getOutputStream().flush();                    // send the handshake byte immediately
            System.out.println("Connected. Waiting for broadcasts...");

            while (true) {
                int type = in.read();       // read the type byte of the next frame
                if (type == -1) break;      // server closed the connection

                if (type == TYPE_TEXT_DUMP) {
                    printTextDump(in);      // handle the incoming field dump
                }
                // unrecognised type bytes are silently skipped
            }
        }

        System.out.println("Connection closed.");
    }

    // Reads a text dump frame line by line and prints each line until the end marker
    private static void printTextDump(InputStream in) throws IOException {
        String line;
        while ((line = StreamUtil.readTextLine(in)) != null) { // read one line at a time
            if (line.equals("--END-OF-DUMP--")) break;         // stop at the end marker
            System.out.println(line);                          // print the field line
        }
        System.out.println(); // blank line after each dump for readability
    }
}
