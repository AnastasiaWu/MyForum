import java.io.*;
import java.net.*;
import java.util.*;

public class Client {
    static DatagramSocket clientSocket;
    static InetAddress IPAddress;
    static int serverPort;

    static String userName;

    static int ACK = 0;
    static int Seq = 0;
    static boolean creatingNewUser = false;

    public static void main(String[] args) throws Exception {
        // Check the input
        if (args.length != 1) {
            System.out.println("Please input the port number");
            return;
        }
        // Define socket parameters, address and Port#
        IPAddress = InetAddress.getByName("localhost");
        serverPort = Integer.parseInt(args[0]);

        // Socket settle
        // create socket which connects to server
        clientSocket = new DatagramSocket();
        clientSocket.setSoTimeout(2500);

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
                    postMessage(spec);
                    break;
                case "DLT":
                    deleteMessage(spec);
                    break;
                case "EDT":
                    editMessage(spec);
                    break;
                case "LST":
                    listThreads(spec);
                    break;
                case "RDT":
                    readThread(spec);
                    break;
                case "UPD":
                    uploadFile(spec);
                    break;
                case "DWN":
                    downloadFile(spec);
                    break;
                case "RMV":
                    removeThread(spec);
                    break;
                case "XIT":
                    int code = exit(spec);
                    if (code == 0) {
                        clientSocket.close();
                        return;
                    }
                    break;
                default:
                    // show error message
                    System.out.println("Invalid command");
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
            if (creatingNewUser) {
                System.out.print("New user, enter password: ");
            } else {
                System.out.print("Enter password: ");
            }
            inFromUser = new BufferedReader(new InputStreamReader(System.in));
            psw = inFromUser.readLine();
            return authentication_psw(psw);
        }
        return false;
    }

    private static boolean authentication_name(String name) throws Exception {
        try {
            UDPSend(String.join(" ", "AUTH", "name", name));
            String response = castResponse(UDPReceive());
            userName = name;
            if (response.equals("TRUE")) {
                return true;
            } else if (response.equals("NEWUSER")) {
                creatingNewUser = true;
                return true;
            } else if (response.equals("ONLINE")) {
                System.out.println(userName + " has already logged in");
                return false;
            }
        } catch (SocketTimeoutException e) {
            // Timeout, resent packet
            System.out.println("Warning: Packet Timeout");
            authentication_name(name);
            return true;
        } catch (Exception e) {
            System.out.println("ERROR");
            e.printStackTrace();
        }
        return false;

    }

    private static boolean authentication_psw(String psw) throws Exception {
        try {
            UDPSend(String.join(" ", "AUTH", "psw", userName, psw));
            String response = castResponse(UDPReceive());
            if (response.equals("TRUE")) {
                System.out.println("Welcome to the forum");
                return true;
            } else if (response.equals("FALSE")) {
                System.out.println("Invalid password");
                return false;
            }
        } catch (SocketTimeoutException e) {
            // Timeout, resent packet
            System.out.println("Warning: Packet Timeout");
            authentication_psw(psw);
            return true;
        } catch (Exception e) {
            System.out.println("ERROR");
            e.printStackTrace();
        }
        return false;
    }

    private static Map showMenu() throws Exception {
        String command = null;
        Map result = null;
        while (command == null) {
            System.out.print("Enter one of the following commands: "
                    + "CRT, MSG, LST, RDT, EDT, DLT, RMV, UPD, DWN, XIT: ");
            BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
            command = inFromUser.readLine();
            // create list for command and loop through check if it's a valid command
            result = commandParse(command);
            if (result != null)
                return result;
            System.out.println("Invalid command");
        }
        return result;

    };

    private static void createThread(String[] command) throws Exception {
        if (command == null || command[0].split(" ").length != 1) {
            System.out.println("Incorrect syntax for CRT");
            return;
        }
        try {
            String data = String.join(" ", "CRT", userName, stringArrayToString(command));
            UDPSend(data);
            String response = castResponse(UDPReceive());
            if (response.equals("TRUE")) {
                System.out.println("Thread " + (String) command[0] + " created");
                return;
            } else if (response.equals("FALSE")) {
                System.out.println("Thread " + (String) command[0] + " exists");
                return;
            }
        } catch (SocketTimeoutException e) {
            // Timeout, resent packet
            System.out.println("Warning: Packet Timeout");
            createThread(command);
            return;
        } catch (Exception e) {
            System.out.println("ERROR");
        }
    }

    private static void postMessage(String[] command) {
        if (command == null || command[0].split(" ").length < 2) {
            System.out.println("Incorrect syntax for PST");
            return;
        }
        String threadName = command[0].split(" ")[0];
        try {
            String data = String.join(" ", "MSG", userName, stringArrayToString(command));
            UDPSend(data);
            String response = castResponse(UDPReceive());
            if (response.equals("FALSE")) {
                System.out.println("Thread title " + threadName + " does not exist");
                return;
            } else if (response.equals("TRUE")) {
                System.out.println("Message posted to " + threadName + " thread");
                return;
            }
        } catch (SocketTimeoutException e) {
            // Timeout, resent packet
            System.out.println("Warning: Packet Timeout");
            postMessage(command);
            return;
        } catch (Exception e) {
            System.out.println("ERROR");
        }
    }

    private static void deleteMessage(String[] command) {
        if (command == null || command[0].split(" ").length != 2) {
            System.out.println("Incorrect syntax for DLT");
            return;
        }
        try {
            // DEBUG
            // for (int i = 0; i < command.length; i++)
            // System.out.println(command[i]);
            String data = String.join(" ", "DLT", userName, stringArrayToString(command));
            UDPSend(data);
            String response = castResponse(UDPReceive());
            if (response.equals("NOTHREAD")) {
                System.out.println("Invalid thread title");
                return;
            } else if (response.equals("NOMESSAGEID")) {
                System.out.println("Invalid message ID");
                return;
            } else if (response.equals("NOUSER")) {
                System.out.println("The message belongs to another user and cannot be deleted");
                return;
            } else if (response.equals("TRUE")) {
                System.out.println("The message has been deleted");
                return;
            }
        } catch (SocketTimeoutException e) {
            // Timeout, resent packet
            System.out.println("Warning: Packet Timeout");
            deleteMessage(command);
            return;
        } catch (Exception e) {
            System.out.println("ERROR");
        }
    }

    private static void editMessage(String[] command) {
        if (command == null || command[0].split(" ").length < 3) {
            System.out.println("Incorrect syntax for EDT");
            return;
        }
        try {
            // DEBUG
            // for (int i = 0; i < command.length; i++)
            // System.out.println(command[i]);
            // System.out.println(stringArrayToString(command));
            String data = String.join(" ", "EDT", userName, stringArrayToString(command));
            UDPSend(data);
            String response = castResponse(UDPReceive());
            if (response.equals("NOTHREAD")) {
                System.out.println("Invalid thread title");
                return;
            } else if (response.equals("NOMESSAGEID")) {
                System.out.println("Invalid message ID");
                return;
            } else if (response.equals("NOUSER")) {
                System.out.println("The message belongs to another user and cannot be edited");
                return;
            } else if (response.equals("TRUE")) {
                System.out.println("The message has been edited");
                return;
            }
        } catch (SocketTimeoutException e) {
            // Timeout, resent packet
            System.out.println("Warning: Packet Timeout");
            editMessage(command);
            return;
        } catch (Exception e) {
            System.out.println("ERROR");
        }
    }

    private static void listThreads(String[] command) {
        if (command != null) {
            System.out.println("Incorrect syntax for LST");
            return;
        }
        // DEBUG
        // System.out.println("LST: " + command[0]);
        try {
            // DEBUG
            // for (int i = 0; i < command.length; i++)
            // System.out.println(command[i]);
            String data = String.join(" ", "LST", userName);
            UDPSend(data);
            String response = castResponse(UDPReceive());
            if (response.equals("FALSE")) {
                System.out.println("No threads to list");
                return;
            } else {
                System.out.println("The list of the active thread: ");
                String[] threadsActive = response.split(",");
                for (int i = 0; i < threadsActive.length; i++) {
                    System.out.println(threadsActive[i]);
                }
                return;
            }
        } catch (SocketTimeoutException e) {
            // Timeout, resent packet
            System.out.println("Warning: Packet Timeout");
            listThreads(command);
            return;
        } catch (Exception e) {
            System.out.println("ERROR");
        }
    }

    private static void readThread(String[] command) {
        if (command == null || command[0].split(" ").length != 1) {
            System.out.println("Incorrect syntax for RDT");
            return;
        }
        // DEBUG
        // System.out.println(command[0].length());
        String threadName = command[0];
        try {
            String data = String.join(" ", "RDT", userName, stringArrayToString(command));
            UDPSend(data);
            String response = castResponse(UDPReceive());
            if (response.equals("FALSE")) {
                System.out.println("Thread " + threadName + " does not exist");
                return;
            } else if (response.equals("EMPTY")) {
                System.out.println("Thread " + threadName + " is empty");
                return;
            } else {
                System.out.println(response);
                return;
            }
        } catch (SocketTimeoutException e) {
            // Timeout, resent packet
            System.out.println("Warning: Packet Timeout");
            readThread(command);
            return;
        } catch (Exception e) {
            System.out.println("ERROR");
        }
    }

    private static void uploadFile(String[] command) {
        if (command == null || command[0].split(" ").length != 2) {
            System.out.println("Incorrect syntax for UPD");
            return;
        }
        String[] ans = command[0].split(" ");
        String threadName = ans[0];
        String fileName = ans[1];
        // File name invalid
        File file = new File(fileName);
        if (!file.exists()) {
            System.out.println("No such file exists in current directory");
            return;
        }
        try {
            String data = String.join(" ", "UPD", userName, stringArrayToString(command));
            UDPSend(data);
            String response = castResponse(UDPReceive());
            if (response.equals("NOTHREAD")) {
                System.out.println("Thread title " + threadName + " does not exist");
                return;
            } else if (response.equals("FILEEXIST")) {
                System.out.println("File already exist");
                return;
            } else {
                // Transfer the file
                TCPSend(fileName);
            }
            response = castResponse(UDPReceive());
            if (response.equals("TRUE")) {
                System.out.println(fileName + " uploaded to " + threadName + " thread");
            } else {
                System.out.println("File uploading fail");
            }

        } catch (SocketTimeoutException e) {
            // Timeout, resent packet
            System.out.println("Warning: Packet Timeout");
            uploadFile(command);
            return;
        } catch (Exception e) {
            System.out.println("ERROR");
            e.printStackTrace();
        }
    }

    private static void downloadFile(String[] command) {
        if (command == null || command[0].split(" ").length != 2) {
            System.out.println("Incorrect syntax for DWN");
            return;
        }
        String[] ans = command[0].split(" ");
        String threadName = ans[0];
        String fileName = ans[1];
        try {
            String data = String.join(" ", "DWN", userName, stringArrayToString(command));
            UDPSend(data);
            String response = castResponse(UDPReceive());
            if (response.equals("NOTHREAD")) {
                System.out.println("Thread " + threadName + " does not exist");
                return;
            } else if (response.equals("NOFILE")) {
                System.out.println("File does not exist in Thread " + threadName);
                return;
            } else {
                // Receive the file
                TCPReceive(fileName);
            }
            System.out.println(fileName + " successfully downloaded");

        } catch (SocketTimeoutException e) {
            // Timeout, resent packet
            System.out.println("Warning: Packet Timeout");
            downloadFile(command);
            return;
        } catch (Exception e) {
            // DEBUG
            System.out.println("ERROR");
            e.printStackTrace();
        }
    }

    private static void removeThread(String[] command) {
        if (command == null || command[0].split(" ").length != 1) {
            System.out.println("Incorrect syntax for RMV");
            return;
        }
        String threadName = command[0];
        try {
            String data = String.join(" ", "RMV", userName, stringArrayToString(command));
            UDPSend(data);
            String response = castResponse(UDPReceive());
            if (response.equals("FALSE")) {
                System.out.println("Thread cannot be removed");
                return;
            } else if (response.equals("NOPERMISSION")) {
                System.out.println("The thread was created by another user and cannot be removed");
                return;
            } else if (response.equals("TRUE")) {
                System.out.println("The thread " + threadName + " has been removed");
                return;
            }
        } catch (SocketTimeoutException e) {
            // Timeout, resent packet
            System.out.println("Warning: Packet Timeout");
            removeThread(command);
            return;
        } catch (Exception e) {
            System.out.println("ERROR");
        }
    }

    private static int exit(String[] command) {
        if (command != null) {
            System.out.println("Incorrect syntax for XIT");
            return 1;
        }
        try {
            String data = String.join(" ", "XIT", userName);
            UDPSend(data);
            String response = castResponse(UDPReceive());
            if (response.equals("TRUE")) {
                System.out.println("Goodbye");
                return 0;
            }
        } catch (SocketTimeoutException e) {
            // Timeout, resent packet
            System.out.println("Warning: Packet Timeout");
            removeThread(command);
            return 0;
        } catch (Exception e) {
            System.out.println("ERROR");
        }
        return 1;
    }

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
        // System.out.println("MSG FROM SERVER:" + response);
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

    private static void TCPSend(String fileName) throws Exception {
        // prepare for sending
        Socket clientSocketTCP = new Socket(null);
        clientSocketTCP.setReuseAddress(true);
        SocketAddress address = new InetSocketAddress(IPAddress, serverPort);
        clientSocketTCP.bind(address);

        // Output to the socket data stream, we use DataOutputStream
        OutputStream outToServer = clientSocketTCP.getOutputStream();
        // Read from the binary file, we use FileInputStream
        InputStream inputStream = new FileInputStream(fileName);
        // Write the binary file to socket data stream and send it
        int byteRead = -1;
        while ((byteRead = inputStream.read()) != -1) {
            outToServer.write(byteRead);
        }
        // Close the file
        inputStream.close();
        // close the server
        clientSocketTCP.close();
    }

    private static void TCPReceive(String fileName) throws Exception {
        // Welcome socket
        ServerSocket welcomeSocketTCP = new ServerSocket();
        welcomeSocketTCP.setReuseAddress(true);
        SocketAddress address = new InetSocketAddress(IPAddress, serverPort);
        welcomeSocketTCP.bind(address);
        // DEBUG
        // System.out.println("TEST");
        // accept connection from connection queue
        Socket connectionSocket = welcomeSocketTCP.accept();
        // create read stream to get input
        InputStream inFromClient = connectionSocket.getInputStream();
        // create the stream to store the input
        OutputStream outputStream = new FileOutputStream(fileName);
        for (int byteRead = inFromClient.read(); byteRead != -1; byteRead = inFromClient.read()) {
            outputStream.write(byteRead);
        }
        outputStream.close();
        connectionSocket.close();
        welcomeSocketTCP.close();

        return;
    }

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
                if (newSpec[0] == "")
                    newSpec = null;
                result.put("command", name);
                result.put("value", newSpec);
                // DEBUG
                // System.out.println(newSpec.length);
                // System.out.println(newSpec[0].length());
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