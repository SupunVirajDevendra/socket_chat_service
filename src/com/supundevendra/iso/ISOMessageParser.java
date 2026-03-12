package com.supundevendra.iso;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Custom ISO 8583 field parser for EBCDIC-encoded Mastercard messages.
 *
 * <p>Decode pipeline:
 * <ol>
 *   <li>Extract hex string from raw input (strips network prefix and IP address if present)</li>
 *   <li>Convert hex to raw bytes</li>
 *   <li>Strip 2-byte binary length header</li>
 *   <li>EBCDIC-decode the 4-byte MTI</li>
 *   <li>Read primary bitmap (8 bytes); extend to 16 bytes if secondary bitmap bit is set</li>
 *   <li>EBCDIC-decode the field data block</li>
 *   <li>Walk enabled fields from the bitmap and parse each according to its type</li>
 * </ol>
 *
 * <p>Supported field types:
 * <ul>
 *   <li>{@code IFA_NUMERIC / IF_CHAR} — fixed-length ASCII characters</li>
 *   <li>{@code IFA_LLNUM / IFA_LLCHAR} — 2-digit ASCII length prefix followed by data</li>
 *   <li>{@code IFA_LLLCHAR} — 3-digit ASCII length prefix followed by data</li>
 *   <li>{@code IFB_BINARY} — fixed-length raw binary bytes</li>
 * </ul>
 */
public class ISOMessageParser {

    private static final int TYPE_FIXED_CHAR  = 1;
    private static final int TYPE_LLVAR_CHAR  = 2;
    private static final int TYPE_LLLVAR_CHAR = 3;
    private static final int TYPE_BINARY      = 4;

    private static final int HEADER_BYTES = 2;
    private static final int MTI_BYTES    = 4;
    private static final int BITMAP_BYTES = 8;

    private static final Charset EBCDIC = Charset.forName("IBM037");
    private static final Charset ASCII  = Charset.forName("US-ASCII");

    private static final int[][] FIELD_DEF = new int[129][2];

