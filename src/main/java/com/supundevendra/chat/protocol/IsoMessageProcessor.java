package com.supundevendra.chat.protocol;

import com.supundevendra.chat.exception.IsoProcessingException;
import com.supundevendra.chat.util.HexUtil;
import com.supundevendra.chat.util.StreamUtil;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.GenericPackager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

// Handles all ISO 8583 message logic for one sender session: unpack, dump, respond
public class IsoMessageProcessor {

    private static final Logger log = LoggerFactory.getLogger(IsoMessageProcessor.class);

    // ISO 8583 field number that carries the response code
    private static final int    FIELD_RESPONSE_CODE  = 39;
    // MTI for an authorization response message
    private static final String RESPONSE_MTI         = "0110";
    // Response code meaning "approved"
    private static final String APPROVED_CODE        = "00";
    // Type byte written before a binary ISO response frame
    public  static final byte   TYPE_BINARY_RESPONSE = 0x01;

    // Shared packager used for pack and unpack operations
    private final GenericPackager packager;

    // Constructor: obtains the shared packager from PackagerLoader
    public IsoMessageProcessor() {
        this.packager = PackagerLoader.getPackager();
    }

    // Reads one ISO frame from the stream: 2-byte length header then that many payload bytes
    public byte[] readFrame(InputStream in) throws IOException {
        byte[] header = StreamUtil.readExact(in, 2);                       // read 2-byte length prefix
        int len = ((header[0] & 0xFF) << 8) | (header[1] & 0xFF);         // combine bytes into int length
        log.debug("Reading ISO frame of {} bytes", len);
        return StreamUtil.readExact(in, len);                              // read the ISO payload
    }

    // Unpacks raw ISO 8583 bytes into an ISOMsg object; throws IsoProcessingException on failure
    public ISOMsg unpack(byte[] rawBytes) {
        try {
            ISOMsg msg = new ISOMsg();          // create empty message container
            msg.setPackager(packager);          // attach the field definitions
            packager.unpack(msg, rawBytes);     // decode the bytes into fields
            log.debug("Unpacked ISO message MTI={}", msg.getMTI());
            return msg;
        } catch (Exception e) {
            throw new IsoProcessingException("Failed to unpack ISO 8583 message", e);
        }
    }

    // Builds a human-readable text dump of all fields in the message
    public String buildDump(ISOMsg msg, String addr) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("================================================================\n");
            sb.append(" ISO 8583 MESSAGE from ").append(addr).append("\n");  // show sender address
            sb.append("================================================================\n");
            sb.append(String.format("MTI       : %s%n", msg.getMTI()));      // print the message type
            for (int i = 2; i <= 128; i++) {                                 // iterate all possible fields
                if (msg.hasField(i)) {                                       // skip absent fields
                    String value = fieldToReadable(msg, i);                  // get printable value
                    if (!value.isEmpty()) {
                        sb.append(String.format("Field %3d : %s%n", i, value)); // append field line
                    }
                }
            }
            sb.append("================================================================\n");
            sb.append("--END-OF-DUMP--");  // marker used by client to detect end of dump
            return sb.toString();
        } catch (Exception e) {
            throw new IsoProcessingException("Failed to build field dump", e);
        }
    }

    // Packs a 0110 approval response and writes it to the output stream with a type+length header
    public void sendResponse(ISOMsg msg, OutputStream out) throws IOException {
        try {
            ISOMsg resp = (ISOMsg) msg.clone();              // copy the original message
            resp.setMTI(RESPONSE_MTI);                       // change MTI to 0110 response
            resp.set(FIELD_RESPONSE_CODE, APPROVED_CODE);    // set field 39 = "00" approved

            byte[] respBytes = packager.pack(resp);          // encode response to bytes
            byte[] frame = new byte[3 + respBytes.length];   // 1 type byte + 2 length bytes + payload
            frame[0] = TYPE_BINARY_RESPONSE;                 // type byte 0x01
            frame[1] = (byte) ((respBytes.length >> 8) & 0xFF); // high byte of length
            frame[2] = (byte) (respBytes.length & 0xFF);        // low byte of length
            System.arraycopy(respBytes, 0, frame, 3, respBytes.length); // copy payload after header

            out.write(frame);  // send the full framed response
            out.flush();       // ensure bytes leave the buffer immediately
            log.debug("Sent 0110 response ({} bytes)", respBytes.length);
        } catch (IOException e) {
            throw e; // let the caller handle I/O errors
        } catch (Exception e) {
            throw new IsoProcessingException("Failed to pack or send 0110 response", e);
        }
    }

    // Returns a printable value for a single ISO field
    // Text fields come back as strings; binary fields (e.g. PIN block) come back as hex
    private static String fieldToReadable(ISOMsg msg, int fieldNum) {
        String value = msg.getString(fieldNum);                       // try text decode first
        if (value != null) return value;                              // return if available
        byte[] raw = msg.getBytes(fieldNum);                          // fall back to raw bytes
        return (raw != null) ? "[HEX] " + HexUtil.toHex(raw) : "";   // show as hex or empty
    }
}
