package com.supundevendra.chat.util;

/*
 * Utility class used for converting between
 * hexadecimal strings and byte arrays.
 * This is required when decoding ISO 8583 messages
 * because ISO messages are usually received as HEX.
 */
public class EbcdicConverter {

    /*
     * Converts a hexadecimal string into a byte array.
     * Example:
     * "F0F1F2" → [F0, F1, F2]
     */
    public static byte[] hexToBytes(String hex) {

        // Remove any spaces from the hex string
        hex = hex.replaceAll("\\s+", "");

        // A valid hex string must have even number of characters
        // because 2 hex characters = 1 byte
        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have even length: " + hex);
        }

        // Create byte array to store converted values
        byte[] result = new byte[hex.length() / 2];

        // Loop through hex string two characters at a time
        for (int i = 0; i < hex.length(); i += 2) {
            // Convert each pair of hex characters into a byte
            result[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
        }
        // Return converted byte array
        return result;
    }

    /*
     * Converts a byte array into a hexadecimal string.
     * Example:
     * [F0, F1, F2] → "F0F1F2"
     */
    public static String bytesToHex(byte[] bytes) {
        // StringBuilder is used for efficient string creation
        StringBuilder sb = new StringBuilder();

        // Convert each byte to a 2-digit hex value
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }

        // Return final hex string
        return sb.toString();
    }
}
