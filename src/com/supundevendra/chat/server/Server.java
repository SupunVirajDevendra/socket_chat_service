package com.supundevendra.chat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * ISO 8583 chat server entry point.
 *
 * <p>Listens on {@code PORT} for incoming TCP connections. Each connection
 * is handed to a {@link ClientHandler} running on its own thread.
 * The shared {@link ClientRegistry} singleton tracks all active clients
 * and handles broadcast / removal.
 */
public class Server {

    private static final int PORT = 8583;

    public static void main(String[] args) throws IOException {
        ClientRegistry registry = ClientRegistry.getInstance();

        System.out.println("ISO 8583 server listening on port " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Client connected: " + socket.getRemoteSocketAddress());

                ClientHandler handler = new ClientHandler(socket, registry);
                registry.addClient(handler);
                new Thread(handler).start();
            }
        }
    }
}
