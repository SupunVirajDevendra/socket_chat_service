package com.supundevendra.chat.client;

import java.io.IOException;

// Entry point: reads the command-line flag and delegates to the correct client mode
public class Client {

    public static void main(String[] args) throws IOException {
        // Default to sender mode if no argument is provided
        String mode = (args.length > 0) ? args[0].toLowerCase() : "--send";

        if (mode.equals("--listen")) {
            ListenerMode.run(); // passive mode — only receives broadcast dumps
        } else {
            SenderMode.run();   // active mode — sends ISO messages and receives responses
        }
    }
}