    static {
        FIELD_DEF[1]   = new int[]{TYPE_BINARY,       8};
        FIELD_DEF[2]   = new int[]{TYPE_LLVAR_CHAR,  19};
        FIELD_DEF[3]   = new int[]{TYPE_FIXED_CHAR,   6};
        FIELD_DEF[4]   = new int[]{TYPE_FIXED_CHAR,  12};
        FIELD_DEF[5]   = new int[]{TYPE_FIXED_CHAR,  12};
        FIELD_DEF[6]   = new int[]{TYPE_FIXED_CHAR,  12};
        FIELD_DEF[7]   = new int[]{TYPE_FIXED_CHAR,  10};
        FIELD_DEF[8]   = new int[]{TYPE_FIXED_CHAR,   8};
        FIELD_DEF[9]   = new int[]{TYPE_FIXED_CHAR,   8};
        FIELD_DEF[10]  = new int[]{TYPE_FIXED_CHAR,   8};
        FIELD_DEF[11]  = new int[]{TYPE_FIXED_CHAR,   6};
        FIELD_DEF[12]  = new int[]{TYPE_FIXED_CHAR,   6};
        FIELD_DEF[13]  = new int[]{TYPE_FIXED_CHAR,   4};
        FIELD_DEF[14]  = new int[]{TYPE_FIXED_CHAR,   4};
        FIELD_DEF[15]  = new int[]{TYPE_FIXED_CHAR,   4};
        FIELD_DEF[16]  = new int[]{TYPE_FIXED_CHAR,   4};
        FIELD_DEF[17]  = new int[]{TYPE_FIXED_CHAR,   4};
        FIELD_DEF[18]  = new int[]{TYPE_FIXED_CHAR,   4};
        FIELD_DEF[19]  = new int[]{TYPE_FIXED_CHAR,   3};
        FIELD_DEF[20]  = new int[]{TYPE_FIXED_CHAR,   3};
        FIELD_DEF[21]  = new int[]{TYPE_FIXED_CHAR,   3};
        FIELD_DEF[22]  = new int[]{TYPE_FIXED_CHAR,   3};
        FIELD_DEF[23]  = new int[]{TYPE_FIXED_CHAR,   3};
        FIELD_DEF[24]  = new int[]{TYPE_FIXED_CHAR,   3};
        FIELD_DEF[25]  = new int[]{TYPE_FIXED_CHAR,   2};
        FIELD_DEF[26]  = new int[]{TYPE_FIXED_CHAR,   2};
        FIELD_DEF[27]  = new int[]{TYPE_FIXED_CHAR,   1};
        FIELD_DEF[28]  = new int[]{TYPE_FIXED_CHAR,   9};
        FIELD_DEF[29]  = new int[]{TYPE_FIXED_CHAR,   9};
        FIELD_DEF[30]  = new int[]{TYPE_FIXED_CHAR,   9};
        FIELD_DEF[31]  = new int[]{TYPE_FIXED_CHAR,   9};
        FIELD_DEF[32]  = new int[]{TYPE_LLVAR_CHAR,  11};
        FIELD_DEF[33]  = new int[]{TYPE_LLVAR_CHAR,  11};
        FIELD_DEF[34]  = new int[]{TYPE_LLVAR_CHAR,  28};
        FIELD_DEF[35]  = new int[]{TYPE_LLVAR_CHAR,  37};
        FIELD_DEF[36]  = new int[]{TYPE_LLLVAR_CHAR,104};
        FIELD_DEF[37]  = new int[]{TYPE_FIXED_CHAR,  12};
        FIELD_DEF[38]  = new int[]{TYPE_FIXED_CHAR,   6};
        FIELD_DEF[39]  = new int[]{TYPE_FIXED_CHAR,   2};
        FIELD_DEF[40]  = new int[]{TYPE_FIXED_CHAR,   3};
        FIELD_DEF[41]  = new int[]{TYPE_FIXED_CHAR,   8};
        FIELD_DEF[42]  = new int[]{TYPE_FIXED_CHAR,  15};
        FIELD_DEF[43]  = new int[]{TYPE_FIXED_CHAR,  40};
        FIELD_DEF[44]  = new int[]{TYPE_LLVAR_CHAR,  25};
        FIELD_DEF[45]  = new int[]{TYPE_LLVAR_CHAR,  76};
        FIELD_DEF[46]  = new int[]{TYPE_LLLVAR_CHAR,999};
        FIELD_DEF[47]  = new int[]{TYPE_LLLVAR_CHAR,999};
        FIELD_DEF[48]  = new int[]{TYPE_LLLVAR_CHAR,999};
        FIELD_DEF[49]  = new int[]{TYPE_FIXED_CHAR,   3};
        FIELD_DEF[50]  = new int[]{TYPE_FIXED_CHAR,   3};
        FIELD_DEF[51]  = new int[]{TYPE_FIXED_CHAR,   3};
        FIELD_DEF[52]  = new int[]{TYPE_BINARY,        8};
        FIELD_DEF[53]  = new int[]{TYPE_FIXED_CHAR,  16};
        FIELD_DEF[54]  = new int[]{TYPE_LLLVAR_CHAR,120};
        FIELD_DEF[55]  = new int[]{TYPE_LLLVAR_CHAR,999};
        FIELD_DEF[56]  = new int[]{TYPE_LLLVAR_CHAR,999};
        FIELD_DEF[57]  = new int[]{TYPE_LLLVAR_CHAR,999};
        FIELD_DEF[58]  = new int[]{TYPE_LLLVAR_CHAR,999};
        FIELD_DEF[59]  = new int[]{TYPE_LLLVAR_CHAR,999};
        FIELD_DEF[60]  = new int[]{TYPE_LLLVAR_CHAR,999};
        FIELD_DEF[61]  = new int[]{TYPE_LLLVAR_CHAR,999};
        FIELD_DEF[62]  = new int[]{TYPE_LLLVAR_CHAR,999};
        FIELD_DEF[63]  = new int[]{TYPE_LLLVAR_CHAR,999};
        FIELD_DEF[64]  = new int[]{TYPE_BINARY,        8};
        FIELD_DEF[65]  = new int[]{TYPE_BINARY,        8};
        FIELD_DEF[66]  = new int[]{TYPE_FIXED_CHAR,   1};
        FIELD_DEF[67]  = new int[]{TYPE_FIXED_CHAR,   2};
        FIELD_DEF[68]  = new int[]{TYPE_FIXED_CHAR,   3};
        FIELD_DEF[69]  = new int[]{TYPE_FIXED_CHAR,   3};
        FIELD_DEF[70]  = new int[]{TYPE_FIXED_CHAR,   3};
        FIELD_DEF[71]  = new int[]{TYPE_FIXED_CHAR,   4};
        FIELD_DEF[72]  = new int[]{TYPE_FIXED_CHAR,   4};
        FIELD_DEF[73]  = new int[]{TYPE_FIXED_CHAR,   6};
        FIELD_DEF[74]  = new int[]{TYPE_FIXED_CHAR,  10};
        FIELD_DEF[75]  = new int[]{TYPE_FIXED_CHAR,  10};
        FIELD_DEF[76]  = new int[]{TYPE_FIXED_CHAR,  10};
        FIELD_DEF[77]  = new int[]{TYPE_FIXED_CHAR,  10};
        FIELD_DEF[78]  = new int[]{TYPE_FIXED_CHAR,  10};
        FIELD_DEF[79]  = new int[]{TYPE_FIXED_CHAR,  10};
        FIELD_DEF[80]  = new int[]{TYPE_FIXED_CHAR,  10};
        FIELD_DEF[81]  = new int[]{TYPE_FIXED_CHAR,  10};
        FIELD_DEF[82]  = new int[]{TYPE_FIXED_CHAR,  12};
        FIELD_DEF[83]  = new int[]{TYPE_FIXED_CHAR,  12};
        FIELD_DEF[84]  = new int[]{TYPE_FIXED_CHAR,  12};
        FIELD_DEF[85]  = new int[]{TYPE_FIXED_CHAR,  12};
        FIELD_DEF[86]  = new int[]{TYPE_FIXED_CHAR,  16};
        FIELD_DEF[87]  = new int[]{TYPE_FIXED_CHAR,  16};
        FIELD_DEF[88]  = new int[]{TYPE_FIXED_CHAR,  16};
        FIELD_DEF[89]  = new int[]{TYPE_FIXED_CHAR,  16};
        FIELD_DEF[90]  = new int[]{TYPE_FIXED_CHAR,  42};
        FIELD_DEF[91]  = new int[]{TYPE_FIXED_CHAR,   1};
        FIELD_DEF[92]  = new int[]{TYPE_FIXED_CHAR,   2};
        FIELD_DEF[93]  = new int[]{TYPE_FIXED_CHAR,   5};
        FIELD_DEF[94]  = new int[]{TYPE_FIXED_CHAR,   7};
        FIELD_DEF[95]  = new int[]{TYPE_FIXED_CHAR,  42};
        FIELD_DEF[96]  = new int[]{TYPE_BINARY,        8};
        FIELD_DEF[97]  = new int[]{TYPE_FIXED_CHAR,  17};
        FIELD_DEF[98]  = new int[]{TYPE_FIXED_CHAR,  25};
        FIELD_DEF[99]  = new int[]{TYPE_LLVAR_CHAR,  11};
        FIELD_DEF[100] = new int[]{TYPE_LLVAR_CHAR,  11};
        FIELD_DEF[101] = new int[]{TYPE_LLVAR_CHAR,  17};
        FIELD_DEF[102] = new int[]{TYPE_LLVAR_CHAR,  28};
        FIELD_DEF[103] = new int[]{TYPE_LLVAR_CHAR,  28};
        FIELD_DEF[104] = new int[]{TYPE_LLLVAR_CHAR,100};
        FIELD_DEF[105] = new int[]{TYPE_LLLVAR_CHAR,999};
        FIELD_DEF[106] = new int[]{TYPE_LLLVAR_CHAR,999};
        FIELD_DEF[107] = new int[]{TYPE_LLLVAR_CHAR,999};
        FIELD_DEF[108] = new int[]{TYPE_LLLVAR_CHAR,999};
        FIELD_DEF[109] = new int[]{TYPE_LLLVAR_CHAR,999};
        FIELD_DEF[110] = new int[]{TYPE_LLLVAR_CHAR,999};
        FIELD_DEF[111] = new int[]{TYPE_LLLVAR_CHAR,999};
        FIELD_DEF[112] = new int[]{TYPE_LLLVAR_CHAR,999};
        FIELD_DEF[113] = new int[]{TYPE_LLLVAR_CHAR,999};
        FIELD_DEF[114] = new int[]{TYPE_LLLVAR_CHAR,999};
        FIELD_DEF[115] = new int[]{TYPE_LLLVAR_CHAR,999};
        FIELD_DEF[116] = new int[]{TYPE_LLLVAR_CHAR,999};
        FIELD_DEF[117] = new int[]{TYPE_LLLVAR_CHAR,999};
        FIELD_DEF[118] = new int[]{TYPE_LLLVAR_CHAR,999};
        FIELD_DEF[119] = new int[]{TYPE_LLLVAR_CHAR,999};
        FIELD_DEF[120] = new int[]{TYPE_LLLVAR_CHAR,999};
        FIELD_DEF[121] = new int[]{TYPE_LLLVAR_CHAR,999};
        FIELD_DEF[122] = new int[]{TYPE_LLLVAR_CHAR,999};
        FIELD_DEF[123] = new int[]{TYPE_LLLVAR_CHAR,999};
        FIELD_DEF[124] = new int[]{TYPE_LLLVAR_CHAR,999};
        FIELD_DEF[125] = new int[]{TYPE_LLLVAR_CHAR,999};
        FIELD_DEF[126] = new int[]{TYPE_LLLVAR_CHAR,999};
        FIELD_DEF[127] = new int[]{TYPE_LLLVAR_CHAR,999};
        FIELD_DEF[128] = new int[]{TYPE_BINARY,        8};
    }

