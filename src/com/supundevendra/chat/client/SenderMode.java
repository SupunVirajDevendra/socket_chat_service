package com.supundevendra.chat.client;

import com.supundevendra.chat.util.HexUtil;
import com.supundevendra.chat.util.StreamUtil;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Sender mode: connects to the server, sends raw ISO 8583 hex frames read
 * from stdin, and prints the decoded field dump and binary response received
 * back from the server.
 */
public class SenderMode {

    private static final String  HOST      = "localhost";
    private static final int     PORT      = 8583;
    private static final Pattern HEX_REGEX = Pattern.compile("[0-9A-Fa-f]+");

    /** Type bytes used in the server → client framing protocol. */
    private static final int TYPE_BINARY_RESPONSE = 0x01;
    private static final int TYPE_TEXT_DUMP       = 0x02;

    /** Handshake byte identifying this connection as a sender. */
    private static final int HANDSHAKE_SENDER = 0x01;

    private SenderMode() {}

    /**
     * Starts sender mode. Blocks until the user types {@code exit} or stdin closes.
     */
    public static void run() throws IOException {
        System.out.println("=== ISO 8583 Sender — " + HOST + ":" + PORT + " ===");
        System.out.println("Paste raw hex and press Enter to send. Type 'exit' to quit.");
        System.out.println();

        BufferedReader stdin = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8));

        try (Socket       socket = new Socket(HOST, PORT);
             OutputStream out    = socket.getOutputStream();
             InputStream  in     = socket.getInputStream()) {

            out.write(HANDSHAKE_SENDER);
            out.flush();

            // Background thread: reads type-framed messages from the server and prints them.
            final boolean[] running = {true};
            Thread receiver = buildReceiverThread(in, running);
            receiver.setDaemon(true);
            receiver.start();

            // Main thread: read hex lines from stdin and send to server.
            runStdinLoop(stdin, out, running);
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static Thread buildReceiverThread(InputStream in, boolean[] running) {
        return new Thread(() -> {
            try {
                while (running[0]) {
                    int type = in.read();
                    if (type == -1) break;

                    if (type == TYPE_BINARY_RESPONSE) {
                        printBinaryResponse(in);
                    } else if (type == TYPE_TEXT_DUMP) {
                        printTextDump(in);
                    }
                    // Unknown type bytes are silently skipped for forward compatibility.
                }
            } catch (EOFException | SocketException ignored) {
                // Server closed the connection — normal on exit.
            } catch (IOException e) {
                if (running[0]) {
                    System.err.println("[Receiver error] " + e.getMessage());
                }
            }
        }, "iso-receiver");
    }

    private static void printBinaryResponse(InputStream in) throws IOException {
        byte[] hdr     = StreamUtil.readExact(in, 2);
        int    len     = ((hdr[0] & 0xFF) << 8) | (hdr[1] & 0xFF);
        byte[] payload = StreamUtil.readExact(in, len);
        System.out.println("\nResponse (0110) hex:");
        System.out.println(HexUtil.toHex(payload));
        System.out.println();
    }

    private static void printTextDump(InputStream in) throws IOException {
        System.out.println("\nDecoded fields:");
        String line;
        while ((line = StreamUtil.readTextLine(in)) != null) {
            if (line.equals("--END-OF-DUMP--")) break;
            System.out.println(line);
        }
        System.out.println();
    }

    private static void runStdinLoop(BufferedReader stdin,
                                     OutputStream out,
                                     boolean[] running) throws IOException {
        while (true) {
            System.out.print("> ");
            System.out.flush();
            String input = stdin.readLine();

            if (input == null || input.trim().equalsIgnoreCase("exit")) {
                System.out.println("Disconnecting.");
                running[0] = false;
                break;
            }

            String hex = input.trim();
            if (hex.isEmpty()) continue;

            if (!HEX_REGEX.matcher(hex).matches() || hex.length() % 2 != 0) {
                System.out.println("[Error] Input must be an even-length hex string (e.g. 016DF0F1...)");
                continue;
            }

            byte[] raw = HexUtil.hexToBytes(hex);
            out.write(raw);
            out.flush();
            System.out.println("Sent " + raw.length + " bytes.");
        }
    }
}
