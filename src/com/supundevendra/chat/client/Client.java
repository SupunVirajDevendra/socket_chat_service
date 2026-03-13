package com.supundevendra.chat.client;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {

    // Server address (localhost = same computer)
    private static final String SERVER_HOST = "localhost";
    // Server port number
    private static final int    SERVER_PORT = 12346;

    // Pattern used to detect ISO network prefix messages
    // Example: "Received message from MASTERCARD:"
    private static final String ISO_PREFIX_PATTERN =
            "received\\s+message\\s+from\\s+(mastercard|visa|amex|discover)\\s*:?";

    public static void main(String[] args) {

        // Connect to server and open keyboard input
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("Connected to chat server.");

            // Read messages coming from server
            BufferedReader serverIn  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            // Send messages to server
            PrintWriter    serverOut = new PrintWriter(socket.getOutputStream(), true);

            /*
             * Background thread that continuously listens for
             * messages from the server and prints them.
             */
            Thread receiveThread = new Thread(() -> {
                try {
                    String line;
                    // Read messages sent by server
                    while ((line = serverIn.readLine()) != null) {
                        System.out.println(line);
                    }
                } catch (IOException e) {
                    System.out.println("Disconnected from server.");
                }
            });
            // Daemon thread stops automatically when program ends
            receiveThread.setDaemon(true);
            receiveThread.start();

            /*
             * Main loop that reads user input from keyboard
             * and sends it to the server.
             */
            while (scanner.hasNextLine()) {
                String input = scanner.nextLine();

                // User wants to exit
                if (input.equalsIgnoreCase("exit")) {
                    serverOut.println("exit");
                    break;
                }

                /*
                 * If the line is only an ISO prefix like:
                 * "Received message from MASTERCARD:"
                 *
                 * then read the next line (which contains
                 * the hex message) and send both together.
                 */
                if (isIsoOnlyPrefix(input) && scanner.hasNextLine()) {
                    serverOut.println(input + "\n" + scanner.nextLine());
                } else {
                    // Normal chat message
                    serverOut.println(input);
                }
            }

            System.out.println("You left the chat.");

        } catch (IOException e) {
            // Happens if server is not running
            System.out.println("Unable to connect to server at " + SERVER_HOST + ":" + SERVER_PORT);
        }
    }

    /*
     * Checks if the input line is only the ISO message prefix.
     *
     * Example:
     * "Received message from MASTERCARD:"
     *
     * If true, the next line will contain the hex message
     * and both lines must be sent together to the server.
     */
    private static boolean isIsoOnlyPrefix(String line) {
        if (line == null) return false;
        return line.trim().toLowerCase().matches(ISO_PREFIX_PATTERN);
    }
}