    /**
     * Parses the ISO 8583 message and prints the decoded output to stdout.
     *
     * @param rawInput raw hex string or any supported prefixed format
     */
    public void parse(String rawInput) {
        System.out.print(parseToString(rawInput));
    }

    /**
     * Parses the ISO 8583 message and returns the decoded output as a string.
     *
     * @param rawInput raw hex string or any supported prefixed format
     * @return fully formatted decoded output
     */
    public String parseToString(String rawInput) {
        StringBuilder sb = new StringBuilder();
        try {
            String hex = extractHex(rawInput);

            sb.append("\n================================================================\n");
            sb.append(" ISO 8583 MESSAGE DECODER\n");
            sb.append("================================================================\n");
            sb.append(String.format(" Raw hex length  : %d chars (%d bytes)\n",
                    hex.length(), hex.length() / 2));

            byte[] rawBytes = EbcdicConverter.hexToBytes(hex);

            if (rawBytes.length <= HEADER_BYTES) {
                throw new IllegalArgumentException("Message too short — must be longer than 2 header bytes.");
            }

            byte[] headerBytes = new byte[HEADER_BYTES];
            System.arraycopy(rawBytes, 0, headerBytes, 0, HEADER_BYTES);
            sb.append(String.format(" Length header   : %s (stripped, value=%d)\n",
                    EbcdicConverter.bytesToHex(headerBytes),
                    ((headerBytes[0] & 0xFF) << 8) | (headerBytes[1] & 0xFF)));

            byte[] messageBytes = new byte[rawBytes.length - HEADER_BYTES];
            System.arraycopy(rawBytes, HEADER_BYTES, messageBytes, 0, messageBytes.length);

            if (messageBytes.length < MTI_BYTES + BITMAP_BYTES) {
                throw new IllegalArgumentException("Message body too short.");
            }

            byte[] mtiBytes = new byte[MTI_BYTES];
            System.arraycopy(messageBytes, 0, mtiBytes, 0, MTI_BYTES);
            String mti = new String(mtiBytes, EBCDIC).trim();

            byte[] primaryBitmap = new byte[BITMAP_BYTES];
            System.arraycopy(messageBytes, MTI_BYTES, primaryBitmap, 0, BITMAP_BYTES);

            boolean hasSecondaryBitmap = (primaryBitmap[0] & 0x80) != 0;
            int totalBitmapBytes = hasSecondaryBitmap ? 16 : 8;

            if (messageBytes.length < MTI_BYTES + totalBitmapBytes) {
                throw new IllegalArgumentException("Message too short for bitmap.");
            }

            byte[] bitmapBytes = new byte[totalBitmapBytes];
            System.arraycopy(messageBytes, MTI_BYTES, bitmapBytes, 0, totalBitmapBytes);

            sb.append(" Bitmap (hex)    : ").append(EbcdicConverter.bytesToHex(bitmapBytes).toUpperCase()).append("\n");
            sb.append(" Bitmap size     : ").append(hasSecondaryBitmap
                    ? "128-bit (primary + secondary)"
                    : "64-bit  (primary only)").append("\n");

            List<Integer> enabledFields = getEnabledFields(bitmapBytes);

            int fieldDataOffset = MTI_BYTES + totalBitmapBytes;
            byte[] fieldDataEbcdic = new byte[messageBytes.length - fieldDataOffset];
            System.arraycopy(messageBytes, fieldDataOffset, fieldDataEbcdic, 0, fieldDataEbcdic.length);
            byte[] fieldDataAscii = new String(fieldDataEbcdic, EBCDIC).getBytes(ASCII);

            Map<Integer, String> parsedFields = parseFields(enabledFields, fieldDataAscii);

            String cardNetwork = detectCardNetwork(parsedFields.get(2));
            sb.append(" Card Network    : ").append(cardNetwork).append("\n");

            buildResults(sb, mti, enabledFields, parsedFields);

        } catch (Exception e) {
            sb.append("\n[ERROR] Failed to parse ISO message: ").append(e.getMessage()).append("\n");
            sb.append("        Verify the input is a valid ISO 8583 hex message.\n");
        }
        return sb.toString();
    }

