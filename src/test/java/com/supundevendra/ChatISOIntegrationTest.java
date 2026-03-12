package com.supundevendra;

import com.supundevendra.chat.server.Server;
import com.supundevendra.chat.server.ClientHandler;
import com.supundevendra.iso.ISOMessageParser;
import com.supundevendra.iso.EbcdicConverter;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

/**
 * Integration test: verifies that when a chat client sends a Mastercard/Visa
 * ISO 8583 hex message, all other clients receive the fully decoded field output.
 *
 * Run with:
 *   javac -cp target/assignment4-1.0-SNAPSHOT.jar -d target/test-classes src/test/java/com/supundevendra/ChatISOIntegrationTest.java
 *   java  -cp "target/assignment4-1.0-SNAPSHOT.jar;target/test-classes" com.supundevendra.ChatISOIntegrationTest
 */
public class ChatISOIntegrationTest {

    // A minimal Mastercard EBCDIC-encoded ISO 8583 hex message
    // Header: 0060  (96 bytes follow)
    // MTI: F0F1F0F0 = "0100" in EBCDIC  (Authorization Request)
    // Bitmap: F07440010A000000  (fields 2,3,4,7,11,12,13,22,25,41,42,49)
    // Field data in EBCDIC: carefully crafted fixed-length fields
    //   F2 = 19-char PAN with LL prefix "16" -> "1651234567890123456"
    //   F3 = "000000"  (6)
    //   F4 = "000000010000"  (12)
    //   F7 = "0601120000"  (10)
    //   F11= "000001"  (6)
    //   F12= "120000"  (6)
    //   F13= "0601"  (4)
    //   F22= "051"  (3)
    //   F25= "00"  (2)
    //   F41= "TERM0001"  (8)
    //   F42= "MERCHANT000001 "  (15)
    //   F49= "840"  (3)
    // Encoded as EBCDIC hex:
    private static final String MASTERCARD_ISO_HEX = buildMastercardHex();

