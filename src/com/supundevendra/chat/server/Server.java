package com.supundevendra.chat.server;

import java.io.*;
import java.net.*;
import java.util.*;

public class Server {

    // Port number where the server will listen for client connections
    private static final int PORT = 12346;

    // List that stores all connected clients (thread-safe list)
    static List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) {
        // Print message when server starts
        System.out.println("Server started on port " + PORT);
        // Write server start event to log file
        logMessage("Server started.");

        // Create server socket and start listening for clients
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            // Server runs forever waiting for new clients
            while (true) {
                // Accept a client connection
                Socket clientSocket = serverSocket.accept();
                // Create a handler object for the new client
                ClientHandler handler = new ClientHandler(clientSocket);
                // Add client to the connected clients list
                clients.add(handler);
                // Run client handler in a new thread (so multiple clients can connect)
                new Thread(handler).start();
            }
        } catch (IOException e) {
            // If server fails, print and log error
            System.out.println("Server error: " + e.getMessage());
            logMessage("Server error: " + e.getMessage());
        }
    }

    // Send a message to all clients except the sender
    public static void broadcast(String message, ClientHandler sender) {
        synchronized (clients) { // lock list while iterating
            for (ClientHandler client : clients) {
                // Do not send message back to sender
                if (client != sender) {
                    client.sendMessage(message);
                }
            }
        }
    }

    // Send a private message to a specific user
    public static void sendToUser(String targetUser, String message) {
        synchronized (clients) {// lock list during search
            for (ClientHandler client : clients) {
                // Find client with matching username
                if (client.getUsername().equalsIgnoreCase(targetUser)) {
                    // Send message to that user
                    client.sendMessage(message);
                    break;
                }
            }
        }
    }

    // Remove client when they disconnect
    public static void removeClient(ClientHandler client) {
        clients.remove(client);
    }

    // Write server events or errors into chat.txt file
    static void logMessage(String message) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("chat.txt", true))) {
            // Append message to log file
            writer.write(message + "\n");
        } catch (IOException e) {
            // If logging fails
            System.out.println("Failed to write to log file.");
        }
    }
}