    /**
     * Detects the card network from the Primary Account Number (Field 2).
     *
     * <p>Detection rules:
     * <ul>
     *   <li>Visa: starts with {@code 4}</li>
     *   <li>Mastercard: starts with {@code 51}–{@code 55}, or in range {@code 2221}–{@code 2720}</li>
     *   <li>Amex: starts with {@code 34} or {@code 37}</li>
     *   <li>Discover: starts with {@code 6011}, {@code 622x}, {@code 644}–{@code 649}, or {@code 65}</li>
     * </ul>
     *
     * @param pan Field 2 PAN value; may be {@code null}
     * @return {@code "VISA"}, {@code "MASTERCARD"}, {@code "AMEX"}, {@code "DISCOVER"}, or {@code "UNKNOWN"}
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

    /**
     * Extracts the hex payload from raw user input, supporting multiple formats:
     * <ul>
     *   <li>Multi-line: scans each line for a pure hex string</li>
     *   <li>Single-line with network prefix: strips {@code "Received message from MASTERCARD:"}</li>
     *   <li>Single-line with IP prefix: strips {@code "192.168.x.x :"}</li>
     *   <li>Fallback: strips all non-hex characters</li>
     * </ul>
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

        cleaned = cleaned.replaceAll("(?i)received\\s+message\\s+from\\s+mastercard\\s*[::]?\\s*", "").trim();
        cleaned = cleaned.replaceAll("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\s*[::]?\\s*", "").trim();

        for (String token : cleaned.split("\\s+")) {
            if (token.matches("[0-9A-Fa-f]+") && token.length() >= 8) {
                return token;
            }
        }

        String hexOnly = cleaned.replaceAll("[^0-9A-Fa-f]", "");
        if (hexOnly.length() >= 8) {
            return (hexOnly.length() % 2 != 0) ? hexOnly.substring(0, hexOnly.length() - 1) : hexOnly;
        }

        throw new IllegalArgumentException("Could not extract a valid hex string from input.");
    }

    /**
     * Returns the list of field numbers enabled in the bitmap.
     * Fields 1 (bitmap) and 65 (secondary bitmap indicator) are excluded.
     */
    private List<Integer> getEnabledFields(byte[] bitmap) {
        List<Integer> fields = new ArrayList<>();
        for (int byteIdx = 0; byteIdx < bitmap.length; byteIdx++) {
            int b = bitmap[byteIdx] & 0xFF;
            for (int bit = 7; bit >= 0; bit--) {
                int fieldNum = byteIdx * 8 + (8 - bit);
                if ((b & (1 << bit)) != 0 && fieldNum != 1 && fieldNum != 65) {
                    fields.add(fieldNum);
                }
            }
        }
        return fields;
    }

