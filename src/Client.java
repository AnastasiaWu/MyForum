import java.io.*;
import java.net.*;
import java.util.*;

public class Client {
    static DatagramSocket clientSocket;
    static InetAddress IPAddress;
    static int serverPort;

    public static void main(String[] args) throws Exception {
        // Check the input
        if (args.length != 1) {
            System.out.println("Please input the port number.");
            return;
        }
        // Define socket parameters, address and Port#
        IPAddress = InetAddress.getByName("localhost");
        serverPort = Integer.parseInt(args[0]);

        // Socket settle
        // create socket which connects to server
        clientSocket = new DatagramSocket();

        // Authentication
        while (true) {
            if (authentication()) {
                break;
            }
        }

        // Interaction with user
        while (true) {
            Map commands = showMenu();
            String command;
            if (commands != null) {
                command = (String) commands.get("command");
            } else {
                continue;
            }
            System.out.println(commands);
            switch (command) {
                case "CRT":
                    createThread();
                    break;
                case "MSG":
                    postMessage();
                    break;
                case "DLT":
                    deleteMessage();
                    break;
                case "EDT":
                    editMessage();
                    break;
                case "LST":
                    listThreads();
                    break;
                case "RDT":
                    readThread();
                    break;
                case "UPD":
                    uploadFile();
                    break;
                case "DWN":
                    downloadFile();
                    break;
                case "RMV":
                    removeThread();
                    break;
                case "XIT":
                    int code = exit();
                    if (code == 0) {
                        clientSocket.close();
                        return;
                    }
                    break;
                default:
                    // show error message
                    System.out.println("ERROR: Invalid command.");
                    break;
            }
        }

    };

    private static boolean authentication() throws Exception {
        // print out message and get username
        String name, psw;
        System.out.print("Enter username: ");
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        name = inFromUser.readLine();

        System.out.print("Enter password: ");
        inFromUser = new BufferedReader(new InputStreamReader(System.in));
        psw = inFromUser.readLine();
        // send user name and get validation
        int count = 0;
        while (count < 20) {
            try {
                UDPSend(String.join(" ", name, psw));
                String response = castResponse(UDPReceive());
                if (response.equals("TRUE")) {
                    return true;
                } else if (response.equals("FALSE")) {
                    System.out.println("ERROR: Incorrect password.\n");
                    return false;
                }
            } catch (Exception e) {
                // TODO: set error msg here
                System.out.println("ERROR: Packet Timeout.");
            }
        }
        return false;
    };

    private static Map showMenu() throws Exception {
        String command = null;
        Map result = null;
        while (command == null) {
            System.out.println("Enter one of the following commands: "
                    + "CRT, MSG, LST, RDT, EDT, DLT, RMV, UPD, DWN,  XIT: ");
            BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
            command = inFromUser.readLine();
            // create list for command and loop through check if it's a valid command
            List<String> commandList = Arrays.asList("CRT", "MSG", "LST", "RDT", "EDT", "DLT", "RMV", "UPD", "DWN",
                    "XIT");
            for (int i = 0; i < commandList.size(); i++) {
                String name = commandList.get(i);
                if (command.contains(name)) {
                    result = new HashMap<>();
                    result.put("command", name);
                    result.put("value", command.split(name));
                    return result;
                }
            }
            System.out.println("Invalid command.");
        }
        return result;
    };

    private static void createThread() {
    };

    private static void postMessage() {
    };

    private static void deleteMessage() {
    };

    private static void editMessage() {
    };

    private static void listThreads() {
    };

    private static void readThread() {
    };

    private static void uploadFile() {
    };

    private static void downloadFile() {
    };

    private static void removeThread() {
    };

    private static int exit() {
        return 0;
    };

    private static void UDPSend(String sentence) throws Exception {
        DatagramPacket request = new DatagramPacket(new byte[1024], 1024);
        // prepare for sending
        byte[] sendData = new byte[1024];
        sendData = sentence.getBytes();
        // write to server, need to create DatagramPAcket with server address and port
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, serverPort);
        // actual send call
        clientSocket.send(sendPacket);
        // clientSocket.setSoTimeout(600);
    };

    private static String UDPReceive() throws Exception {
        // prepare buffer to receive reply
        String response = null;
        byte[] receiveData = new byte[1024];
        // receive from server
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        clientSocket.receive(receivePacket);

        response = new String(receivePacket.getData());
        System.out.println("MSG FROM SERVER:" + response);
        return response;
    };

    private static void TCPSend() {
    };

    private static void TCPReceive() {
    };

    private static String castResponse(String response) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < response.length(); i++) {
            if (response.charAt(i) != '\0') {
                sb.append(response.charAt(i));
            } else {
                break;
            }
        }
        return sb.toString();
    }
}
