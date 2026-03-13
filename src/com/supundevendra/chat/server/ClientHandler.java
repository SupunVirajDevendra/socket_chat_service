package com.supundevendra.chat.server;

import com.supundevendra.chat.parser.ISOMessageParser;

import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {

    // Default username if user does not provide one
    private static final String DEFAULT_USERNAME = "anonymous";
    // Separator used to display decoded ISO messages clearly
    private static final String SEPARATOR = "================================================================";

    private final Socket socket;// Socket connection for this client
    private BufferedReader in;// Input stream to read messages from client
    private PrintWriter out; // Output stream to send messages to client
    private String username = DEFAULT_USERNAME;  // Username of this client

    // Constructor: sets up communication streams for the client
    public ClientHandler(Socket socket) {
        this.socket = socket;
        try {
            // Read data coming from client
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            // Send data to client
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            System.out.println("Failed to initialise client streams.");
        }
    }

    // Returns the client's username
    public String getUsername() {
        return username;
    }

    // Main thread execution for each connected client
    @Override
    public void run() {
        try {
            // Ask client for username
            username = readUsername();

            // Notify all users that someone joined
            Server.broadcast(username + " joined the chat.", this);
            // Log connection event
            Server.logMessage(username + " connected.");

            String message;

            // Continuously read messages from client
            while ((message = in.readLine()) != null) {
                // If user types "exit", disconnect
                if (message.equalsIgnoreCase("exit")) {
                    Server.logMessage(username + " disconnected.");
                    break;
                }
                // Route message depending on type
                route(message);
            }

        } catch (IOException e) {
            System.out.println("Connection error with " + username);
            Server.logMessage("Connection error with " + username);
        } finally {
            // Always cleanup when client disconnects
            cleanup();
        }
    }

    // Reads username from client input
    private String readUsername() throws IOException {
        out.println("Enter your username:");
        String name = in.readLine();
        // If username is empty, use default
        return (name == null || name.trim().isEmpty()) ? DEFAULT_USERNAME : name.trim();
    }

    // Decides how to handle the received message
    private void route(String message) {

        // Private message format: @username message
        if (message.startsWith("@")) {
            handleDirectedMessage(message);

            // If message looks like ISO8583 hex message
        } else if (isIsoHexMessage(message)) {
            // Decode ISO message
            String decoded = parseIsoAndFormat(message, username, null);
            // Broadcast decoded message
            Server.broadcast(decoded, this);
            out.println("[Server] ISO 8583 message decoded and broadcast to all clients.");

            // Normal chat message
        } else {
            Server.broadcast(username + " : " + message, this);
        }
    }

    // Handles private messages sent to a specific user
    private void handleDirectedMessage(String message) {

        // Split message into username and actual message
        String[] parts = message.split(" ", 2);
        if (parts.length < 2) return;

        // Extract target username
        String targetUser = parts[0].substring(1);
        // Extract message body
        String payload    = parts[1].trim();

        // If payload is ISO message
        if (isIsoHexMessage(payload)) {
            String decoded = parseIsoAndFormat(payload, username, targetUser);

            // Send decoded ISO message privately
            Server.sendToUser(targetUser, decoded);
            out.println("[Server] ISO 8583 message decoded and sent privately to " + targetUser + ".");
        } else {
            // Send normal private message
            Server.sendToUser(targetUser, username + " (private): " + payload);
        }
    }

    // Cleans up resources when client disconnects
    private void cleanup() {
        try {

            // Remove client from server list
            Server.removeClient(this);

            // Notify other clients
            Server.broadcast(username + " left the chat.", this);

            // Close socket connection
            socket.close();

        } catch (IOException e) {
            System.out.println("Error closing connection for " + username);
            Server.logMessage("Error closing connection for " + username);
        }
    }

    // Detects whether a message is an ISO8583 hex message
    private boolean isIsoHexMessage(String message) {
        if (message == null) return false;

        // Case 1: message is a long hex string
        if (message.matches("[0-9A-Fa-f]{20,}")) return true;
        String lower = message.trim().toLowerCase();

        // Case 2: message starts with network prefix
        if (lower.startsWith("received message from mastercard") ||
            lower.startsWith("received message from visa")       ||
            lower.startsWith("received message from amex")       ||
            lower.startsWith("received message from discover")) {
            return true;
        }

        // Case 3: message contains hex token somewhere
        for (String token : message.split("[\\s:]+")) {
            if (token.matches("[0-9A-Fa-f]{20,}")) return true;
        }

        return false;
    }

    // Parses ISO8583 message and formats it for display
    private String parseIsoAndFormat(String hex, String sender, String target) {
        try {
            // Create parser
            ISOMessageParser parser = new ISOMessageParser();
            // Decode ISO message
            String decoded  = parser.parseToString(hex);
            // Detect card network
            String network  = extractNetwork(decoded);

            // Build display label
            String label = (target != null)
                    ? sender + " sent a " + network + " ISO 8583 message privately to " + target
                    : sender + " sent a " + network + " ISO 8583 message";

            // Return formatted result
            return SEPARATOR + "\n [" + label + "]\n" + SEPARATOR + decoded;

        } catch (Exception e) {
            // If parsing fails
            return "[ISO Parse Error from " + sender + "]: " + e.getMessage();
        }
    }

    // Detects card network from decoded message text
    private String extractNetwork(String decoded) {
        if (decoded.contains("Card Network    : MASTERCARD")) return "MASTERCARD";
        if (decoded.contains("Card Network    : VISA"))       return "VISA";
        if (decoded.contains("Card Network    : AMEX"))       return "AMEX";
        if (decoded.contains("Card Network    : DISCOVER"))   return "DISCOVER";
        return "ISO 8583";
    }

    /*
     * Sends message to this client.
     * If message has multiple lines, send them line-by-line
     * so the client can read them properly.
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
