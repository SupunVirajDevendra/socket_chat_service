package com.supundevendra.chat.protocol;

import org.jpos.iso.packager.GenericPackager;
import java.io.InputStream;

/**
 * Loads the jPOS {@link GenericPackager} from {@code packager.xml} once at
 * class-initialization time. All callers share the same immutable instance,
 * which is thread-safe for concurrent pack/unpack operations.
 */
public final class PackagerLoader {

    private static final GenericPackager PACKAGER;

    static {
        try (InputStream xml = PackagerLoader.class.getResourceAsStream("/resources/packager.xml")) {
            if (xml == null) {
                throw new IllegalStateException("packager.xml not found on classpath");
            }
            PACKAGER = new GenericPackager(xml);
        } catch (Exception e) {
            throw new ExceptionInInitializerError("Cannot load packager.xml: " + e);
        }
    }

    private PackagerLoader() {}

    /** Returns the shared, fully initialized {@link GenericPackager}. */
    public static GenericPackager getPackager() {
        return PACKAGER;
    }
}
