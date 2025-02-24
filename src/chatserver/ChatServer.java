package chatserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;

/**
 * A multi-threaded chat room server. When a client connects the
 * server requests a screen name by sending the client the
 * text "SUBMITNAME", and keeps requesting a name until
 * a unique one is received.  After a client submits a unique
 * name, the server acknowledges with "NAMEACCEPTED".  Then
 * all messages from that client will be broadcast to all other
 * clients that have submitted a unique screen name.  The
 * broadcast messages are prefixed with "MESSAGE ".
 *
 * Because this is just a teaching example to illustrate a simple
 * chat server, there are a few features that have been left out.
 * Two are very useful and belong in production code:
 *
 *     1. The protocol should be enhanced so that the client can
 *        send clean disconnect messages to the server.
 *
 *     2. The server should do some logging.
 */  
public class ChatServer {

    /**
     * The port that the server listens on.
     */
    private static final int PORT = 9001;

    /**
     * The set of all names of clients in the chat room.  Maintained
     * so that we can check that new clients are not registering name
     * already in use.
     */
    private static HashSet<String> names = new HashSet<String>();

    /**
     * The set of all the print writers for all the clients.  This
     * set is kept so we can easily broadcast messages.
     */
    private static HashSet<PrintWriter> writers = new HashSet<PrintWriter>();
    
    /**
     * A map to store client names and their corresponding PrintWriter objects.
     * This allows sending messages to specific clients
     */
    private static HashMap<String, PrintWriter> clientWriters = new HashMap<String, PrintWriter>();

    /**
     * The application main method, which just listens on a port and
     * spawns handler threads.
     */
    public static void main(String[] args) throws Exception {
        System.out.println("The chat server is running.");
        
        // Create a ServerSocket to listen for client connections on the specified port.
        ServerSocket listener = new ServerSocket(PORT);
        try {
        	// Continuously accept new client connections. 
            while (true) {
            	// Accept a new client connection
            	Socket socket  = listener.accept();
                
            	// Create a new thread to handle the client
            	Thread handlerThread = new Thread(new Handler(socket));
                
            	// Start the thread
            	handlerThread.start();
            }
        } finally {
        	// Close the ServerSocket when the server shuts down. 
            listener.close();
        }
    }

    /**
     * A handler thread class.  Handlers are spawned from the listening
     * loop and are responsible for a dealing with a single client
     * and broadcasting its messages.
     */
    private static class Handler implements Runnable {
        private String name; // The name of the client
        private Socket socket; // The socket connected to the client
        private BufferedReader in; // Input stream to read messages from the client
        private PrintWriter out; // Output stream to send messages to client

        /**
         * Constructs a handler thread, squirreling away the socket.
         * All the interesting work is done in the run method.
         */
        public Handler(Socket socket) {
            this.socket = socket; // Store the client socket
        }

        /**
         * Services this thread's client by repeatedly requesting a
         * screen name until a unique one has been submitted, then
         * acknowledges the name and registers the output stream for
         * the client in a global set, then repeatedly gets inputs and
         * broadcasts them.
         */
        public void run() {
            try {
                // Create character streams for the socket.
                in = new BufferedReader(new InputStreamReader(
                    socket.getInputStream())); // Initialize the input stream
                out = new PrintWriter(socket.getOutputStream(), true); // Initialize the output

                // Request a name from this client.  Keep requesting until
                // a name is submitted that is not already used.  
                // Note that checking for the existence of a name and adding the name
                // must be done while locking the set of names.
                while (true) {
                    out.println("SUBMITNAME"); // Ask the client to submit a name
                    name = in.readLine(); // Read the name submitted by client
                    if (name == null) {
                        return; // If the client disconnect, exit the loop
                    }
                    
                    // Ensure the thread safety of the shared variable 'names'
                    synchronized (names) {
                    	if(!names.contains(name)) { // Check if the name is unique.
                    		names.add(name); // Add the name to the set of names.
                    		break; // Exit the loop once the unique name is accepted.
                    	}
					}
                 }

                // Now that a successful name has been chosen, add the
                // socket's print writer to the set of all writers so
                // this client can receive broadcast messages.
                out.println("NAMEACCEPTED");
                writers.add(out); // Add the client's PritnWriter to the set of writers
                clientWriters.put(name, out); // Add the client's name and PrintWriter to the map
                
                // Broadcast the updated client list
                broadcastClientList();
                
                // Accept messages from this client and broadcast them.
                // Ignore other clients that cannot be broadcasted to.
                while (true) {
                    String input = in.readLine(); // Read the message from client
                    if (input == null) { 
                        return;
                    }
                    
                    // Check if the message is point to point
                    if(input.startsWith("TO:")) {
                    	String[] parts = input.split(":", 3); // Split the message into parts
                    	
                    	if (parts.length >= 3) { // Ensure the message is properly formatted                     		
                    		String[] recipients = parts[1].split(","); // Extract the recipient
                    		String message = parts[2]; // Extract the message
                    		
                    		// Send the message to the specified recipients
                            for (String recipient : recipients) {
                                PrintWriter recipientWriter = clientWriters.get(recipient); // Get the recipient PrintWriter
                                if (recipientWriter != null) {
                                    recipientWriter.println("MESSAGE " + name + ": " + message); // Send the message to recipient
                                }
                            }
                    	}
                    } else {
                    	
                    	// Broadcast the message to all clients
                    	for (PrintWriter writer : writers) {
                    		writer.println("MESSAGE " + name + ": " + input);
                    	}                    	
                    }
                    
                }
            }// TODO: Handle the SocketException here to handle a client closing the socket
            catch (IOException e) {
                System.out.println(e);
            } finally {
                // This client is going down!  Remove its name and its PrintWriter from the sets, and close its socket.
                if (name != null) {
                    names.remove(name); // Remove the client's name from the set
                    clientWriters.remove(name); // Remove the PrintWriter from the map
                    broadcastClientList(); // Broadcast the updated client list
                }
                if (out != null) {
                    writers.remove(out); // Remove the client's PrintWriter from the set
                }
                try {
                    socket.close(); // Close the client socket
                } catch (IOException e) {
                }
            }
        }
        
        /**
         * Broadcast the updated list of connected clients to all clients
         */
        private void broadcastClientList() {
        	StringBuilder clientList = new StringBuilder(); // Create s StringBuilder to build the client list
        	
        	for(String client : names) {
        		clientList.append(client).append(","); // Append each client to the list
        	}
        	
        	for(PrintWriter writer: writers) {
        		writer.println("CLIENTLIST" + clientList.toString()); // Send the client list to all clients
        	}
        }
    }
}