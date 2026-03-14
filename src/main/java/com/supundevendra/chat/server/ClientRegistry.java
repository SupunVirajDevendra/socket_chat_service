package com.supundevendra.chat.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Singleton that tracks all active client connections and provides broadcast/remove operations
public class ClientRegistry {

    private static final Logger log = LoggerFactory.getLogger(ClientRegistry.class);

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
        log.info("Client registered: {} — total connected: {}", handler.getAddress(), clients.size());
    }

    // Removes a client from the registry when it disconnects
    public void removeClient(ClientHandler handler) {
        clients.remove(handler); // unregister the client
        log.info("Client disconnected: {} — total connected: {}", handler.getAddress(), clients.size());
    }

    // Sends message to every registered client except the original sender
    public void broadcast(String message, ClientHandler sender) {
        synchronized (clients) {                     // lock to prevent concurrent modification
            for (ClientHandler client : clients) {   // iterate all connected clients
                if (client != sender) {              // skip the client who sent the message
                    client.sendText(message);        // push the dump to this listener
                }
            }
        }
        log.debug("Broadcast sent to {} client(s)", clients.size() - 1);
    }
}
