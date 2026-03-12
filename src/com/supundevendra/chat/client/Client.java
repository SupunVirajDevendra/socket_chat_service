package com.supundevendra.chat.client;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {

    private static final String SERVER_HOST = "localhost";
    private static final int    SERVER_PORT = 12346;

    private static final String ISO_PREFIX_PATTERN =
            "received\\s+message\\s+from\\s+(mastercard|visa|amex|discover)\\s*:?";

    public static void main(String[] args) {

        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("Connected to chat server.");

            BufferedReader serverIn  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter    serverOut = new PrintWriter(socket.getOutputStream(), true);

            Thread receiveThread = new Thread(() -> {
                try {
                    String line;
                    while ((line = serverIn.readLine()) != null) {
                        System.out.println(line);
                    }
                } catch (IOException e) {
                    System.out.println("Disconnected from server.");
                }
            });
            receiveThread.setDaemon(true);
            receiveThread.start();

            while (scanner.hasNextLine()) {
                String input = scanner.nextLine();

                if (input.equalsIgnoreCase("exit")) {
                    serverOut.println("exit");
                    break;
                }

                if (isIsoOnlyPrefix(input) && scanner.hasNextLine()) {
                    serverOut.println(input + "\n" + scanner.nextLine());
                } else {
                    serverOut.println(input);
                }
            }

            System.out.println("You left the chat.");

        } catch (IOException e) {
            System.out.println("Unable to connect to server at " + SERVER_HOST + ":" + SERVER_PORT);
        }
    }

    /**
     * Returns true when the line is a bare ISO network prefix with no trailing
     * payload on the same line (e.g. {@code "Received message from MASTERCARD:"}).
     * In this case the next line contains the IP and hex payload and must be
     * combined before sending.
     */
    private static boolean isIsoOnlyPrefix(String line) {
        if (line == null) return false;
        return line.trim().toLowerCase().matches(ISO_PREFIX_PATTERN);
    }
}