    private static String buildMastercardHex() {
        // Build the message programmatically using EBCDIC encoding
        try {
            java.nio.charset.Charset EBCDIC = java.nio.charset.Charset.forName("IBM037");

            // MTI: 0100
            byte[] mti = "0100".getBytes(EBCDIC); // 4 bytes

            // Primary bitmap: fields 2,3,4,7,11,12,13,22,25,41,42,49 present
            // F2=bit2, F3=bit3, F4=bit4, F7=bit7, F11=bit11, F12=bit12, F13=bit13
            // F22=bit22, F25=bit25, F41=bit41, F42=bit42, F49=bit49
            // Byte 1: bits 1-8:   0 1 1 1 0 0 0 1  = 0x71  (F2,F3,F4,F7 set; F1=secondary=0)
            // Byte 2: bits 9-16:  0 0 0 1 1 1 0 0  = 0x1C  (F11,F12,F13)
            // Byte 3: bits17-24:  0 0 0 0 0 1 0 0  = 0x04  (F22)
            // Byte 4: bits25-32:  1 0 0 0 0 0 0 0  = 0x80  (F25)
            // Byte 5: bits33-40:  0 0 0 0 0 0 0 0  = 0x00
            // Byte 6: bits41-48:  1 1 0 0 0 0 0 0  = 0xC0  (F41,F42)
            // Byte 7: bits49-56:  1 0 0 0 0 0 0 0  = 0x80  (F49)
            // Byte 8: bits57-64:  0 0 0 0 0 0 0 0  = 0x00
            byte[] bitmap = {(byte)0x71, (byte)0x1C, (byte)0x04, (byte)0x80,
                             (byte)0x00, (byte)0xC0, (byte)0x80, (byte)0x00};

            // Field data in EBCDIC
            // F2: LLVAR 19 max -> LL="16" + data="5123456789012345" (Mastercard)
            byte[] f2  = ("165123456789012345").getBytes(EBCDIC);
            // F3: FIXED 6
            byte[] f3  = "000000".getBytes(EBCDIC);
            // F4: FIXED 12
            byte[] f4  = "000000010000".getBytes(EBCDIC);
            // F7: FIXED 10
            byte[] f7  = "0601120000".getBytes(EBCDIC);
            // F11: FIXED 6
            byte[] f11 = "000001".getBytes(EBCDIC);
            // F12: FIXED 6
            byte[] f12 = "120000".getBytes(EBCDIC);
            // F13: FIXED 4
            byte[] f13 = "0601".getBytes(EBCDIC);
            // F22: FIXED 3
            byte[] f22 = "051".getBytes(EBCDIC);
            // F25: FIXED 2
            byte[] f25 = "00".getBytes(EBCDIC);
            // F41: FIXED 8
            byte[] f41 = "TERM0001".getBytes(EBCDIC);
            // F42: FIXED 15
            byte[] f42 = "MERCHANT000001 ".getBytes(EBCDIC);
            // F49: FIXED 3
            byte[] f49 = "840".getBytes(EBCDIC);

            // Assemble body (no header yet)
            ByteArrayOutputStream body = new ByteArrayOutputStream();
            body.write(mti);
            body.write(bitmap);
            body.write(f2); body.write(f3); body.write(f4); body.write(f7);
            body.write(f11); body.write(f12); body.write(f13);
            body.write(f22); body.write(f25);
            body.write(f41); body.write(f42); body.write(f49);

            byte[] bodyBytes = body.toByteArray();
            int bodyLen = bodyBytes.length;

            // 2-byte binary length header
            byte[] header = {(byte)((bodyLen >> 8) & 0xFF), (byte)(bodyLen & 0xFF)};

            ByteArrayOutputStream full = new ByteArrayOutputStream();
            full.write(header);
            full.write(bodyBytes);

            return EbcdicConverter.bytesToHex(full.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed to build test hex: " + e.getMessage(), e);
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== ChatISOIntegrationTest ===\n");

        // --- Unit test: detectCardNetwork ---
        System.out.println("--- Test 1: detectCardNetwork ---");
        assertEq("MASTERCARD", ISOMessageParser.detectCardNetwork("5123456789012345"), "MC 51xx");
        assertEq("MASTERCARD", ISOMessageParser.detectCardNetwork("5512345678901234"), "MC 55xx");
        assertEq("MASTERCARD", ISOMessageParser.detectCardNetwork("2221000000000000"), "MC 2221");
        assertEq("VISA",       ISOMessageParser.detectCardNetwork("4111111111111111"), "Visa 4xxx");
        assertEq("AMEX",       ISOMessageParser.detectCardNetwork("371234567890123"),  "Amex 37");
        assertEq("DISCOVER",   ISOMessageParser.detectCardNetwork("6011111111111117"), "Discover 6011");
        assertEq("UNKNOWN",    ISOMessageParser.detectCardNetwork(null),               "null PAN");
        System.out.println("  PASS: all card network detections correct\n");

        // --- Unit test: parseToString returns full multi-line output ---
        System.out.println("--- Test 2: parseToString returns multi-line decoded output ---");
        System.out.println("  Built test hex: " + MASTERCARD_ISO_HEX);
        ISOMessageParser parser = new ISOMessageParser();
        String decoded = parser.parseToString(MASTERCARD_ISO_HEX);
        System.out.println("  Decoded output (" + decoded.split("\n").length + " lines):\n");
        System.out.println(decoded);

        assertTrue(decoded.contains("ISO 8583 MESSAGE DECODER"), "contains header");
        assertTrue(decoded.contains("MASTERCARD"),               "contains MASTERCARD");
        assertTrue(decoded.contains("0100"),                     "contains MTI 0100");
        assertTrue(decoded.contains("5123456789012345"),         "contains PAN");
        assertTrue(decoded.contains("Authorization Request"),    "contains MTI description");
        assertTrue(decoded.split("\n").length > 10,              "output has >10 lines");
        System.out.println("  PASS: parseToString produces correct multi-line output\n");

        // --- Integration test: server + two sockets ---
        System.out.println("--- Test 3: Chat broadcast integration ---");

        // Start server in background thread
        ExecutorService exec = Executors.newSingleThreadExecutor();
        exec.submit(() -> {
            try { Server.main(new String[]{}); }
            catch (Exception e) { /* server killed */ }
        });
        Thread.sleep(500); // let server bind

        // Client B connects first (receiver)
        Socket sockB = new Socket("localhost", 12346);
        BufferedReader readerB = new BufferedReader(new InputStreamReader(sockB.getInputStream()));
        PrintWriter   writerB = new PrintWriter(new OutputStreamWriter(sockB.getOutputStream()), true);

        readerB.readLine(); // "Enter your username:"
        writerB.println("Bob");
        Thread.sleep(200);

        // Client A connects (sender)
        Socket sockA = new Socket("localhost", 12346);
        BufferedReader readerA = new BufferedReader(new InputStreamReader(sockA.getInputStream()));
        PrintWriter   writerA = new PrintWriter(new OutputStreamWriter(sockA.getOutputStream()), true);

        readerA.readLine(); // "Enter your username:"
        writerA.println("Alice");
        Thread.sleep(200);

        // Drain Bob's "Alice joined" message
        readerB.readLine();

        // Alice sends the Mastercard ISO hex
        writerA.println(MASTERCARD_ISO_HEX);
        Thread.sleep(2000); // wait for server to parse and broadcast

        // Read everything Bob received (with timeout)
        sockB.setSoTimeout(800);
        StringBuilder bobReceived = new StringBuilder();
        try {
            String line;
            while ((line = readerB.readLine()) != null) {
                bobReceived.append(line).append("\n");
            }
        } catch (SocketTimeoutException e) { /* done reading */ }

        System.out.println("  Bob received (" + bobReceived.toString().split("\n").length + " lines):\n");
        System.out.println(bobReceived);

        String bobOutput = bobReceived.toString();
        assertTrue(bobOutput.contains("Alice sent a MASTERCARD ISO 8583 message"), "header line broadcast");
        assertTrue(bobOutput.contains("ISO 8583 MESSAGE DECODER"),                 "decoder header broadcast");
        assertTrue(bobOutput.contains("0100"),                                      "MTI broadcast");
        assertTrue(bobOutput.contains("5123456789012345"),                          "PAN broadcast");
        assertTrue(bobOutput.contains("Primary Account Number"),                    "field name broadcast");
        System.out.println("  PASS: full ISO decoded output correctly broadcast to Bob\n");

        // Alice should get the server confirmation (not the decoded output)
        sockA.setSoTimeout(500);
        StringBuilder aliceReceived = new StringBuilder();
        try {
            String line;
            while ((line = readerA.readLine()) != null) {
                aliceReceived.append(line).append("\n");
            }
        } catch (SocketTimeoutException e) { /* done */ }
        System.out.println("  Alice received: " + aliceReceived.toString().trim());
        assertTrue(aliceReceived.toString().contains("ISO message decoded"), "sender gets confirmation");
        System.out.println("  PASS: sender gets confirmation only\n");

        // Cleanup
        writerA.println("exit");
        writerB.println("exit");
        Thread.sleep(200);
        sockA.close();
        sockB.close();
        exec.shutdownNow();

        System.out.println("=== ALL TESTS PASSED ===");
    }

    private static void assertEq(String expected, String actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError("FAIL [" + label + "]: expected=" + expected + " actual=" + actual);
        }
        System.out.println("  OK [" + label + "]: " + actual);
    }

    private static void assertTrue(boolean condition, String label) {
        if (!condition) {
            throw new AssertionError("FAIL [" + label + "]: condition was false");
        }
        System.out.println("  OK [" + label + "]");
    }
}
