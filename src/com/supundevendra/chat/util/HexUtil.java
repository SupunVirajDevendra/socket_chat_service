package com.supundevendra.chat.util;

// Shared hex encode/decode helpers used by both client and server
public final class HexUtil {

    // Prevent instantiation — static utility class
    private HexUtil() {}

    // Converts a byte array to an uppercase hex string e.g. {0x0A} -> "0A"
    public static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2); // 2 hex chars per byte
        for (byte b : bytes) sb.append(String.format("%02X", b)); // format each byte as 2-digit hex
        return sb.toString();
    }

    // Converts an even-length hex string to a byte array e.g. "0A1B" -> {0x0A, 0x1B}
    public static byte[] hexToBytes(String hex) {
        byte[] result = new byte[hex.length() / 2]; // each pair of chars = one byte
        for (int i = 0; i < hex.length(); i += 2) {
            result[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16); // parse two hex chars
        }
        return result;
    }
}
