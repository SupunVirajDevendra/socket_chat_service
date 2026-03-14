package com.supundevendra.chat.util;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

// Low-level stream reading helpers shared by client and server
public final class StreamUtil {

    // Prevent instantiation — static utility class
    private StreamUtil() {}

    // Reads exactly n bytes from the stream, retrying on partial TCP reads
    // Throws EOFException if the stream ends before n bytes arrive
    public static byte[] readExact(InputStream in, int n) throws IOException {
        byte[] buf = new byte[n];   // buffer to hold the result
        int offset = 0;             // how many bytes collected so far
        while (offset < n) {
            int read = in.read(buf, offset, n - offset); // read remaining bytes
            if (read == -1) {
                throw new EOFException("Stream ended at " + offset + "/" + n); // peer closed early
            }
            offset += read; // advance the fill position
        }
        return buf;
    }

    // Reads one newline-terminated text line from a raw stream, byte by byte
    // Returns null when the stream ends before any bytes are read (clean EOF)
    public static String readTextLine(InputStream in) throws IOException {
        ByteArrayOutputStream lineBuf = new ByteArrayOutputStream(256); // accumulate line bytes
        int b;
        boolean gotAny = false; // tracks whether we read at least one byte
        while ((b = in.read()) != -1) {
            gotAny = true;
            if (b == '\n') break;      // end of line
            if (b == '\r') continue;   // skip carriage return
            lineBuf.write(b);          // accumulate the character
        }
        if (!gotAny) return null; // EOF with no data — signal caller
        return lineBuf.toString(StandardCharsets.UTF_8); // decode collected bytes as UTF-8
    }
}
