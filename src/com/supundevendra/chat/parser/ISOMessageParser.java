package com.supundevendra.chat.parser;

import com.supundevendra.chat.util.EbcdicConverter;
import com.supundevendra.chat.util.ISOFieldDictionary;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.jpos.iso.packager.GenericPackager;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/*
 * ISO 8583 message parser using the jPOS library.
 * This class reads a raw ISO8583 hex message and converts it
 * into a human readable decoded format.
 */
public class ISOMessageParser {

    // ISO messages often start with a 2-byte length header
    private static final int HEADER_BYTES = 2;

    // jPOS packager used to unpack ISO8583 fields
    private static final GenericPackager PACKAGER;
    // Stores error if packager fails to initialize
    private static final Exception       PACKAGER_INIT_ERROR;

    /*
     * Static block runs once when the class loads.
     * It loads packager.xml which defines ISO8583 field structure.
     */
    static {
        GenericPackager p = null;
        Exception err     = null;

        try {
            // Load packager configuration file
            InputStream xml = ISOMessageParser.class
                    .getResourceAsStream("/resources/packager.xml");
            if (xml == null) {
                throw new IllegalStateException(
                        "packager.xml not found on classpath at " +
                                "/resources/packager.xml");
            }
            // Create jPOS packager
            p = new GenericPackager(xml);
        } catch (Exception e) {
            err = e;
        }
        PACKAGER            = p;
        PACKAGER_INIT_ERROR = err;
    }

    /*
     * Main method that parses an ISO8583 message and returns
     * a formatted readable string.
     */
    public String parseToString(String rawInput) {
        StringBuilder sb = new StringBuilder();
        try {
            // Check if packager loaded successfully
            if (PACKAGER_INIT_ERROR != null) {
                throw new IllegalStateException(
                        "GenericPackager failed to initialise: " +
                        PACKAGER_INIT_ERROR.getMessage(), PACKAGER_INIT_ERROR);
            }

            // Extract hex payload from input
            String hex = extractHex(rawInput);

            sb.append("\n================================================================\n");
            sb.append(" ISO 8583 MESSAGE DECODER\n");
            sb.append("================================================================\n");
            sb.append(String.format(" Raw hex length  : %d chars (%d bytes)\n",
                    hex.length(), hex.length() / 2));

            // Convert hex string to byte array
            byte[] rawBytes = EbcdicConverter.hexToBytes(hex);

            // Check if message contains header + data
            if (rawBytes.length <= HEADER_BYTES) {
                throw new IllegalArgumentException(
                        "Message too short — must be longer than 2 header bytes.");
            }

            // Read the 2-byte length header
            byte[] headerBytes = new byte[HEADER_BYTES];
            System.arraycopy(rawBytes, 0, headerBytes, 0, HEADER_BYTES);
            sb.append(String.format(" Length header   : %s (stripped, value=%d)\n",
                    EbcdicConverter.bytesToHex(headerBytes),
                    ((headerBytes[0] & 0xFF) << 8) | (headerBytes[1] & 0xFF)));

            // Remove header and keep actual ISO message
            byte[] messageBytes = new byte[rawBytes.length - HEADER_BYTES];
            System.arraycopy(rawBytes, HEADER_BYTES, messageBytes, 0, messageBytes.length);

            // Create ISO message object
            ISOMsg msg = new ISOMsg();
            msg.setPackager(PACKAGER);

            // Unpack ISO fields using jPOS
            PACKAGER.unpack(msg, messageBytes);

            // Extract bitmap for display
            String bitmapHex = extractBitmapHex(messageBytes);
            sb.append(" Bitmap (hex)    : ").append(bitmapHex).append("\n");

            // Determine bitmap size
            sb.append(" Bitmap size     : ").append(
                    msg.getMaxField() > 64
                            ? "128-bit (primary + secondary)"
                            : "64-bit  (primary only)").append("\n");

            // Store fields detected in bitmap
            List<Integer> enabledFields = new ArrayList<>();
            // Store decoded field values
            Map<Integer, String> parsedFields = new LinkedHashMap<>();

            // Check fields from 2 to 128
            for (int i = 2; i <= 128; i++) {
                if (!msg.hasField(i)) continue;
                enabledFields.add(i);
                try {
                    // Read field component
                    org.jpos.iso.ISOComponent component = msg.getComponent(i);
                    // Handle sub-fields (fields like 48 or 54)
                    if (component instanceof org.jpos.iso.ISOMsg) {
                        // Sub-field packager field — render all sub-fields
                        parsedFields.put(i, formatSubFields((org.jpos.iso.ISOMsg) component));
                    } else {
                        byte[] raw = msg.getBytes(i);
                        String strVal = msg.getString(i);

                        // If printable text
                        if (strVal != null && !strVal.isEmpty() && isPrintable(strVal)) {
                            parsedFields.put(i, strVal);

                            // If binary data
                        } else if (raw != null) {
                            parsedFields.put(i, "[binary: " + ISOUtil.hexString(raw).toUpperCase() + "]");
                        } else {
                            parsedFields.put(i, "[empty]");
                        }
                    }
                } catch (Exception e) {
                    parsedFields.put(i, "[error reading field: " + e.getMessage() + "]");
                }
            }

            // Detect card network from PAN (Field 2)
            String cardNetwork = detectCardNetwork(parsedFields.get(2));
            sb.append(" Card Network    : ").append(cardNetwork).append("\n");

            // Build final decoded output
            buildResults(sb, msg.getMTI(), enabledFields, parsedFields);

        } catch (Exception e) {
            sb.append("\n[ERROR] Failed to parse ISO message: ").append(e.getMessage()).append("\n");
            sb.append("        Verify the input is a valid ISO 8583 hex message.\n");
        }
        return sb.toString();
    }

