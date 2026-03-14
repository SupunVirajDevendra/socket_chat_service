package com.supundevendra.chat.client;

import com.supundevendra.chat.config.AppConfig;
import com.supundevendra.chat.util.HexUtil;
import com.supundevendra.chat.util.StreamUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

// Sender mode: sends raw ISO 8583 hex to the server and prints the response and field dump
public class SenderMode {

    private static final Logger log = LoggerFactory.getLogger(SenderMode.class); // per-class logger

    private static final Pattern HEX_REGEX = Pattern.compile("[0-9A-Fa-f]+"); // validates hex input

    // Type byte meaning the server is sending a binary ISO response frame
    private static final int TYPE_BINARY_RESPONSE = 0x01;
    // Type byte meaning the server is sending a text field dump
    private static final int TYPE_TEXT_DUMP       = 0x02;
    // Handshake byte that tells the server this is a sender connection
    private static final int HANDSHAKE_SENDER     = 0x01;

    // Prevent instantiation — all methods are static entry points
    private SenderMode() {}

    // Connects to the server, starts a receiver thread, then reads hex from stdin and sends it
    public static void run() throws IOException {
        AppConfig config = AppConfig.getInstance(); // load host/port from config.properties
        String host = config.getHost();             // server hostname
        int    port = config.getPort();             // server port

        System.out.println("=== ISO 8583 Sender — " + host + ":" + port + " ===");
        System.out.println("Paste raw hex and press Enter to send. Type 'exit' to quit.");
        System.out.println();

        // Wrap stdin in a BufferedReader so we can read whole lines
        BufferedReader stdin = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8));

        try (Socket       socket = new Socket(host, port);    // connect to server
             OutputStream out    = socket.getOutputStream();  // stream for sending
             InputStream  in     = socket.getInputStream()) { // stream for receiving

            out.write(HANDSHAKE_SENDER); // identify as sender
            out.flush();                 // send the handshake byte immediately

            final boolean[] running = {true}; // shared flag to stop the receiver thread on exit
            Thread receiver = buildReceiverThread(in, running); // create background reader
            receiver.setDaemon(true); // let the JVM exit even if receiver is still running
            receiver.start();         // start reading server responses in the background

            runStdinLoop(stdin, out, running); // block here reading user input
        }
    }

    // Creates a background thread that reads and prints framed messages from the server
    private static Thread buildReceiverThread(InputStream in, boolean[] running) {
        return new Thread(() -> {
            try {
                while (running[0]) {
                    int type = in.read();       // read the type byte of the next frame
                    if (type == -1) break;      // server closed the connection

                    if (type == TYPE_BINARY_RESPONSE) {
                        printBinaryResponse(in); // handle binary 0110 response frame
                    } else if (type == TYPE_TEXT_DUMP) {
                        printTextDump(in);       // handle text field dump frame
                    }
                    // unrecognised type bytes are silently skipped
                }
            } catch (EOFException | SocketException ignored) {
                // server closed the connection — expected on exit
            } catch (IOException e) {
                if (running[0]) {
                    log.warn("Receiver error: {}", e.getMessage()); // only log if not shutting down
                }
            }
        }, "iso-receiver");
    }

    // Reads a binary 0110 response frame and prints it as hex
    private static void printBinaryResponse(InputStream in) throws IOException {
        byte[] hdr     = StreamUtil.readExact(in, 2);                        // read 2-byte length header
        int    len     = ((hdr[0] & 0xFF) << 8) | (hdr[1] & 0xFF);          // combine bytes into length
        byte[] payload = StreamUtil.readExact(in, len);                      // read ISO payload
        System.out.println("\nResponse (0110) hex:");
        System.out.println(HexUtil.toHex(payload)); // print payload as uppercase hex
        System.out.println();
    }

    // Reads a text dump frame line by line and prints it until the end marker is found
    private static void printTextDump(InputStream in) throws IOException {
        System.out.println("\nDecoded fields:");
        String line;
        while ((line = StreamUtil.readTextLine(in)) != null) { // read one line at a time
            if (line.equals("--END-OF-DUMP--")) break;         // stop at the end marker
            System.out.println(line);                          // print the field line
        }
        System.out.println();
    }

    // Reads hex strings from stdin and sends them to the server; exits on "exit" or EOF
    private static void runStdinLoop(BufferedReader stdin,
                                     OutputStream out,
                                     boolean[] running) throws IOException {
        while (true) {
            System.out.print("> ");
            System.out.flush();              // ensure the prompt appears before blocking
            String input = stdin.readLine(); // block until the user presses Enter

            if (input == null || input.trim().equalsIgnoreCase("exit")) {
                System.out.println("Disconnecting.");
                running[0] = false; // signal receiver thread to stop
                break;
            }

            String hex = input.trim();
            if (hex.isEmpty()) continue; // ignore blank lines

            if (!HEX_REGEX.matcher(hex).matches() || hex.length() % 2 != 0) {
                System.out.println("[Error] Input must be an even-length hex string (e.g. 016DF0F1...)");
                continue; // ask for input again
            }

            byte[] raw = HexUtil.hexToBytes(hex); // convert hex string to bytes
            out.write(raw);                        // send the ISO 8583 frame
            out.flush();                           // push bytes to the server immediately
            System.out.println("Sent " + raw.length + " bytes.");
        }
    }
}
