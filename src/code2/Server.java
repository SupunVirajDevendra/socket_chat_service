package code2;

import java.io.*;
import java.net.*;
import java.util.*;

public class Server {

    private static final int PORT = 12346;

    // Store connected clients
    static List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) {

        System.out.println("Server started...");
        logMessage("Server started...");

        try (ServerSocket s2 = new ServerSocket(PORT)) {

            while (true) {
                Socket s3 = s2.accept();
                ClientHandler handler = new ClientHandler(s3);
                clients.add(handler);
                new Thread(handler).start();
            }

        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
            logMessage("Server error: " + e.getMessage());
        }
    }

    // Broadcast to all clients
    public static void broadcast(String message, ClientHandler sender) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                if (client != sender) {
                    client.sendMessage(message);
                }
            }
        }
    }

    // Private message
    public static void privateMessage(String targetUser, String message) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                if (client.getUsername().equalsIgnoreCase(targetUser)) {
                    client.sendMessage("[Private] " + message);
                    break;
                }
            }
        }
    }

    // Remove client
    public static void removeClient(ClientHandler client) {
        clients.remove(client);
    }

    // Log to file
    private static void logMessage(String message) {
        try (FileWriter chatlog = new FileWriter("chat.txt", true)) {
            chatlog.write(message + "\n");
        } catch (IOException e) {
            System.out.println("Logging error.");
        }
    }
}