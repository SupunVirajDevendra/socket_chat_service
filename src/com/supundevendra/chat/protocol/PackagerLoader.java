package com.supundevendra.chat.protocol;

import org.jpos.iso.packager.GenericPackager;
import java.io.InputStream;

// Loads the jPOS GenericPackager from packager.xml once and shares it across all threads
public final class PackagerLoader {

    // Packager instance created once at class load time
    private static final GenericPackager PACKAGER;

    static {
        // Open packager.xml from the classpath
        try (InputStream xml = PackagerLoader.class.getResourceAsStream("/resources/packager.xml")) {
            if (xml == null) {
                throw new IllegalStateException("packager.xml not found on classpath"); // file missing
            }
            PACKAGER = new GenericPackager(xml); // parse the XML field definitions
        } catch (Exception e) {
            throw new ExceptionInInitializerError("Cannot load packager.xml: " + e); // crash fast on bad config
        }
    }

    // Prevent instantiation — static utility class
    private PackagerLoader() {}

    // Returns the shared packager instance — thread-safe for concurrent pack/unpack
    public static GenericPackager getPackager() {
        return PACKAGER;
    }
}
