/*
 *
 * TCPServer from Kurose and Ross
 * Compile: javac TCPServer.java
 * Run: java TCPServer
 */

import java.io.*;
import java.net.*;

public class WebServer {
    static DataOutputStream outToClient = null;
    static final String HTML_START = "<html>" +
            "<body>";

    static final String HTML_END = "</body>" +
            "</html>";

    public static void main(String[] args) throws Exception {
        /*
         * define socket parameters, Address + PortNo, Address will default to localhost
         */
        if (args.length != 1) {
            System.out.println("Required argument: port.");
            return;
        }
        int serverPort = Integer.parseInt(args[0]);
        /* change above port number if required */

        /*
         * create server socket that is assigned the serverPort (6789)
         * We will listen on this port for connection request from clients
         */
        ServerSocket welcomeSocket = new ServerSocket(serverPort);
        System.out.println("Server is ready :");

        while (true) {

            // accept connection from connection queue
            Socket connectionSocket = welcomeSocket.accept();
            /*
             * When a client knocks on this door, the program invokes the accept( ) method
             * for welcomeSocket, which creates a new socket in the server, called
             * connectionSocket, dedicated to this particular client. The client and server
             * then complete the handshaking, creating a TCP connection between the client’s
             * clientSocket and the server’s connectionSocket. With the TCP connection
             * established, the client and server can now send bytes to each other over the
             * connection. With TCP, all bytes sent from one side not are not only
             * guaranteed to arrive at the other side but also guaranteed to arrive in order
             */

            // create read stream to get input
            BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
            String clientSentence;
            clientSentence = inFromClient.readLine();
            // data from client is stored in clientSentence
            // process input, change the case
            String messages[] = clientSentence.split(" ");
            String data = messages[1];
            data = data.substring(1);

            // send reply
            outToClient = new DataOutputStream(connectionSocket.getOutputStream());
            if (new File(data).isFile()) {
                sendResponse(200, data, true);
            } else if (data == "favicon.ico") {
                sendResponse(204, "<b>204 No Content", false);
            } else {
                sendResponse(404, "<b>404 Not Found", false);
            }

            connectionSocket.close();
            /*
             * In this program, after sending the capitalized sentence to the client, we
             * close the connection socket. But since welcomeSocket remains open, another
             * client can now knock on the door and send the server a sentence to modify.
             */
        } // end of while (true)

    } // end of main()

    public static void sendResponse(int statusCode, String responseString, boolean isFile) throws Exception {

        String statusLine = null;
        String serverdetails = "Server: Java HTTPServer";
        String contentLengthLine = null;
        String fileName = null;
        String contentTypeLine = "Content-Type: text/html" + "\r\n";
        FileInputStream fin = null;

        if (statusCode == 200)
            statusLine = "HTTP/1.1 200 OK" + "\r\n";
        else if (statusCode == 204) {
            statusLine = "HTTP/1.1 204 No Content" + "\r\n";
        } else
            statusLine = "HTTP/1.1 404 Not Found" + "\r\n";

        if (isFile) {
            fileName = responseString;
            fin = new FileInputStream(fileName);
            contentLengthLine = "Content-Length: " + Integer.toString(fin.available()) + "\r\n";
        } else {
            responseString = WebServer.HTML_START + responseString + WebServer.HTML_END;
            contentLengthLine = "Content-Length: " + responseString.length() + "\r\n";
        }

        outToClient.writeBytes(statusLine);
        outToClient.writeBytes(serverdetails);
        outToClient.writeBytes(contentTypeLine);
        outToClient.writeBytes(contentLengthLine);
        outToClient.writeBytes("Connection: close\r\n");
        outToClient.writeBytes("\r\n");

        if (isFile)
            sendFile(fin, outToClient);
        else
            outToClient.writeBytes(responseString);

        outToClient.close();
    }

    public static void sendFile(FileInputStream fin, DataOutputStream out) throws Exception {
        byte[] buffer = new byte[1024];
        int bytesRead;

        while ((bytesRead = fin.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
        }
        fin.close();
    }
} // end of class TCPServer
