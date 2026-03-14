package com.supundevendra.chat.util;

/**
 * Shared hex encoding / decoding utilities used by both the client and server.
 */
public final class HexUtil {

    private HexUtil() {}

    /** Converts a byte array to an uppercase hex string (e.g. "0A1B2C"). */
    public static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02X", b));
        return sb.toString();
    }

    /** Converts an even-length hex string to a byte array. */
    public static byte[] hexToBytes(String hex) {
        byte[] result = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            result[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
        }
        return result;
    }
}
