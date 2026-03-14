package com.supundevendra.chat.config;

import com.supundevendra.chat.exception.ConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

// Singleton that loads config.properties from the classpath and exposes typed getters
// All values fall back to safe defaults if the property is absent or the file is missing
public class AppConfig {

    // Classpath location of the properties file
    private static final String CONFIG_FILE = "/config.properties";

    // Default values used when a property is missing
    private static final String  DEFAULT_HOST        = "localhost";
    private static final int     DEFAULT_PORT        = 8583;
    private static final int     DEFAULT_MAX_THREADS = 50;

    // Single shared instance — created eagerly at class load time
    private static final AppConfig INSTANCE = new AppConfig();

    // Loaded properties — never null after construction
    private final Properties props;

    // Private constructor: loads config.properties; falls back gracefully if absent
    private AppConfig() {
        props = new Properties();
        try (InputStream in = AppConfig.class.getResourceAsStream(CONFIG_FILE)) {
            if (in != null) {
                props.load(in); // parse key=value pairs from the file
            }
            // file absent is not fatal — all getters have defaults
        } catch (IOException e) {
            throw new ConfigurationException("Failed to read " + CONFIG_FILE, e);
        }
    }

    // Returns the single shared AppConfig instance
    public static AppConfig getInstance() {
        return INSTANCE;
    }

    // Returns the server hostname; defaults to "localhost"
    public String getHost() {
        return props.getProperty("server.host", DEFAULT_HOST);
    }

    // Returns the server port number; defaults to 8583
    public int getPort() {
        String raw = props.getProperty("server.port", String.valueOf(DEFAULT_PORT));
        try {
            int port = Integer.parseInt(raw.trim()); // parse the string value
            if (port < 1 || port > 65535) {
                throw new ConfigurationException("server.port out of range: " + port);
            }
            return port;
        } catch (NumberFormatException e) {
            throw new ConfigurationException("server.port is not a valid integer: " + raw, e);
        }
    }

    // Returns the maximum number of concurrent client handler threads; defaults to 50
    public int getMaxThreads() {
        String raw = props.getProperty("server.maxThreads", String.valueOf(DEFAULT_MAX_THREADS));
        try {
            int n = Integer.parseInt(raw.trim()); // parse the string value
            if (n < 1) {
                throw new ConfigurationException("server.maxThreads must be >= 1, got: " + n);
            }
            return n;
        } catch (NumberFormatException e) {
            throw new ConfigurationException("server.maxThreads is not a valid integer: " + raw, e);
        }
    }
}
