package com.supundevendra.chat.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Singleton registry that tracks all active {@link ClientHandler} connections.
 *
 * <p>Provides thread-safe operations for adding, removing, and broadcasting
 * to connected clients. Use {@link #getInstance()} to obtain the shared instance.
 */
public class ClientRegistry {

    private static final ClientRegistry INSTANCE = new ClientRegistry();

    private final List<ClientHandler> clients =
            Collections.synchronizedList(new ArrayList<>());

    private ClientRegistry() {}

    /** Returns the single shared {@link ClientRegistry} instance. */
    public static ClientRegistry getInstance() {
        return INSTANCE;
    }

    /** Registers a newly connected client. */
    public void addClient(ClientHandler handler) {
        clients.add(handler);
    }

    /**
     * Removes a client from the registry on disconnect.
     * Safe to call from any thread, including the client's own handler thread.
     */
    public void removeClient(ClientHandler handler) {
        clients.remove(handler);
        System.out.println("Client disconnected: " + handler.getAddress());
    }

    /**
     * Broadcasts {@code message} to every registered client except {@code sender}.
     * Each send is performed under the registry lock to avoid concurrent modification.
     */
    public void broadcast(String message, ClientHandler sender) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                if (client != sender) {
                    client.sendText(message);
                }
            }
        }
    }
}
