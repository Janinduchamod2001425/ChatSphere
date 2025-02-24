package chatserver;

import java.awt.Color;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * A simple Swing-based client for the chat server.  Graphically
 * it is a frame with a text field for entering messages and a
 * TextArea to see the whole dialog.
 *
 * The client follows the Chat Protocol which is as follows.
 * When the server sends "SUBMITNAME" the client replies with the
 * desired screen name.  The server will keep sending "SUBMITNAME"
 * requests as long as the client submits screen names that are
 * already in use.  When the server sends a line beginning
 * with "NAMEACCEPTED" the client is now allowed to start
 * sending the server arbitrary strings to be broadcast to all
 * chatters connected to the server.  When the server sends a
 * line beginning with "MESSAGE " then all characters following
 * this string should be displayed in its message area.
 */

public class ChatClient {

    BufferedReader in; // Input stream to read messages from the server.
    PrintWriter out; // Output stream to send messages to the server.
    JFrame frame = new JFrame("Chatter Box :)"); // The main window of the client.
    JTextField textField = new JTextField(40); // Text field for entering messages.
    JTextArea messageArea = new JTextArea(8, 40); // Text area to display the chat history.
    JList<String> clientList = new JList<String>(); // List to display connected clients.
    DefaultListModel<String> listModel = new DefaultListModel<String>(); // Model for the client list
    JCheckBox broadcastCheckBox = new JCheckBox("Broadcast"); // Checkbox to enable broadcasting.

