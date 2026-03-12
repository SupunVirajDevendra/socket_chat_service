package com.supundevendra.iso;

import java.nio.charset.Charset;

/**
 * EbcdicConverter
 *
 * Converts EBCDIC-encoded bytes (IBM037 / Mastercard format) to ASCII.
 * Only used for field value bytes - the binary bitmap is NOT converted.
 */
public class EbcdicConverter {

    private static final Charset EBCDIC = Charset.forName("IBM037");
    private static final Charset ASCII  = Charset.forName("US-ASCII");

    /**
     * Convert a full byte array from EBCDIC to ASCII string.
     *
     * @param ebcdicBytes raw EBCDIC bytes
     * @return ASCII string representation
     */
    public static String toAscii(byte[] ebcdicBytes) {
        String unicode = new String(ebcdicBytes, EBCDIC);
        return new String(unicode.getBytes(ASCII), ASCII);
    }

    /**
     * Convert a full byte array from EBCDIC to ASCII bytes.
     *
     * @param ebcdicBytes raw EBCDIC bytes
     * @return ASCII bytes
     */
    public static byte[] toAsciiBytes(byte[] ebcdicBytes) {
        String unicode = new String(ebcdicBytes, EBCDIC);
        return unicode.getBytes(ASCII);
    }

    /**
     * Convert a hex string representing EBCDIC bytes to ASCII string.
     * Convenience method for when input arrives as a hex string.
     *
     * @param hexEbcdic hex-encoded EBCDIC data (e.g. "F0F1F2")
     * @return decoded ASCII string
     */
    public static String hexEbcdicToAscii(String hexEbcdic) {
        byte[] ebcdicBytes = hexToBytes(hexEbcdic);
        return toAscii(ebcdicBytes);
    }

    /**
     * Utility: convert hex string to raw byte array.
     *
     * @param hex hex string (even length, e.g. "016D")
     * @return byte array
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

    /**
     * Utility: convert raw byte array to hex string.
     *
     * @param bytes raw bytes
     * @return uppercase hex string
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