    /*
     * Detect card network using PAN (field 2).
     * Uses BIN ranges.
     */
    public static String detectCardNetwork(String pan) {
        if (pan == null || pan.trim().isEmpty()) return "UNKNOWN";

        String p = pan.trim();
        // VISA cards start with 4
        if (p.startsWith("4")) return "VISA";

        // AMEX
        if (p.length() >= 2) {
            String prefix2 = p.substring(0, 2);
            if (prefix2.equals("34") || prefix2.equals("37")) return "AMEX";
            // MasterCard ranges
            try {
                int n = Integer.parseInt(prefix2);
                if (n >= 51 && n <= 55) return "MASTERCARD";
            } catch (NumberFormatException ignored) { }
        }

        if (p.length() >= 4) {
            try {
                int prefix4 = Integer.parseInt(p.substring(0, 4));
                if (prefix4 >= 2221 && prefix4 <= 2720) return "MASTERCARD";
                if (prefix4 == 6011) return "DISCOVER";
                int prefix3 = prefix4 / 10;
                if (prefix3 >= 644 && prefix3 <= 649) return "DISCOVER";
                if (prefix3 >= 622 && prefix3 <= 629) return "DISCOVER";
            } catch (NumberFormatException ignored) { }
        }

        if (p.length() >= 2 && p.startsWith("65")) return "DISCOVER";

        return "UNKNOWN";
    }

