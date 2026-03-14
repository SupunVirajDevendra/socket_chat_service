package com.supundevendra.chat.client;

import com.supundevendra.chat.util.StreamUtil;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

/**
 * Listener mode: connects to the server passively and prints every ISO 8583
 * field dump that is broadcast by the server.
 */
public class ListenerMode {

    private static final String HOST = "localhost";
    private static final int    PORT = 8583;

    /** Type byte for text-dump frames sent by the server. */
    private static final int TYPE_TEXT_DUMP = 0x02;

    /** Handshake byte identifying this connection as a listener. */
    private static final int HANDSHAKE_LISTENER = 0x00;

    private ListenerMode() {}

    /**
     * Starts listener mode. Blocks until the server closes the connection.
     */
    public static void run() throws IOException {
        System.out.println("=== ISO 8583 Listener — " + HOST + ":" + PORT + " ===");

        try (Socket      socket = new Socket(HOST, PORT);
             InputStream in     = socket.getInputStream()) {

            socket.getOutputStream().write(HANDSHAKE_LISTENER);
            socket.getOutputStream().flush();
            System.out.println("Connected. Waiting for broadcasts...");

            while (true) {
                int type = in.read();
                if (type == -1) break;

                if (type == TYPE_TEXT_DUMP) {
                    printTextDump(in);
                }
                // Unknown type bytes silently skipped for forward compatibility.
            }
        }

        System.out.println("Connection closed.");
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static void printTextDump(InputStream in) throws IOException {
        String line;
        while ((line = StreamUtil.readTextLine(in)) != null) {
            if (line.equals("--END-OF-DUMP--")) break;
            System.out.println(line);
        }
        System.out.println();
    }
}
