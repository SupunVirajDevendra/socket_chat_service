package com.supundevendra.chat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

// Entry point: opens the server socket and spawns a thread per incoming connection
public class Server {

    // Port the server listens on — matches the ISO 8583 standard port
    private static final int PORT = 8583;

    public static void main(String[] args) throws IOException {
        ClientRegistry registry = ClientRegistry.getInstance(); // get the shared client registry

        System.out.println("ISO 8583 server listening on port " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) { // bind to the port
            while (true) {
                Socket socket = serverSocket.accept();             // block until a client connects
                System.out.println("Client connected: " + socket.getRemoteSocketAddress());

                ClientHandler handler = new ClientHandler(socket, registry); // create handler for this connection
                registry.addClient(handler);                                  // register before starting thread
                new Thread(handler).start();                                  // run handler on its own thread
            }
        }
    }
}