    /**
     * Walks the ASCII-decoded field data buffer and parses each enabled field.
     *
     * @param enabledFields field numbers to parse, in order
     * @param data          EBCDIC-decoded field data as ASCII bytes
     * @return ordered map of field number to decoded value string
     */
    private Map<Integer, String> parseFields(List<Integer> enabledFields, byte[] data) {
        Map<Integer, String> result = new LinkedHashMap<>();
        int pos = 0;

        for (int fieldNum : enabledFields) {
            if (fieldNum < 1 || fieldNum > 128 || FIELD_DEF[fieldNum] == null) {
                System.out.printf(" [WARN] Field %d: no definition, skipping%n", fieldNum);
                continue;
            }
            if (pos > data.length) {
                System.out.printf(" [WARN] Field %d: buffer exhausted at pos %d%n", fieldNum, pos);
                break;
            }

            int type      = FIELD_DEF[fieldNum][0];
            int maxLength = FIELD_DEF[fieldNum][1];

            try {
                switch (type) {
                    case TYPE_FIXED_CHAR: {
                        if (pos + maxLength > data.length) {
                            result.put(fieldNum, "[buffer underrun]");
                            pos = data.length;
                        } else {
                            result.put(fieldNum, new String(data, pos, maxLength, ASCII));
                            pos += maxLength;
                        }
                        break;
                    }
                    case TYPE_LLVAR_CHAR: {
                        if (pos + 2 > data.length) { result.put(fieldNum, "[buffer underrun reading LL]"); pos = data.length; break; }
                        String llStr = new String(data, pos, 2, ASCII).trim();
                        int len;
                        try { len = Integer.parseInt(llStr); }
                        catch (NumberFormatException e) { result.put(fieldNum, "[invalid LL: '" + llStr + "']"); pos += 2; break; }
                        pos += 2;
                        if (len > maxLength)          { result.put(fieldNum, "[LL " + len + " exceeds max " + maxLength + "]"); pos += Math.min(len, data.length - pos); break; }
                        if (pos + len > data.length)  { result.put(fieldNum, "[buffer underrun reading " + len + " bytes]"); pos = data.length; break; }
                        result.put(fieldNum, new String(data, pos, len, ASCII));
                        pos += len;
                        break;
                    }
                    case TYPE_LLLVAR_CHAR: {
                        if (pos + 3 > data.length) { result.put(fieldNum, "[buffer underrun reading LLL]"); pos = data.length; break; }
                        String lllStr = new String(data, pos, 3, ASCII).trim();
                        int len;
                        try { len = Integer.parseInt(lllStr); }
                        catch (NumberFormatException e) { result.put(fieldNum, "[invalid LLL: '" + lllStr + "']"); pos += 3; break; }
                        pos += 3;
                        if (len > maxLength)          { result.put(fieldNum, "[LLL " + len + " exceeds max " + maxLength + "]"); pos += Math.min(len, data.length - pos); break; }
                        if (pos + len > data.length)  { result.put(fieldNum, "[buffer underrun reading " + len + " bytes]"); pos = data.length; break; }
                        result.put(fieldNum, new String(data, pos, len, ASCII));
                        pos += len;
                        break;
                    }
                    case TYPE_BINARY: {
                        if (pos + maxLength > data.length) {
                            result.put(fieldNum, "[binary - buffer underrun]");
                            pos = data.length;
                        } else {
                            byte[] raw = new byte[maxLength];
                            System.arraycopy(data, pos, raw, 0, maxLength);
                            result.put(fieldNum, "[binary: " + EbcdicConverter.bytesToHex(raw).toUpperCase() + "]");
                            pos += maxLength;
                        }
                        break;
                    }
                    default:
                        result.put(fieldNum, "[unknown type]");
                }
            } catch (Exception e) {
                result.put(fieldNum, "[parse error: " + e.getMessage() + "]");
            }
        }

        return result;
    }

    /**
     * Appends the formatted decoded message to the given {@link StringBuilder}.
     * Uses {@code \n} throughout to ensure clean line-by-line delivery over sockets.
     */
    private void buildResults(StringBuilder sb, String mti,
                              List<Integer> enabledFields, Map<Integer, String> parsedFields) {

        sb.append("\n----------------------------------------------------------------\n");
        sb.append(" MTI : ").append(mti).append("  (").append(ISOFieldDictionary.getMtiDescription(mti)).append(")\n");
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
            sb.append("   Value       : ").append(parsedFields.getOrDefault(fieldNum, "[not parsed]")).append("\n");
            sb.append("   Description : ").append(ISOFieldDictionary.getDescription(fieldNum)).append("\n\n");
            count++;
        }

        if (count == 0) sb.append(" No fields were decoded.\n");

        sb.append("================================================================\n");
        sb.append(" Total fields decoded: ").append(count).append("\n");
        sb.append("================================================================\n\n");
    }
}
