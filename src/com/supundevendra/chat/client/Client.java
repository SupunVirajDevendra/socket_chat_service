package com.supundevendra.chat.client;

import java.io.IOException;

/**
 * ISO 8583 chat client entry point.
 *
 * <p>Parses the {@code --listen} / {@code --send} command-line flag and
 * delegates to {@link ListenerMode} or {@link SenderMode} respectively.
 * All connection and protocol logic lives in those classes.
 *
 * <pre>
 *   Usage:
 *     java Client            # defaults to sender mode
 *     java Client --send     # sender mode: paste hex, press Enter
 *     java Client --listen   # listener mode: prints broadcast field dumps
 * </pre>
 */
public class Client {

    public static void main(String[] args) throws IOException {
        String mode = (args.length > 0) ? args[0].toLowerCase() : "--send";

        if (mode.equals("--listen")) {
            ListenerMode.run();
        } else {
            SenderMode.run();
        }
    }
}
