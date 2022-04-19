import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.swing.text.html.HTMLDocument.RunElement;

public class Client {
    static DatagramSocket clientSocket;
    static InetAddress IPAddress;
    static int serverPort;

    static String userName;

    static int ACK = 0;
    static int Seq = 0;

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
        clientSocket.setSoTimeout(1000);

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
            String[] spec;
            if (commands != null) {
                command = (String) commands.get("command");
                spec = (String[]) commands.get("value");
            } else {
                continue;
            }
            // DEBUG
            // for (int i = 0; i < spec.length; i++) {
            // System.out.println(spec[i]);
            // }
            switch (command) {
                case "CRT":
                    createThread(spec);
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
        String name;
        System.out.print("Enter username: ");
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        name = inFromUser.readLine();
        if (authentication_name(name)) {
            // print out message and get password
            String psw;
            System.out.print("Enter password: ");
            inFromUser = new BufferedReader(new InputStreamReader(System.in));
            psw = inFromUser.readLine();
            return authentication_psw(psw);
        }
        return false;
    }

    private static boolean authentication_name(String name) throws Exception {
        try {
            UDPSend(String.join(" ", "name", name));
            String response = castResponse(UDPReceive());
            if (response.equals("TRUE")) {
                userName = name;
                return true;
            } else if (response.equals("FALSE")) {
                System.out.println("ERROR: Name not matched.\n");
                return false;
            } else if (response.equals("ONLINE")) {
                System.out.println("ERROR: Already logged in.\n");
                return false;
            }
        } catch (SocketTimeoutException e) {
            // Timeout, resent packet
            System.out.println("Warning: Packet Timeout.");
            authentication_name(name);
            return true;
        } catch (Exception e) {
            System.out.println("ERROR");
        }
        return false;

    }

    private static boolean authentication_psw(String psw) throws Exception {
        try {
            UDPSend(String.join(" ", "psw", userName, psw));
            String response = castResponse(UDPReceive());
            if (response.equals("TRUE")) {
                return true;
            } else if (response.equals("FALSE")) {
                System.out.println("ERROR: Incorrect password.\n");
                return false;
            }
        } catch (SocketTimeoutException e) {
            // Timeout, resent packet
            System.out.println("Warning: Packet Timeout.");
            authentication_psw(psw);
            return true;
        } catch (Exception e) {
            System.out.println("ERROR");
        }
        return false;
    }

    private static Map showMenu() throws Exception {
        String command = null;
        Map result = null;
        while (command == null) {
            System.out.print("Enter one of the following commands: "
                    + "CRT, MSG, LST, RDT, EDT, DLT, RMV, UPD, DWN,  XIT: ");
            BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
            command = inFromUser.readLine();
            // create list for command and loop through check if it's a valid command
            result = commandParse(command);
            if (result != null)
                return result;
            System.out.println("ERROR: Invalid command.");
        }
        return result;

    };

    private static void createThread(String[] command) throws Exception {
        if (command[0].split(" ").length != 1) {
            System.out.println("ERRROR: Invalid name.");
            return;
        }
        try {
            String data = String.join(" ", "CRT", stringArrayToString(command), userName);
            UDPSend(data);
            String response = castResponse(UDPReceive());
            if (response.equals("TRUE")) {
                System.out.println("Thread " + (String) command[0] + " created.");
            } else if (response.equals("FALSE")) {
                System.out.println("ERROR: Thread " + (String) command[0] + " exists.");
            }
        } catch (SocketTimeoutException e) {
            // Timeout, resent packet
            System.out.println("Warning: Packet Timeout.");
            createThread(command);
            return;
        } catch (Exception e) {
            System.out.println("ERROR");
        }
    }

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
        // prepare for sending
        byte[] sendData = new byte[1024];
        sentence = String.join(" ", Integer.toString(ACK), Integer.toString(Seq),
                Integer.toString(sentence.length()), sentence);
        sendData = sentence.getBytes();

        // write to server, need to create DatagramPAcket with server address and port
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, serverPort);
        // actual send call
        clientSocket.send(sendPacket);
        // set timeout
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
        // DEBUG
        System.out.println("MSG FROM SERVER:" + response);
        // parse the packet
        Map messages = UDPMessageParse(response);
        int ackRecieve = Integer.parseInt((String) messages.get("ACK"));
        int seqReceive = Integer.parseInt((String) messages.get("Seq"));
        int lengthReceive = Integer.parseInt((String) messages.get("Length"));
        response = (String) messages.get("message");
        Seq = ackRecieve;
        ACK = seqReceive + lengthReceive;
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
    };

    private static Map commandParse(String command) {
        List<String> commandList = Arrays.asList("CRT", "MSG", "LST", "RDT", "EDT", "DLT", "RMV", "UPD", "DWN",
                "XIT");
        Map result = null;
        for (int i = 0; i < commandList.size(); i++) {
            String name = commandList.get(i);
            if (command.contains(name)) {
                result = new HashMap<>();
                String[] spec = command.split(name + " ");
                String[] newSpec = { "" };
                System.arraycopy(spec, 1, newSpec, 0, spec.length - 1);
                result.put("command", name);
                result.put("value", newSpec);
                return result;
            }
        }
        return result;
    }

    private static Map UDPMessageParse(String line) {
        Map result = new HashMap<>();
        String[] ans = line.split(" ");
        result.put("ACK", ans[0]);
        result.put("Seq", ans[1]);
        result.put("Length", ans[2]);
        ArrayList message = new ArrayList<>();
        for (int i = 3; i < ans.length; i++) {
            message.add(ans[i]);
        }
        result.put("message", String.join(" ", message));
        // DEBUG
        // System.out.println((String) message.toString());
        return result;
    }

    private static String stringArrayToString(String[] input) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length; i++) {
            sb.append(input[i]);
            if (i != input.length - 1)
                sb.append(" ");
        }
        return sb.toString();
    }
}