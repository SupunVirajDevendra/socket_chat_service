package com.supundevendra.iso;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.jpos.iso.packager.GenericPackager;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/*
 * ISO 8583 message parser backed by the jPOS
 */
public class ISOMessageParser {

    private static final int HEADER_BYTES = 2;

    /* Singleton packager loaded from classpath packager.xml */
    private static final GenericPackager PACKAGER;
    private static final Exception       PACKAGER_INIT_ERROR;

    static {
        GenericPackager p = null;
        Exception err     = null;
        try {
            InputStream xml = ISOMessageParser.class
                    .getResourceAsStream("/com/supundevendra/iso/packager.xml");
            if (xml == null) {
                throw new IllegalStateException(
                        "packager.xml not found on classpath at " +
                        "/com/supundevendra/iso/packager.xml");
            }
            p = new GenericPackager(xml);
        } catch (Exception e) {
            err = e;
        }
        PACKAGER            = p;
        PACKAGER_INIT_ERROR = err;
    }

    /*
     * Parses the ISO 8583 message and returns the decoded output as a string.
     */
    public String parseToString(String rawInput) {
        StringBuilder sb = new StringBuilder();
        try {
            if (PACKAGER_INIT_ERROR != null) {
                throw new IllegalStateException(
                        "GenericPackager failed to initialise: " +
                        PACKAGER_INIT_ERROR.getMessage(), PACKAGER_INIT_ERROR);
            }

            String hex = extractHex(rawInput);

            sb.append("\n================================================================\n");
            sb.append(" ISO 8583 MESSAGE DECODER\n");
            sb.append("================================================================\n");
            sb.append(String.format(" Raw hex length  : %d chars (%d bytes)\n",
                    hex.length(), hex.length() / 2));

            byte[] rawBytes = EbcdicConverter.hexToBytes(hex);

            if (rawBytes.length <= HEADER_BYTES) {
                throw new IllegalArgumentException(
                        "Message too short — must be longer than 2 header bytes.");
            }

            // Log length header
            byte[] headerBytes = new byte[HEADER_BYTES];
            System.arraycopy(rawBytes, 0, headerBytes, 0, HEADER_BYTES);
            sb.append(String.format(" Length header   : %s (stripped, value=%d)\n",
                    EbcdicConverter.bytesToHex(headerBytes),
                    ((headerBytes[0] & 0xFF) << 8) | (headerBytes[1] & 0xFF)));

            // Strip the 2-byte header and hand the rest to jPOS
            byte[] messageBytes = new byte[rawBytes.length - HEADER_BYTES];
            System.arraycopy(rawBytes, HEADER_BYTES, messageBytes, 0, messageBytes.length);

            ISOMsg msg = new ISOMsg();
            msg.setPackager(PACKAGER);
            PACKAGER.unpack(msg, messageBytes);

            // Bitmap info — derive from which fields are set
            String bitmapHex = extractBitmapHex(messageBytes);
            sb.append(" Bitmap (hex)    : ").append(bitmapHex).append("\n");
            sb.append(" Bitmap size     : ").append(
                    msg.getMaxField() > 64
                            ? "128-bit (primary + secondary)"
                            : "64-bit  (primary only)").append("\n");

            // Collect enabled fields and their decoded values
            List<Integer> enabledFields = new ArrayList<>();
            Map<Integer, String> parsedFields = new LinkedHashMap<>();

            for (int i = 2; i <= 128; i++) {
                if (!msg.hasField(i)) continue;
                enabledFields.add(i);
                try {
                    org.jpos.iso.ISOComponent component = msg.getComponent(i);
                    if (component instanceof org.jpos.iso.ISOMsg) {
                        // Sub-field packager field — render all sub-fields
                        parsedFields.put(i, formatSubFields((org.jpos.iso.ISOMsg) component));
                    } else {
                        byte[] raw = msg.getBytes(i);
                        String strVal = msg.getString(i);
                        if (strVal != null && !strVal.isEmpty() && isPrintable(strVal)) {
                            parsedFields.put(i, strVal);
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

            String cardNetwork = detectCardNetwork(parsedFields.get(2));
            sb.append(" Card Network    : ").append(cardNetwork).append("\n");

            buildResults(sb, msg.getMTI(), enabledFields, parsedFields);

        } catch (Exception e) {
            sb.append("\n[ERROR] Failed to parse ISO message: ").append(e.getMessage()).append("\n");
            sb.append("        Verify the input is a valid ISO 8583 hex message.\n");
        }
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Card network detection
    // -------------------------------------------------------------------------

    /*
     * Detects the card network from the Primary Account Number (Field 2).
     * @param pan Field 2 PAN value; may be {@code null}
     * return {@code "VISA"}, {@code "MASTERCARD"}, {@code "AMEX"},
     *         {@code "DISCOVER"}, or {@code "UNKNOWN"}
     */
    public static String detectCardNetwork(String pan) {
        if (pan == null || pan.trim().isEmpty()) return "UNKNOWN";

        String p = pan.trim();

        if (p.startsWith("4")) return "VISA";

        if (p.length() >= 2) {
            String prefix2 = p.substring(0, 2);
            if (prefix2.equals("34") || prefix2.equals("37")) return "AMEX";
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

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /*
     * Extracts the hex payload from raw user input, supporting multiple formats:
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

        cleaned = cleaned.replaceAll(
                "(?i)received\\s+message\\s+from\\s+(mastercard|visa|amex|discover)\\s*[::]?\\s*", "").trim();
        cleaned = cleaned.replaceAll(
                "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\s*[::]?\\s*", "").trim();

        for (String token : cleaned.split("\\s+")) {
            if (token.matches("[0-9A-Fa-f]+") && token.length() >= 8) {
                return token;
            }
        }

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
     * Reads the first 8 or 16 bytes of the message (after the header has been stripped) and returns them as an uppercase hex string for display.
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
     * Formats an {@link ISOMsg} sub-field container (e.g. Field 48, 54, 61) as a multi-line string showing each sub-field id and value.
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
     * Returns {@code true} if the string contains only printable ASCII (0x20–0x7E).
     */
    private boolean isPrintable(String s) {
        for (char c : s.toCharArray()) {
            if (c < 0x20 || c > 0x7E) return false;
        }
        return true;
    }

    /*
     * Appends the formatted decoded message to the given {@link StringBuilder}.
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
