package com.supundevendra.chat.util;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Low-level stream reading utilities shared by the client and server.
 */
public final class StreamUtil {

    private StreamUtil() {}

    /**
     * Reads exactly {@code n} bytes from the stream, handling TCP partial reads.
     * Throws {@link EOFException} if the stream ends before {@code n} bytes are read.
     */
    public static byte[] readExact(InputStream in, int n) throws IOException {
        byte[] buf = new byte[n];
        int offset = 0;
        while (offset < n) {
            int read = in.read(buf, offset, n - offset);
            if (read == -1) {
                throw new EOFException("Stream ended at " + offset + "/" + n);
            }
            offset += read;
        }
        return buf;
    }

    /**
     * Reads one LF-terminated text line from a raw InputStream byte-by-byte,
     * so we never over-read into the next framed message.
     * Returns {@code null} on EOF before any bytes are read.
     */
    public static String readTextLine(InputStream in) throws IOException {
        ByteArrayOutputStream lineBuf = new ByteArrayOutputStream(256);
        int b;
        boolean gotAny = false;
        while ((b = in.read()) != -1) {
            gotAny = true;
            if (b == '\n') break;
            if (b == '\r') continue;
            lineBuf.write(b);
        }
        if (!gotAny) return null;
        return lineBuf.toString(StandardCharsets.UTF_8);
    }
}
