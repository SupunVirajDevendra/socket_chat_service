package com.supundevendra.chat.protocol;

import com.supundevendra.chat.util.HexUtil;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.GenericPackager;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Handles ISO 8583 message processing for a single connected sender session.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Unpacking raw bytes into an {@link ISOMsg}</li>
 *   <li>Building a human-readable field dump</li>
 *   <li>Packing and sending a {@code 0110} approval response</li>
 * </ul>
 *
 * <p>Instantiate once per sender connection and call
 * {@link #unpack(byte[])}, {@link #buildDump(ISOMsg, String)},
 * and {@link #sendResponse(ISOMsg, OutputStream)} as needed.
 */
public class IsoMessageProcessor {

    private static final int FIELD_RESPONSE_CODE = 39;
    private static final String RESPONSE_MTI      = "0110";
    private static final String APPROVED_CODE     = "00";

    /** Type byte written before a binary ISO response frame. */
    public static final byte TYPE_BINARY_RESPONSE = 0x01;

    private final GenericPackager packager;

    public IsoMessageProcessor() {
        this.packager = PackagerLoader.getPackager();
    }

    /**
     * Unpacks {@code rawBytes} into an {@link ISOMsg}.
     */
    public ISOMsg unpack(byte[] rawBytes) throws Exception {
        ISOMsg msg = new ISOMsg();
        msg.setPackager(packager);
        packager.unpack(msg, rawBytes);
        return msg;
    }

    /**
     * Reads an ISO 8583 payload from {@code in}: first 2-byte big-endian length
     * header, then that many bytes of ISO data.
     */
    public byte[] readFrame(InputStream in) throws IOException {
        byte[] header = com.supundevendra.chat.util.StreamUtil.readExact(in, 2);
        int len = ((header[0] & 0xFF) << 8) | (header[1] & 0xFF);
        return com.supundevendra.chat.util.StreamUtil.readExact(in, len);
    }

    /**
     * Builds a human-readable dump of all ISO 8583 fields in {@code msg}.
     *
     * @param msg  the unpacked ISO message
     * @param addr the remote address of the sender, for display
     */
    public String buildDump(ISOMsg msg, String addr) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("================================================================\n");
        sb.append(" ISO 8583 MESSAGE from ").append(addr).append("\n");
        sb.append("================================================================\n");
        sb.append(String.format("MTI       : %s%n", msg.getMTI()));
        for (int i = 2; i <= 128; i++) {
            if (msg.hasField(i)) {
                String value = fieldToReadable(msg, i);
                if (!value.isEmpty()) {
                    sb.append(String.format("Field %3d : %s%n", i, value));
                }
            }
        }
        sb.append("================================================================\n");
        sb.append("--END-OF-DUMP--");
        return sb.toString();
    }

    /**
     * Packs a {@code 0110} approval response and writes it to {@code out}
     * prefixed with type byte {@code 0x01} and a 2-byte big-endian length header.
     */
    public void sendResponse(ISOMsg msg, OutputStream out) throws Exception {
        ISOMsg resp = (ISOMsg) msg.clone();
        resp.setMTI(RESPONSE_MTI);
        resp.set(FIELD_RESPONSE_CODE, APPROVED_CODE);

        byte[] respBytes = packager.pack(resp);
        byte[] frame = new byte[3 + respBytes.length];
        frame[0] = TYPE_BINARY_RESPONSE;
        frame[1] = (byte) ((respBytes.length >> 8) & 0xFF);
        frame[2] = (byte) (respBytes.length & 0xFF);
        System.arraycopy(respBytes, 0, frame, 3, respBytes.length);

        out.write(frame);
        out.flush();
    }

    /**
     * Returns a human-readable value for a single ISO 8583 field.
     * Text/numeric fields are returned as-is (jPOS decodes EBCDIC during unpack).
     * Truly binary fields (PIN block, MACs) are returned as {@code "[HEX] ..."}.
     */
    private static String fieldToReadable(ISOMsg msg, int fieldNum) {
        String value = msg.getString(fieldNum);
        if (value != null) return value;
        byte[] raw = msg.getBytes(fieldNum);
        return (raw != null) ? "[HEX] " + HexUtil.toHex(raw) : "";
    }
}
