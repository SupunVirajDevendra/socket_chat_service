package com.supundevendra.iso;

import java.util.Scanner;

/**
 * Main
 *
 * CLI entry point for the ISO 8583 Mastercard Message Decoder.
 *
 * Usage:
 *   Run the program, then paste a raw Mastercard ISO 8583 hex message.
 *   The message can be in either of these formats:
 *
 *   Format 1 - plain hex:
 *     016DF0F1F0F0F67F4401A8E9A00A000000000080000...
 *
 *   Format 2 - with Mastercard prefix:
 *     Received message from MASTERCARD:
 *     016DF0F1F0F0F67F4401A8E9A00A000000000080000...
 *
 *   Type 'exit' or 'quit' to stop.
 */
public class Main {

    private static final String SEPARATOR =
            "================================================================";

    public static void main(String[] args) {
        System.out.println(SEPARATOR);
        System.out.println("  ISO 8583 Mastercard Message Decoder");
        System.out.println("  Custom Field Walker (EBCDIC/Binary)");
        System.out.println(SEPARATOR);
        System.out.println("  Supports: EBCDIC->ASCII, 2-byte header strip,");
        System.out.println("            primary + secondary bitmap (fields 1-128)");
        System.out.println(SEPARATOR);

        ISOMessageParser parser;
        try {
            parser = new ISOMessageParser();
        } catch (Exception e) {
            System.out.println("[FATAL] Could not initialize ISO parser: " + e.getMessage());
            return;
        }

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\nPaste ISO 8583 hex message below (or type 'exit' to quit).");
            System.out.println("For multi-line input (with 'Received message from MASTERCARD:' prefix),");
            System.out.println("paste all lines then press Enter twice.\n");
            System.out.print("> ");

            StringBuilder inputBuilder = new StringBuilder();
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();

                if (line.trim().equalsIgnoreCase("exit") ||
                        line.trim().equalsIgnoreCase("quit")) {
                    System.out.println("\nGoodbye.");
                    return;
                }

                if (line.trim().isEmpty()) {
                    if (inputBuilder.length() > 0) break;
                    continue;
                }

                inputBuilder.append(line).append("\n");

                String trimmed = line.trim();
                if (trimmed.matches("[0-9A-Fa-f]+") && trimmed.length() > 8) {
                    break;
                }
            }

            String input = inputBuilder.toString().trim();
            if (input.isEmpty()) {
                System.out.println("[WARN] No input received. Please paste a hex message.");
                continue;
            }

            parser.parse(input);

            System.out.println("\nPress Enter to decode another message, or type 'exit' to quit.");
        }
    }
}