    /**
     * Constructs the client by laying out the GUI and registering a
     * listener with the textfield so that pressing Return in the
     * listener sends the textfield contents to the server.  Note
     * however that the textfield is initially NOT editable, and
     * only becomes editable AFTER the client receives the NAMEACCEPTED
     * message from the server.
     */
    public ChatClient() {

        // Layout GUI
        textField.setEditable(false);
        messageArea.setEditable(false);
        frame.getContentPane().add(new JScrollPane(messageArea), "Center");
        clientList.setModel(listModel);
        frame.getContentPane().add(new JScrollPane(clientList), "West");
        frame.getContentPane().add(broadcastCheckBox, "South");

        // Create a panel to hold the text field and the send button at the top right
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        inputPanel.add(textField);

        // Add an action listener to the JButton to handle message sending.
        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(new ActionListener() {
            /**
             * Responds to click the JButton by sending
             * the contents of the text field to the server. Then clear
             * the text area in preparation for the next message.
             */
            public void actionPerformed(ActionEvent e) {

                String message = textField.getText(); // Get the message from the text field.
                if(message.isEmpty()) {
                    // Ignore empty messages
                    return;
                }
                if(broadcastCheckBox.isSelected()) {
                    // Broadcast the message to all clients
                    out.println(message);
                } else {
                    // Send the message to selected client(s)
                    String[] selectedClients = clientList.getSelectedValuesList().toArray(new String[0]);

                    // Show a warning if no recipient is selected.
                    if(selectedClients.length == 0) {
                        JOptionPane.showMessageDialog(frame, "Please select a recipient from the list", "No Recipient Selected", JOptionPane.WARNING_MESSAGE);
                    } else {                        
                        String recipients = String.join(",", selectedClients); // Join the selected clients into a comma-separated string.
                        out.println("TO:" + recipients + ":" + message); // Send the message to the server.
                    }

                }
                textField.setText(""); // Clear the input field
            }
        });
        
        // Add hover effect to send button
        sendButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                sendButton.setBackground(new Color(0, 128, 230)); // Darker blue on hover
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                sendButton.setBackground(new Color(0, 153, 255)); // Original color when mouse leaves
            }
        });

        
        inputPanel.add(sendButton);
        frame.getContentPane().add(inputPanel, "North"); // Add the input panel to the top

        frame.pack(); 

        // Set different fonts for each component
        Font textFieldFont = new Font("Segoe UI", Font.PLAIN, 14); // Font for the text field
        Font messageAreaFont = new Font("Consolas", Font.PLAIN, 12); // Monospaced font for the message area
        Font clientListFont = new Font("Arial", Font.BOLD, 12); // Bold font for the client list
        Font checkboxFont = new Font("Verdana", Font.ITALIC, 12); // Italic font for the checkbox

        // Apply fonts to components
        textField.setFont(textFieldFont);
        messageArea.setFont(messageAreaFont);
        clientList.setFont(clientListFont);
        broadcastCheckBox.setFont(checkboxFont);

        // Set a modern color scheme
        Color backgroundColor = new Color(240, 240, 240); // Light gray background
        Color textColor = new Color(50, 50, 50); // Dark gray text

        // Apply colors to components
        frame.getContentPane().setBackground(backgroundColor);
        textField.setBackground(Color.WHITE);
        textField.setForeground(textColor);
        messageArea.setBackground(Color.WHITE);
        messageArea.setForeground(textColor);
        clientList.setForeground(textColor);
        broadcastCheckBox.setBackground(backgroundColor);
        broadcastCheckBox.setForeground(textColor);
        
        clientList.setBackground(new Color(230, 240, 255)); // Soft light blue
        clientList.setSelectionBackground(new Color(173, 216, 230)); // Highlight color when selected
        clientList.setSelectionForeground(Color.BLACK); // Text color when selected
        
        // Customize the Send button appearance
        sendButton.setBackground(new Color(0, 153, 255)); // Light Blue Background
        sendButton.setForeground(Color.WHITE); // White Text
        sendButton.setFont(new Font("Arial", Font.BOLD, 14)); // Bold text
        sendButton.setFocusPainted(false); // Remove focus border
        sendButton.setBorderPainted(false); // Remove border
        sendButton.setOpaque(true); // Make sure the background color is applied
    }

    /**
     * Prompt for and return the address of the server.
     */
    private String getServerAddress() {
        return JOptionPane.showInputDialog(
            frame,
            "Enter IP Address of the Server:",
            "Welcome to the Chatter Box :)",
            JOptionPane.QUESTION_MESSAGE);
    }

    /**
     * Prompt for and return the desired screen name.
     */
    private String getName() {
        return JOptionPane.showInputDialog(
            frame,
            "Choose a screen name:",
            "Screen name selection",
            JOptionPane.PLAIN_MESSAGE);
    }

    /**
     * Connects to the server then enters the processing loop.
     */
    private void run() throws IOException {

        // Make connection and initialize streams
        String serverAddress = getServerAddress(); // Get the server address from the user.
        Socket socket = new Socket(serverAddress, 9001); // Connect to the server.
        in = new BufferedReader(new InputStreamReader(socket.getInputStream())); // Initialize the input stream.
        out = new PrintWriter(socket.getOutputStream(), true); // Initialize the output stream.

        // Process all messages from server, according to the protocol.
        while (true) {
            String line = in.readLine(); // Read a message from the server.
            if (line.startsWith("SUBMITNAME")) {
                out.println(getName()); // Send the chosen screen name to the server.
            } else if (line.startsWith("NAMEACCEPTED")) {
                textField.setEditable(true); // Enable the text field after the name is accepted.
            } else if (line.startsWith("MESSAGE")) {
                messageArea.append(line.substring(8) + "\n");  // Display the message in the message area.
            } else if (line.startsWith("CLIENTLIST")) {

                // Update the client list
                String[] clients = line.substring(10).split(","); // Extract the list of clients.
                listModel.clear(); // Clear the current client list.

                for(String client : clients) {
                    if(!client.isEmpty()) {                        
                        listModel.addElement(client); // Add each client to the list.
                    }
                }
            }
        }
    }

    /**
     * Runs the client as an application with a closeable frame.
     */
    public static void main(String[] args) throws Exception {
        ChatClient client = new ChatClient(); // Create a new client instance.
        client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Close the application when the window is closed.
        client.frame.setVisible(true); // Make the window visible.
        client.run(); // Start the client.
    }
}
