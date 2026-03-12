package com.supundevendra.chat.server;

import java.io.*;
import java.net.*;
import java.util.*;

public class Server {

    private static final int PORT = 12346;

    static List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) {
        System.out.println("Server started on port " + PORT);
        logMessage("Server started.");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket);
                clients.add(handler);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
            logMessage("Server error: " + e.getMessage());
        }
    }

    public static void broadcast(String message, ClientHandler sender) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                if (client != sender) {
                    client.sendMessage(message);
                }
            }
        }
    }

    public static void privateMessage(String targetUser, String message) {
        sendToUser(targetUser, message);
    }

    public static void sendToUser(String targetUser, String message) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                if (client.getUsername().equalsIgnoreCase(targetUser)) {
                    client.sendMessage(message);
                    break;
                }
            }
        }
    }

    public static void removeClient(ClientHandler client) {
        clients.remove(client);
    }

    static void logMessage(String message) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("chat.txt", true))) {
            writer.write(message + "\n");
        } catch (IOException e) {
            System.out.println("Failed to write to log file.");
        }
    }
}
