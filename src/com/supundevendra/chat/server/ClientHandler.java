package com.supundevendra.chat.server;

import com.supundevendra.iso.ISOMessageParser;

import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {

    private static final String DEFAULT_USERNAME = "anonymous";
    private static final String SEPARATOR = "================================================================";

    private final Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username = DEFAULT_USERNAME;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        try {
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            System.out.println("Failed to initialise client streams.");
        }
    }

    public String getUsername() {
        return username;
    }

    @Override
    public void run() {
        try {
            username = readUsername();

            Server.broadcast(username + " joined the chat.", this);
            Server.logMessage(username + " connected.");

            String message;
            while ((message = in.readLine()) != null) {
                if (message.equalsIgnoreCase("exit")) {
                    Server.logMessage(username + " disconnected.");
                    break;
                }
                route(message);
            }

        } catch (IOException e) {
            System.out.println("Connection error with " + username);
            Server.logMessage("Connection error with " + username);
        } finally {
            cleanup();
        }
    }

    private String readUsername() throws IOException {
        out.println("Enter your username:");
        String name = in.readLine();
        return (name == null || name.trim().isEmpty()) ? DEFAULT_USERNAME : name.trim();
    }

    private void route(String message) {
        if (message.startsWith("@")) {
            handleDirectedMessage(message);
        } else if (isIsoHexMessage(message)) {
            String decoded = parseIsoAndFormat(message, username, null);
            Server.broadcast(decoded, this);
            out.println("[Server] ISO 8583 message decoded and broadcast to all clients.");
        } else {
            Server.broadcast(username + " : " + message, this);
        }
    }

    private void handleDirectedMessage(String message) {
        String[] parts = message.split(" ", 2);
        if (parts.length < 2) return;

        String targetUser = parts[0].substring(1);
        String payload    = parts[1].trim();

        if (isIsoHexMessage(payload)) {
            String decoded = parseIsoAndFormat(payload, username, targetUser);
            Server.sendToUser(targetUser, decoded);
            out.println("[Server] ISO 8583 message decoded and sent privately to " + targetUser + ".");
        } else {
            Server.privateMessage(targetUser, username + " (private): " + payload);
        }
    }

    private void cleanup() {
        try {
            Server.removeClient(this);
            Server.broadcast(username + " left the chat.", this);
            socket.close();
        } catch (IOException e) {
            System.out.println("Error closing connection for " + username);
            Server.logMessage("Error closing connection for " + username);
        }
    }

    /**
     * Returns true if the message contains an ISO 8583 hex payload in any of
     * the supported formats:
     * <ul>
     *   <li>Pure hex string: {@code 016DF0F1...}</li>
     *   <li>Single-line prefixed: {@code Received message from MASTERCARD: 192.168.1.34 : 016D...}</li>
     *   <li>Multi-line prefixed: {@code Received message from MASTERCARD:\n192.168.1.34 : 016D...}</li>
     *   <li>IP-prefixed: {@code 192.168.1.34 : 016D...}</li>
     * </ul>
     */
    private boolean isIsoHexMessage(String message) {
        if (message == null) return false;
        if (message.matches("[0-9A-Fa-f]{20,}")) return true;

        String lower = message.trim().toLowerCase();
        if (lower.startsWith("received message from mastercard") ||
            lower.startsWith("received message from visa")       ||
            lower.startsWith("received message from amex")       ||
            lower.startsWith("received message from discover")) {
            return true;
        }

        for (String token : message.split("[\\s:]+")) {
            if (token.matches("[0-9A-Fa-f]{20,}")) return true;
        }

        return false;
    }

    /**
     * Parses the ISO 8583 hex payload and wraps the decoded output with a
     * contextual header identifying the sender and, optionally, the private recipient.
     *
     * @param hex    raw ISO 8583 hex string (or any supported prefixed format)
     * @param sender username of the client who sent the message
     * @param target recipient username for private delivery; {@code null} for broadcast
     * @return fully formatted decoded output ready to send
     */
    private String parseIsoAndFormat(String hex, String sender, String target) {
        try {
            ISOMessageParser parser = new ISOMessageParser();
            String decoded  = parser.parseToString(hex);
            String network  = extractNetwork(decoded);

            String label = (target != null)
                    ? sender + " sent a " + network + " ISO 8583 message privately to " + target
                    : sender + " sent a " + network + " ISO 8583 message";

            return SEPARATOR + "\n [" + label + "]\n" + SEPARATOR + decoded;

        } catch (Exception e) {
            return "[ISO Parse Error from " + sender + "]: " + e.getMessage();
        }
    }

    private String extractNetwork(String decoded) {
        if (decoded.contains("Card Network    : MASTERCARD")) return "MASTERCARD";
        if (decoded.contains("Card Network    : VISA"))       return "VISA";
        if (decoded.contains("Card Network    : AMEX"))       return "AMEX";
        if (decoded.contains("Card Network    : DISCOVER"))   return "DISCOVER";
        return "ISO 8583";
    }

    /**
     * Delivers a message to this client line by line so that the client's
     * {@link java.io.BufferedReader#readLine()} receives each line intact.
     */
    public void sendMessage(String message) {
        if (message == null) return;
        if (message.contains("\n")) {
            for (String line : message.split("\\r?\\n", -1)) {
                out.println(line);
            }
        } else {
            out.println(message);
        }
    }
}
