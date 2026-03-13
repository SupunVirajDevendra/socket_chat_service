package com.supundevendra.iso;

/*
 * Utility class for hex/byte conversion used in ISO 8583 message parsing.
 */
public class EbcdicConverter {

    /*
     * Utility: convert hex string to raw byte array.
     */
    public static byte[] hexToBytes(String hex) {
        hex = hex.replaceAll("\\s+", "");
        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have even length: " + hex);
        }
        byte[] result = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            result[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
        }
        return result;
    }

    /*
     * Utility: convert raw byte array to hex string.
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
