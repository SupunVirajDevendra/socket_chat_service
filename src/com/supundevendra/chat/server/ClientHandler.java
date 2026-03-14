package com.supundevendra.chat.server;

import com.supundevendra.chat.protocol.IsoMessageProcessor;
import org.jpos.iso.ISOMsg;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

// Manages one client connection on its own thread: handshake, framing, and lifecycle
public class ClientHandler implements Runnable {

    // Type byte prefix written before text-dump frames sent to this client
    private static final byte TYPE_TEXT_DUMP = 0x02;
    // Handshake byte that identifies a listener client
    private static final int  MODE_LISTENER  = 0x00;
    // Handshake byte that identifies a sender client
    private static final int  MODE_SENDER    = 0x01;

    private final Socket               socket;     // the accepted TCP connection
    private final ClientRegistry       registry;   // shared registry for broadcast and removal
    private final IsoMessageProcessor  processor;  // handles all ISO 8583 logic

    private OutputStream out; // output stream set once run() opens the socket streams

    // Constructor: stores the socket and registry, creates a processor for this session
    public ClientHandler(Socket socket, ClientRegistry registry) {
        this.socket    = socket;
        this.registry  = registry;
        this.processor = new IsoMessageProcessor();
    }

    // Returns the remote address as a string for logging
    public String getAddress() {
        return socket.getRemoteSocketAddress().toString();
    }

    // Sends a text-dump frame to this client; safe to call from any thread
    public void sendText(String message) {
        try {
            byte[] data = (message + "\n").getBytes(StandardCharsets.UTF_8); // encode as UTF-8
            synchronized (this) {
                if (out == null) return;   // stream not ready yet — client still in handshake
                out.write(TYPE_TEXT_DUMP); // type byte 0x02
                out.write(data);           // message content
                out.flush();               // send immediately
            }
        } catch (IOException e) {
            System.err.println("Send failed to " + getAddress() + ": " + e.getMessage());
        } catch (NullPointerException e) {
            // out became null between the null-check and write — client is disconnecting
        }
    }

    // Thread entry point: opens streams, reads handshake, delegates to sender or listener mode
    @Override
    public void run() {
        String addr = getAddress(); // capture address before socket closes
        try (InputStream in = socket.getInputStream();
             OutputStream os = socket.getOutputStream()) {

            this.out = os; // make the stream available to sendText()

            int mode = in.read(); // read the 1-byte handshake from the client
            if (mode == -1) return; // client disconnected before sending handshake

            if (mode == MODE_LISTENER) {
                runListener(in);       // hold connection open for broadcasts
            } else if (mode == MODE_SENDER) {
                runSender(in, addr);   // process incoming ISO messages
            } else {
                System.err.println("Unknown handshake 0x" + Integer.toHexString(mode) + " from " + addr);
            }

        } catch (EOFException e) {
            // client disconnected cleanly — no action needed
        } catch (Exception e) {
            System.err.println("Error on client " + addr + ": " + e.getMessage());
        } finally {
            synchronized (this) { out = null; }  // prevent sendText() writing to closed stream
            registry.removeClient(this);          // unregister and log the disconnect
        }
    }

    // Listener mode: block until client disconnects, discarding any bytes it sends
    private void runListener(InputStream in) throws IOException {
        while (in.read() != -1) {
            // drain any bytes — we only push outbound broadcasts to listeners
        }
    }

    // Sender mode: loop reading ISO frames, replying with 0110, and broadcasting the dump
    private void runSender(InputStream in, String addr) throws Exception {
        while (true) {
            byte[] payload = processor.readFrame(in);          // read next ISO 8583 frame
            ISOMsg  msg    = processor.unpack(payload);        // decode bytes into fields
            String  dump   = processor.buildDump(msg, addr);   // build human-readable dump

            synchronized (this) {
                processor.sendResponse(msg, out); // send 0110 approval back to this sender
            }

            registry.broadcast(dump, this); // push the dump to all other connected clients
        }
    }
}
