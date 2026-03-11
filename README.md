# Socket Chat Service

A multi-threaded Java chat application that enables real-time communication between multiple clients through a central server. This project demonstrates fundamental socket programming concepts and concurrent client handling in Java.

## Features

- **Multi-client Support**: Concurrent handling of multiple client connections
- **Real-time Messaging**: Instant message broadcasting to all connected clients
- **Private Messaging**: Direct messaging between specific users using `@username` syntax
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
- Processes incoming messages and routes them appropriately
- Manages client disconnection and cleanup

### Client (`Client.java`)
- Connects to the chat server
- Provides command-line interface for sending/receiving messages
- Supports both public and private messaging
- Handles concurrent message sending and receiving

## Requirements

- Java Development Kit (JDK) 8 or higher
- No external dependencies required

## Installation & Setup

1. Clone or download the project
2. Navigate to the project directory
3. Compile the Java files:
   ```bash
   javac src/code2/*.java
   ```

## Usage

### Starting the Server
```bash
java -cp src code2.Server
```
The server will start on port 12346 and display "Server started..." when ready.

### Connecting Clients
Open new terminal windows and run:
```bash
java -cp src code2.Client
```

Each client will be prompted to enter a username. Once connected, you can:

- **Send public messages**: Simply type your message and press Enter
- **Send private messages**: Use `@username message` format
- **Exit the chat**: Type `exit` and press Enter

## Commands

| Command | Description |
|---------|-------------|
| `@username message` | Send a private message to a specific user |
| `exit` | Disconnect from the chat server |
| Any text | Send a public message to all connected users |

## File Structure

```
Assignment4/
├── src/
│   └── code2/
│       ├── Server.java          # Main server implementation
│       ├── ClientHandler.java   # Client connection handler
│       └── Client.java          # Client application
├── chat.txt                     # Chat log file (auto-generated)
└── README.md                    # This file
```

## Technical Details

- **Port**: 12346 (configurable in source code)
- **Protocol**: TCP/IP sockets
- **Threading**: Multi-threaded server with separate thread per client
- **Logging**: File-based logging with timestamp entries
- **Default Username**: "anura" (assigned when no username is provided)

## Error Handling

The application includes comprehensive error handling for:
- Connection failures
- Socket communication errors
- File I/O operations
- Client disconnections

## Security Considerations

This is a basic educational implementation and does not include:
- Authentication or authorization
- Message encryption
- Input validation beyond basic checks
- Protection against malicious clients

For production use, consider implementing additional security measures.

## Contributing

Feel free to submit issues, feature requests, or pull requests to improve this chat application.

## License

This project is provided for educational purposes. Feel free to use and modify as needed.
