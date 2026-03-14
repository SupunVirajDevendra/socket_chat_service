package com.supundevendra.chat.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Singleton that tracks all active client connections and provides broadcast/remove operations
public class ClientRegistry {

    // Single shared instance created eagerly at class load time
    private static final ClientRegistry INSTANCE = new ClientRegistry();

    // Thread-safe list of currently connected clients
    private final List<ClientHandler> clients =
            Collections.synchronizedList(new ArrayList<>());

    // Private constructor prevents external instantiation
    private ClientRegistry() {}

    // Returns the single shared ClientRegistry instance
    public static ClientRegistry getInstance() {
        return INSTANCE;
    }

    // Registers a newly accepted client so it can receive broadcasts
    public void addClient(ClientHandler handler) {
        clients.add(handler);
    }

    // Removes a client from the registry when it disconnects and logs the event
    public void removeClient(ClientHandler handler) {
        clients.remove(handler);                                        // unregister the client
        System.out.println("Client disconnected: " + handler.getAddress()); // log disconnect
    }

    // Sends message to every registered client except the original sender
    public void broadcast(String message, ClientHandler sender) {
        synchronized (clients) {                    // lock to prevent concurrent modification
            for (ClientHandler client : clients) {  // iterate all connected clients
                if (client != sender) {             // skip the client who sent the message
                    client.sendText(message);       // push the dump to this listener
                }
            }
        }
    }
}
