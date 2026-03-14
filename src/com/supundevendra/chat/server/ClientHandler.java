package com.supundevendra.chat.server;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.GenericPackager;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientHandler implements Runnable {

    /** Shared packager loaded once from packager.xml — thread-safe for unpack/pack. */
    private static final GenericPackager PACKAGER;

    static {
        try (InputStream xml = ClientHandler.class.getResourceAsStream("/resources/packager.xml")) {
            if (xml == null) {
                throw new IllegalStateException("packager.xml not found on classpath");
            }
            PACKAGER = new GenericPackager(xml);
        } catch (Exception e) {
            throw new ExceptionInInitializerError("Cannot load packager.xml: " + e.getMessage());
        }
    }

    private final Socket socket;
    private OutputStream out;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    public String getAddress() {
        return socket.getRemoteSocketAddress().toString();
    }

    /** Sends a text dump to this client, prefixed with type byte 0x02. */
    public void sendText(String message) {
        try {
            byte[] data = (message + "\n").getBytes(StandardCharsets.UTF_8);
            synchronized (this) {
                out.write(0x02);   // type: text dump
                out.write(data);
                out.flush();
            }
        } catch (IOException e) {
            System.err.println("Send failed to " + getAddress() + ": " + e.getMessage());
        }
    }

    @Override
    public void run() {
        String addr = getAddress();
        try (InputStream in = socket.getInputStream();
             OutputStream os = socket.getOutputStream()) {

            this.out = os;

            // Handshake: 0x00 = listener, 0x01 = sender
            int mode = in.read();
            if (mode == -1) return;

            if (mode == 0x00) {
                runListener(in);
                return;
            }

            if (mode == 0x01) {
                runSender(in, addr);
            }

        } catch (EOFException e) {
            // Client disconnected cleanly — no action needed
        } catch (Exception e) {
            System.err.println("Error on client " + addr + ": " + e.getMessage());
        } finally {
            Server.removeClient(this);
        }
    }

    /** Listener mode: block until the client disconnects. */
    private void runListener(InputStream in) throws IOException {
        while (in.read() != -1) {
            // Discard any bytes; we only need to hold the connection open
            // so sendText() can push broadcast messages to this client.
        }
    }

    /** Sender mode: read ISO 8583 messages, broadcast field dump, reply with 0110. */
    private void runSender(InputStream in, String addr) throws Exception {
        while (true) {
            // Read 2-byte big-endian length header
            byte[] header = readExact(in, 2);
            int len = ((header[0] & 0xFF) << 8) | (header[1] & 0xFF);

            // Read ISO 8583 payload
            byte[] payload = readExact(in, len);

            // Unpack with jPOS
            ISOMsg msg = new ISOMsg();
            msg.setPackager(PACKAGER);
            PACKAGER.unpack(msg, payload);

            String dump = buildDump(msg, addr);

            // Send 0110 authorization response back to this sender only
            sendResponse(msg);

            // Broadcast decoded field dump to all other connected clients
            Server.broadcast(dump, this);
        }
    }

    /** Builds a human-readable dump of all ISO 8583 fields. */
    private String buildDump(ISOMsg msg, String addr) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("================================================================\n");
        sb.append(" ISO 8583 MESSAGE from ").append(addr).append("\n");
        sb.append("================================================================\n");
        sb.append(String.format("MTI       : %s%n", msg.getMTI()));
        for (int i = 2; i <= 128; i++) {
            if (msg.hasField(i)) {
                String value = fieldToReadable(msg, i);
                if (!value.isEmpty()) {
                    sb.append(String.format("Field %3d : %s%n", i, value));
                }
            }
        }
        sb.append("================================================================\n");
        sb.append("--END-OF-DUMP--");
        return sb.toString();
    }

    /** Packs a 0110 approval response and sends it prefixed with type byte 0x01 + 2-byte length header. */
    private void sendResponse(ISOMsg msg) throws Exception {
        ISOMsg resp = (ISOMsg) msg.clone();
        resp.setMTI("0110");
        resp.set(39, "00"); // Response code: approved

        byte[] respBytes = PACKAGER.pack(resp);
        byte[] frame = new byte[3 + respBytes.length];
        frame[0] = 0x01;   // type: binary ISO response
        frame[1] = (byte) ((respBytes.length >> 8) & 0xFF);
        frame[2] = (byte) (respBytes.length & 0xFF);
        System.arraycopy(respBytes, 0, frame, 3, respBytes.length);

        synchronized (this) {
            out.write(frame);
            out.flush();
        }
    }

    /** Reads exactly {@code n} bytes, handling TCP partial reads. */
    private static byte[] readExact(InputStream in, int n) throws IOException {
        byte[] buf = new byte[n];
        int offset = 0;
        while (offset < n) {
            int read = in.read(buf, offset, n - offset);
            if (read == -1) {
                throw new EOFException("Stream ended at " + offset + "/" + n);
            }
            offset += read;
        }
        return buf;
    }

    /**
     * Returns a human-readable value for an ISO 8583 field.
     * jPOS decodes EBCDIC internally during unpack, so getString() gives the
     * correct plain-text value for all text/numeric fields.
     * Truly binary fields (PIN block, MACs) return null from getString() —
     * those are shown as uppercase hex instead.
     */
    private static String fieldToReadable(ISOMsg msg, int fieldNum) {
        String value = msg.getString(fieldNum);
        if (value != null) {
            return value;
        }
        // Binary field (e.g. field 52 PIN block, field 64/128 MAC) — show as hex
        byte[] raw = msg.getBytes(fieldNum);
        return (raw != null) ? "[HEX] " + toHex(raw) : "";
    }

    /** Converts a byte array to an uppercase hex string. */
    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02X", b));
        return sb.toString();
    }
}
