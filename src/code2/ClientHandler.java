package code2;

import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        try {
            in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(
                    socket.getOutputStream(), true);
        } catch (IOException e) {
            System.out.println("Client setup error.");
        }
    }

    public String getUsername() {
        return username;
    }

    @Override
    public void run() {
        try {

            // Ask username
            out.println("Enter your username:");
            username = in.readLine();

            if (username == null || username.trim().isEmpty()) {
                username = "anura";
            }

            System.out.println(username + " connected.");
            Server.broadcast(username + " joined the chat.", this);
            logMessage(username + " connected.");

            String message;

            while ((message = in.readLine()) != null) {

                if (message.equalsIgnoreCase("exit")) {
                    System.out.println(username + " disconnected.");
                    logMessage(username + " disconnected.");
                    break;
                }

                // Private message
                if (message.startsWith("@")) {
                    String[] parts = message.split(" ", 2);
                    if (parts.length == 2) {
                        String targetUser = parts[0].substring(1);
                        Server.privateMessage(
                                targetUser,
                                username + ": " + parts[1]
                        );
                    }
                } else {
                    String formatted =
                             username + " : " + message;
                    Server.broadcast(formatted, this);
                }
            }

        } catch (IOException e) {
            System.out.println("Connection error with " + username);
            logMessage("Connection error with " + username);

        } finally {
            try {
                Server.removeClient(this);
                Server.broadcast(username + " left the chat.", this);
                socket.close();
            } catch (IOException e) {
                System.out.println("Error closing connection.");
                logMessage("Error closing connection.");
            }
        }
    }

    // Log to file
    private static void logMessage(String message) {
        try (FileWriter chatlog = new FileWriter("chat.txt", true)) {
            chatlog.write(message + "\n");
        } catch (IOException e) {
            System.out.println(e.getMessage());
            logMessage("Logging error: " + e.getMessage());
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }
}