# Socket Chat Service with ISO 8583 Message Decoding

A multi-threaded Java chat application that enables real-time communication between multiple clients through a central server. When a client sends a raw Mastercard or Visa ISO 8583 hex message, the server automatically decodes it and broadcasts the full field-by-field breakdown to all connected clients.

## Features

- **Multi-client Support**: Concurrent handling of multiple client connections
- **Real-time Messaging**: Instant message broadcasting to all connected clients
- **Private Messaging**: Direct messaging between specific users using `@username` syntax
- **ISO 8583 Decoding**: Paste any raw Mastercard or Visa ISO 8583 hex message in chat — the server auto-detects and broadcasts the fully decoded output
- **Card Network Detection**: Automatically identifies VISA, MASTERCARD, AMEX, or DISCOVER from the PAN (Field 2)
- **User Management**: Automatic username assignment and connection tracking
- **Chat Logging**: Persistent logging of all chat activities to `chat.txt`
- **Graceful Disconnection**: Clean handling of client exits and connection errors

## Architecture

The application consists of three main components:

### Server (`Server.java`)
- Listens for incoming client connections on port 12346
- Maintains a synchronized list of connected clients
- Handles message broadcasting and private message routing
- Logs server events and errors

### Client Handler (`ClientHandler.java`)
- Manages individual client connections
- Handles username authentication
- **Detects ISO 8583 hex messages** and routes them through the ISO parser
- Processes incoming messages and routes them appropriately
- Manages client disconnection and cleanup

### Client (`Client.java`)
- Connects to the chat server
- Provides command-line interface for sending/receiving messages
- Supports both public and private messaging
- Handles concurrent message sending and receiving with a daemon receive thread

## ISO 8583 Chat Feature

When any connected client sends a pure hex string of 20 or more characters (no spaces), the server treats it as an ISO 8583 message:

1. The server parses the hex using the built-in `ISOMessageParser`
2. Detects the card network (VISA / MASTERCARD / AMEX / DISCOVER) from the PAN in Field 2
3. Broadcasts the fully decoded field-by-field output to all other connected clients
4. Sends a confirmation back to the sender

### Example

**Client A types:**
```
016DF0F1F0F0F67F4401A8E9A00A00000000800000...
```

**All other clients receive:**
```
================================================================
 [Alice sent a MASTERCARD ISO 8583 message]
================================================================

 ISO 8583 MESSAGE DECODER
================================================================
 Raw hex length  : 220 chars (110 bytes)
 Length header   : 016D (stripped, value=365)
 Bitmap (hex)    : F07440010A000000
 Bitmap size     : 64-bit  (primary only)
 Card Network    : MASTERCARD

----------------------------------------------------------------
 MTI : 0100  (Authorization Request)
----------------------------------------------------------------

 Enabled fields detected from bitmap:
 2, 3, 4, 7, 11, 12, 13, 22, 25, 35, 41, 42, 49

----------------------------------------------------------------
 Decoded Message
----------------------------------------------------------------

 Field 2
   Name        : Primary Account Number (PAN)
   Value       : 5412345678901234
   Description : Card number used for the transaction

 Field 3
   Name        : Processing Code
   Value       : 000000
   Description : Transaction type and account type identifier

 ...
================================================================
 Total fields decoded: 13
================================================================
```

### ISO Detection Rules

| Condition | Treated as |
|-----------|-----------|
| Pure hex, no spaces, 20+ chars | ISO 8583 message |
| Text with spaces | Normal chat message |
| `@username ...` | Private message |
| `exit` | Disconnect |

### Card Network Detection (Field 2 PAN Prefix)

| Prefix | Network |
|--------|---------|
| `4xxx` | VISA |
| `51`–`55` | MASTERCARD |
| `2221`–`2720` | MASTERCARD |
| `34`, `37` | AMEX |
| `6011`, `622xxx`, `644`–`649`, `65xx` | DISCOVER |

## Requirements

- Java Development Kit (JDK) 17 or higher
- Maven 3.6+

## Build

```bash
mvn clean package
```

This produces a fat JAR at `target/assignment4-1.0-SNAPSHOT.jar`.

## Usage

### Starting the Server
```bash
java -cp target/assignment4-1.0-SNAPSHOT.jar com.supundevendra.chat.server.Server
```
The server will start on port 12346 and display "Server started..." when ready.

### Connecting Clients
Open new terminal windows and run:
```bash
java -cp target/assignment4-1.0-SNAPSHOT.jar com.supundevendra.chat.client.Client
```

Each client will be prompted to enter a username. Once connected:

- **Send public messages**: Simply type your message and press Enter
- **Send private messages**: Use `@username message` format
- **Send an ISO 8583 message**: Paste the raw hex string (no spaces) and press Enter
- **Exit the chat**: Type `exit` and press Enter

### Running the ISO 8583 CLI Decoder (standalone)
```bash
java -jar target/assignment4-1.0-SNAPSHOT.jar
```

## Commands

| Command | Description |
|---------|-------------|
| `@username message` | Send a private message to a specific user |
| `exit` | Disconnect from the chat server |
| `<hex string>` | Decode and broadcast an ISO 8583 message |
| Any other text | Send a public message to all connected users |

## File Structure

```
Assignment4/
├── src/
│   └── com/supundevendra/
│       ├── chat/
│       │   ├── client/
│       │   │   └── Client.java          # Chat client application
│       │   └── server/
│       │       ├── Server.java          # Main server implementation
│       │       └── ClientHandler.java   # Per-client handler + ISO detection
│       └── iso/
│           ├── Main.java                # Standalone ISO 8583 CLI decoder
│           ├── ISOMessageParser.java    # ISO 8583 parser (EBCDIC/binary)
│           ├── ISOFieldDictionary.java  # Field name/description dictionary
│           ├── EbcdicConverter.java     # EBCDIC <-> ASCII utilities
│           └── packager.xml             # jPOS GenericPackager field definitions
├── pom.xml                              # Maven build (Java 17, jPOS 2.1.9)
├── chat.txt                             # Chat log file (auto-generated)
└── README.md                            # This file
```

## Technical Details

- **Port**: 12346 (configurable in `Server.java`)
- **Protocol**: TCP/IP sockets
- **Threading**: Multi-threaded server with separate thread per client; daemon receive thread in client
- **ISO Parsing**: Custom EBCDIC field walker supporting 128 ISO 8583 fields
- **Logging**: File-based logging to `chat.txt`
- **Default Username**: `anura` (assigned when no username is provided)

## Error Handling

- Connection failures and socket errors
- Invalid or unparseable ISO 8583 hex — server broadcasts `[ISO Parse Error]` instead of crashing
- Client disconnections (clean exit or unexpected drop)
- File I/O operations

## Security Considerations

This is a basic educational implementation and does not include:
- Authentication or authorization
- Message encryption
- Input validation beyond basic checks
- Protection against malicious clients

For production use, consider implementing additional security measures.