    /*
     * Extracts hex message from raw input.
     * Supports many formats (logs, prefixes, etc).
     */
    private String extractHex(String rawInput) {
        if (rawInput == null || rawInput.trim().isEmpty()) {
            throw new IllegalArgumentException("Input is empty.");
        }

        String cleaned = rawInput.trim();

        for (String line : cleaned.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (trimmed.matches("[0-9A-Fa-f]+") && trimmed.length() >= 8) {
                return trimmed;
            }
        }

        // Case 1: pure hex line and Remove common prefixes
        cleaned = cleaned.replaceAll(
                "(?i)received\\s+message\\s+from\\s+(mastercard|visa|amex|discover)\\s*[::]?\\s*", "").trim();
        cleaned = cleaned.replaceAll(
                "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\s*[::]?\\s*", "").trim();
        for (String token : cleaned.split("\\s+")) {
            if (token.matches("[0-9A-Fa-f]+") && token.length() >= 8) {
                return token;
            }
        }

        // Keep only hex characters
        String hexOnly = cleaned.replaceAll("[^0-9A-Fa-f]", "");
        if (hexOnly.length() >= 8) {
            return (hexOnly.length() % 2 != 0)
                    ? hexOnly.substring(0, hexOnly.length() - 1)
                    : hexOnly;
        }

        throw new IllegalArgumentException(
                "Could not extract a valid hex string from input.");
    }

    /*
     * Reads bitmap bytes and converts them to hex for display.
     */
    private String extractBitmapHex(byte[] messageBytes) {

        // MTI is 4 bytes (EBCDIC-encoded), bitmap starts at offset 4
        int bitmapOffset = 4;
        if (messageBytes.length < bitmapOffset + 8) return "N/A";
        boolean hasSecondary = (messageBytes[bitmapOffset] & 0x80) != 0;
        int bitmapLen = hasSecondary ? 16 : 8;
        if (messageBytes.length < bitmapOffset + bitmapLen) bitmapLen = messageBytes.length - bitmapOffset;
        byte[] bmap = new byte[bitmapLen];
        System.arraycopy(messageBytes, bitmapOffset, bmap, 0, bitmapLen);
        return EbcdicConverter.bytesToHex(bmap).toUpperCase();
    }

    /*
     * Formats sub-fields (fields containing nested data).
     */
    private String formatSubFields(ISOMsg sub) {
        StringBuilder sb = new StringBuilder("[sub-fields]");
        try {
            for (int i = 0; i <= sub.getMaxField(); i++) {
                if (!sub.hasField(i)) continue;
                String val = sub.getString(i);
                if (val == null || val.isEmpty()) continue;
                sb.append("\n      [").append(i).append("] ").append(val);
            }
        } catch (Exception ignored) { }
        return sb.toString();
    }

    /*
     * Check if string contains printable ASCII characters.
     */
    private boolean isPrintable(String s) {
        for (char c : s.toCharArray()) {
            if (c < 0x20 || c > 0x7E) return false;
        }
        return true;
    }

    /*
     * Builds the final formatted decoded output.
     */
    private void buildResults(StringBuilder sb, String mti,
                              List<Integer> enabledFields,
                              Map<Integer, String> parsedFields) {

        sb.append("\n----------------------------------------------------------------\n");
        sb.append(" MTI : ").append(mti)
          .append("  (").append(ISOFieldDictionary.getMtiDescription(mti)).append(")\n");
        sb.append("----------------------------------------------------------------\n");

        sb.append("\n Enabled fields detected from bitmap:\n ");
        for (int i = 0; i < enabledFields.size(); i++) {
            sb.append(enabledFields.get(i));
            if (i < enabledFields.size() - 1) sb.append(", ");
        }
        sb.append("\n");

        sb.append("\n----------------------------------------------------------------\n");
        sb.append(" Decoded Message\n");
        sb.append("----------------------------------------------------------------\n\n");

        int count = 0;
        for (int fieldNum : enabledFields) {
            sb.append(String.format(" Field %-3d\n", fieldNum));
            sb.append("   Name        : ").append(ISOFieldDictionary.getName(fieldNum)).append("\n");
            sb.append("   Value       : ").append(
                    parsedFields.getOrDefault(fieldNum, "[not parsed]")).append("\n");
            sb.append("   Description : ").append(
                    ISOFieldDictionary.getDescription(fieldNum)).append("\n\n");
            count++;
        }

        if (count == 0) sb.append(" No fields were decoded.\n");

        sb.append("================================================================\n");
        sb.append(" Total fields decoded: ").append(count).append("\n");
        sb.append("================================================================\n\n");
    }
}
