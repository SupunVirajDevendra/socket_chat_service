package com.supundevendra.chat.protocol;

import com.supundevendra.chat.exception.ConfigurationException;
import org.jpos.iso.packager.GenericPackager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.InputStream;

// Loads the jPOS GenericPackager from packager.xml once and shares it across all threads
public final class PackagerLoader {

    private static final Logger log = LoggerFactory.getLogger(PackagerLoader.class);

    // Packager instance created once at class load time — fails fast on bad config
    private static final GenericPackager PACKAGER;

    static {
        log.info("Loading ISO 8583 packager from classpath:/packager.xml");
        try (InputStream xml = PackagerLoader.class.getResourceAsStream("/packager.xml")) {
            if (xml == null) {
                throw new ConfigurationException("packager.xml not found on classpath"); // file missing
            }
            PACKAGER = new GenericPackager(xml); // parse the XML field definitions
            log.info("ISO 8583 packager loaded successfully");
        } catch (ConfigurationException e) {
            log.error("Packager config error: {}", e.getMessage());
            throw e; // re-throw so startup fails immediately with a clear message
        } catch (Exception e) {
            log.error("Failed to load packager.xml", e);
            throw new ConfigurationException("Cannot load packager.xml: " + e.getMessage(), e);
        }
    }

    // Prevent instantiation — static utility class
    private PackagerLoader() {}

    // Returns the shared packager instance — thread-safe for concurrent pack/unpack
    public static GenericPackager getPackager() {
        return PACKAGER;
    }
}
