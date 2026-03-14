package com.supundevendra.chat.client;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class Client {

    private static final String  HOST      = "localhost";
    private static final int     PORT      = 8583;
    private static final Pattern HEX_REGEX = Pattern.compile("[0-9A-Fa-f]+");

    /** Type bytes used by the server→client framing protocol. */
    private static final int TYPE_BINARY_RESPONSE = 0x01;
    private static final int TYPE_TEXT_DUMP       = 0x02;

    public static void main(String[] args) throws IOException {
        String mode = (args.length > 0) ? args[0].toLowerCase() : "--send";
        if (mode.equals("--listen")) {
            runListener();
        } else {
            runSender();
        }
    }

    // ── SENDER MODE ───────────────────────────────────────────────────────────

    private static void runSender() throws IOException {
        System.out.println("=== ISO 8583 Sender — " + HOST + ":" + PORT + " ===");
        System.out.println("Paste raw hex and press Enter to send. Type 'exit' to quit.");
        System.out.println();

        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

        try (Socket socket    = new Socket(HOST, PORT);
             OutputStream out = socket.getOutputStream();
             InputStream  in  = socket.getInputStream()) {

            out.write(0x01); // Handshake: sender
            out.flush();

            // Background thread: continuously reads type-framed messages from the server
            // and prints them as they arrive, regardless of whether we are waiting to send.
            // Uses a simple volatile flag so the main thread can signal it to stop.
            final boolean[] running = {true};
            Thread receiver = new Thread(() -> {
                try {
                    while (running[0]) {
                        int type = in.read();
                        if (type == -1) break;

                        if (type == TYPE_BINARY_RESPONSE) {
                            // 0x01 — [2-byte big-endian len][EBCDIC ISO payload]
                            byte[] hdr     = readExact(in, 2);
                            int    len     = ((hdr[0] & 0xFF) << 8) | (hdr[1] & 0xFF);
                            byte[] payload = readExact(in, len);
                            System.out.println("\nResponse (0110) hex:");
                            System.out.println(toHex(payload));
                            System.out.println();

                        } else if (type == TYPE_TEXT_DUMP) {
                            // 0x02 — UTF-8 text lines terminated by "--END-OF-DUMP--\n"
                            System.out.println("\nDecoded fields:");
                            String line;
                            while ((line = readTextLine(in)) != null) {
                                if (line.equals("--END-OF-DUMP--")) break;
                                System.out.println(line);
                            }
                            System.out.println();
                        }
                        // Unknown type bytes are silently skipped for forward compatibility.
                    }
                } catch (EOFException | java.net.SocketException ignored) {
                    // Server closed the connection — normal on exit.
                } catch (IOException e) {
                    if (running[0]) {
                        System.err.println("[Receiver error] " + e.getMessage());
                    }
                }
            }, "iso-receiver");
            receiver.setDaemon(true);
            receiver.start();

            // Main thread: read hex from stdin and send to server.
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

                byte[] raw = hexToBytes(hex);
                out.write(raw);
                out.flush();
                System.out.println("Sent " + raw.length + " bytes.");
            }
        }
    }

    /**
     * Reads one LF-terminated text line from a raw InputStream byte-by-byte,
     * so we never over-read into the next framed message.
     * Returns null on EOF before any bytes are read.
     */
    private static String readTextLine(InputStream in) throws IOException {
        ByteArrayOutputStream lineBuf = new ByteArrayOutputStream(256);
        int b;
        boolean gotAny = false;
        while ((b = in.read()) != -1) {
            gotAny = true;
            if (b == '\n') break;
            if (b == '\r') continue;
            lineBuf.write(b);
        }
        if (!gotAny) return null;
        return lineBuf.toString(StandardCharsets.UTF_8);
    }

    // ── LISTENER MODE ─────────────────────────────────────────────────────────

    private static void runListener() throws IOException {
        System.out.println("=== ISO 8583 Listener — " + HOST + ":" + PORT + " ===");

        try (Socket socket = new Socket(HOST, PORT);
             InputStream in = socket.getInputStream()) {

            socket.getOutputStream().write(0x00); // Handshake: listener
            socket.getOutputStream().flush();
            System.out.println("Connected. Waiting for broadcasts...");

            // Listener receives only 0x02 text dump frames.
            while (true) {
                int type = in.read();
                if (type == -1) break;
                if (type == TYPE_TEXT_DUMP) {
                    String line;
                    while ((line = readTextLine(in)) != null) {
                        if (line.equals("--END-OF-DUMP--")) break;
                        System.out.println(line);
                    }
                    System.out.println();
                }
                // Unknown type bytes skipped.
            }
        }

        System.out.println("Connection closed.");
    }

    // ── UTILITIES ─────────────────────────────────────────────────────────────

    private static byte[] hexToBytes(String hex) {
        byte[] result = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            result[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
        }
        return result;
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02X", b));
        return sb.toString();
    }

    private static byte[] readExact(InputStream in, int n) throws IOException {
        byte[] buf    = new byte[n];
        int    offset = 0;
        while (offset < n) {
            int read = in.read(buf, offset, n - offset);
            if (read == -1) throw new EOFException("Stream ended at " + offset + "/" + n);
            offset += read;
        }
        return buf;
    }
}

