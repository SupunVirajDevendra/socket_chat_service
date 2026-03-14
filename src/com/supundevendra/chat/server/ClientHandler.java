package com.supundevendra.chat.server;

import com.supundevendra.chat.protocol.IsoMessageProcessor;
import org.jpos.iso.ISOMsg;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Handles a single client connection on its own thread.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Reading the 1-byte handshake to determine client mode</li>
 *   <li>Delegating to {@link #runListener} or {@link #runSender}</li>
 *   <li>Writing text-dump frames to this client via {@link #sendText}</li>
 *   <li>Cleaning up via {@link ClientRegistry} on disconnect</li>
 * </ul>
 *
 * <p>All ISO 8583 business logic is handled by {@link IsoMessageProcessor}.
 */
public class ClientHandler implements Runnable {

    /** Type byte prefix for outbound text-dump frames. */
    private static final byte TYPE_TEXT_DUMP = 0x02;

    /** Handshake byte sent by listener clients. */
    private static final int MODE_LISTENER = 0x00;

    /** Handshake byte sent by sender clients. */
    private static final int MODE_SENDER = 0x01;

    private final Socket           socket;
    private final ClientRegistry   registry;
    private final IsoMessageProcessor processor;

    private OutputStream out;

    public ClientHandler(Socket socket, ClientRegistry registry) {
        this.socket     = socket;
        this.registry   = registry;
        this.processor  = new IsoMessageProcessor();
    }

    /** Returns the remote address of this client as a display string. */
    public String getAddress() {
        return socket.getRemoteSocketAddress().toString();
    }

    /**
     * Sends a text dump frame to this client.
     * Safe to call from any thread. Silently drops the message if the
     * output stream is not yet initialized or has already been closed.
     */
    public void sendText(String message) {
        try {
            byte[] data = (message + "\n").getBytes(StandardCharsets.UTF_8);
            synchronized (this) {
                if (out == null) return; // not yet ready — client still in handshake
                out.write(TYPE_TEXT_DUMP);
                out.write(data);
                out.flush();
            }
        } catch (IOException e) {
            System.err.println("Send failed to " + getAddress() + ": " + e.getMessage());
        } catch (NullPointerException e) {
            // out became null between the null-check and the write — client is disconnecting
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

            if (mode == MODE_LISTENER) {
                runListener(in);
            } else if (mode == MODE_SENDER) {
                runSender(in, addr);
            } else {
                System.err.println("Unknown handshake byte 0x"
                        + Integer.toHexString(mode) + " from " + addr);
            }

        } catch (EOFException e) {
            // Client disconnected cleanly — no action needed
        } catch (Exception e) {
            System.err.println("Error on client " + addr + ": " + e.getMessage());
        } finally {
            synchronized (this) { out = null; }
            registry.removeClient(this);
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Listener mode: hold the connection open so {@link #sendText} can push
     * broadcast frames. Discards any bytes the client sends.
     */
    private void runListener(InputStream in) throws IOException {
        while (in.read() != -1) {
            // Discard incoming bytes — we only push outbound broadcasts
        }
    }

    /**
     * Sender mode: read ISO 8583 frames in a loop, broadcast a field dump to
     * all other clients, and reply with a {@code 0110} approval response.
     */
    private void runSender(InputStream in, String addr) throws Exception {
        while (true) {
            byte[] payload = processor.readFrame(in);
            ISOMsg  msg    = processor.unpack(payload);
            String  dump   = processor.buildDump(msg, addr);

            synchronized (this) {
                processor.sendResponse(msg, out);
            }

            registry.broadcast(dump, this);
        }
    }
}
