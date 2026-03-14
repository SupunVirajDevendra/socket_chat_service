package com.supundevendra.chat.server;

import com.supundevendra.chat.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

// Entry point: opens the server socket and dispatches each connection to a fixed thread pool
public class Server {

    private static final Logger log = LoggerFactory.getLogger(Server.class); // per-class logger

    public static void main(String[] args) throws IOException {
        AppConfig config = AppConfig.getInstance(); // load host/port/maxThreads from config.properties
        int port       = config.getPort();          // server listen port
        int maxThreads = config.getMaxThreads();    // cap on concurrent client handler threads

        ClientRegistry registry = ClientRegistry.getInstance(); // shared client registry singleton

        // Fixed thread pool: bounds the number of simultaneous client sessions
        ExecutorService pool = Executors.newFixedThreadPool(maxThreads);

        // JVM shutdown hook: gives in-flight handlers a chance to finish cleanly
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutdown signal received — stopping thread pool");
            pool.shutdown(); // stop accepting new tasks
            try {
                if (!pool.awaitTermination(30, TimeUnit.SECONDS)) { // wait up to 30 s
                    log.warn("Pool did not terminate in time — forcing shutdown");
                    pool.shutdownNow(); // cancel remaining tasks
                }
            } catch (InterruptedException e) {
                pool.shutdownNow(); // interrupted while waiting — force immediate shutdown
                Thread.currentThread().interrupt(); // restore interrupted status
            }
            log.info("Server shut down.");
        }, "shutdown-hook")); // name the hook thread for easier debugging

        log.info("ISO 8583 server listening on port {}", port);
        try (ServerSocket serverSocket = new ServerSocket(port)) { // bind to the configured port
            while (!pool.isShutdown()) {
                Socket socket = serverSocket.accept(); // block until a client connects
                log.info("Client connected: {}", socket.getRemoteSocketAddress());

                ClientHandler handler = new ClientHandler(socket, registry); // create handler for this connection
                registry.addClient(handler);   // register before submitting to pool
                pool.submit(handler);          // hand off to the thread pool — no raw Thread.start()
            }
        }
    }
}
