package code2;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {

    private static final String IP = "localhost";
    private static final int PORT = 12346;

    public static void main(String[] args) {

        try (Socket s = new Socket(IP, PORT)) {

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(s.getInputStream()));

            PrintWriter out = new PrintWriter(
                    s.getOutputStream(), true);

            Scanner scanner = new Scanner(System.in);

            // Receive messages
            Thread receiveThread = new Thread(() -> {
                try {
                    String response;
                    while ((response = in.readLine()) != null) {
                        System.out.println(response);
                    }
                } catch (IOException e) {
                    System.out.println("Disconnected from server.");
                }
            });

            receiveThread.start();

            // Send messages
            while (true) {
                String userInput = scanner.nextLine();

                if (userInput.equalsIgnoreCase("exit")) {
                    out.println("exit");
                    break;
                }

                out.println(userInput);
            }

            System.out.println("You left the chat.");

        } catch (IOException e) {
            System.out.println("Unable to connect to server.");
        }
    }
}
