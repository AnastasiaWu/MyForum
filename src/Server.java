import java.util.*;
import java.util.Map.Entry;
import java.io.*;
import java.net.*;
import java.nio.*;
// userInfo{
//  userName: {
//      psw: falcon*solo,
//      status: online,
//   },
// }

// threadInfo {
//      threadName: {
//          owner:,
//          counter:,    
//          files:[],
//      },
// }

// "ACK Seq Length message"

public class Server {
    static Map threadInfo = new HashMap<>();
    static Map userInfo = new HashMap<>();
    static ArrayList clientThread = new ArrayList<HashMap>();

    static DatagramSocket serverSocket;

    static InetAddress IPAddress;
    static int serverPort;

    static int ACK = 0;
    static int Seq = 0;

    static String buffer;

    // DEBUG
    // private static final int AVERAGE_DELAY = 1000; // milliseconds
    // private static final double LOSS_RATE = 0.8;

    public static void main(String[] args) throws Exception {
        // Check the input
        if (args.length != 1) {
            System.out.println("Please input the port number.");
            return;
        }

        // Define socket parameters, address and Port#
        IPAddress = InetAddress.getByName("localhost");
        if (args.length != 1) {
            System.out.println("Required arguments: port");
            return;
        }
        serverPort = Integer.parseInt(args[0]);

        // Socket settle
        serverSocket = new DatagramSocket(serverPort);
        // Read user info
        readCredentials();

        // Authentication
        authentication();

        // Interaction with user
        while (true) {
            Map response = UDPReceive();
            if (response == null)
                continue;
            DatagramPacket request = (DatagramPacket) response.get("request");
            String content = castResponse((String) response.get("content"));
            Map commands = commandParse(content);
            // command is command sign (CRT, MSG...)
            String command = (String) commands.get("command");
            // value is the thing after command sign(CRT, MSG...)
            String[] spec = (String[]) commands.get("value");

            switch (command) {
                case "CRT":
                    createThread(spec, request);
                    break;
                case "MSG":
                    postMessage(spec, request);
                    break;
                case "DLT":
                    deleteMessage(spec, request);
                    break;
                case "EDT":
                    editMessage(spec, request);
                    break;
                case "LST":
                    listThreads(spec, request);
                    break;
                case "RDT":
                    readThread(spec, request);
                    break;
                case "UPD":
                    uploadFile(spec, request);
                    break;
                case "DWN":
                    downloadFile();
                    break;
                case "RMV":
                    removeThread(spec, request);
                    break;
                case "XIT":
                    exit(spec, request);
                    authentication();
                    break;
                default:
                    // show error message
                    System.out.println("Invalid command.");
                    break;
            }
        }
    };

    private static void readCredentials() throws Exception {
        try {
            BufferedReader buff = new BufferedReader(new FileReader("credentials.txt"));
            String info;
            while ((info = buff.readLine()) != null) {
                String[] userNP = info.split(" ");
                Map user = new HashMap<>();
                user.put("psw", userNP[1]);
                user.put("status", "offline");
                userInfo.put(userNP[0], user);
            }

        } catch (IOException e) {
            System.out.println("ERROR: credentials.txt does not exist.");
        }
    }

    private static void authentication() throws Exception {
        System.out.println("Waiting for clients.");
        while (true) {
            if (authentication_validation()) {
                break;
            }
        }
    }

    private static boolean authentication_validation() throws Exception {

        Map response = UDPReceive();
        if (response == null)
            return false;
        DatagramPacket request = (DatagramPacket) response.get("request");
        String content = castResponse((String) response.get("content"));
        String[] ans = content.split(" ");
        // Match the username
        // DEBUG
        // System.out.println(ans[0]);
        if (ans[0].equals("name")) {
            System.out.println("Client authenticating.");
            // name authentication
            if (userInfo.containsKey(ans[1])) {
                Map user = (Map) userInfo.get(ans[1]);
                // DEBUG
                // System.out.println(ans[1]);
                if (((String) user.get("status")).equals("online")) {
                    UDPSend(request, "ONLINE");
                    return false;
                } else {
                    UDPSend(request, "TRUE");
                    return false;
                }
            } else {
                UDPSend(request, "FALSE");
                return false;
            }
        } else if (ans[0].equals("psw")) {
            // password authentication
            Map user = (Map) userInfo.get(ans[1]);
            if (((String) user.get("psw")).equals(ans[2])) {
                user.put("status", "online");
                UDPSend(request, "TRUE");
                System.out.println(ans[1] + " successful login.");
                return true;
            }
        }
        UDPSend(request, "FALSE");
        return false;
    }

    private static void createThread(String[] command, DatagramPacket request) throws Exception {
        String[] spec = command[0].split(" ");
        String fileName = spec[0];
        String userName = spec[1];
        System.out.println(userName + " issued CRT command");
        // Check if a thread with this title exists - yesError to user
        File file = new File((String) fileName);
        if (file.exists()) {
            UDPSend(request, "FALSE");
            System.out.println("Thread " + (String) fileName + " exists.");
            return;
        }
        // create thread
        // DEBUG
        // System.out.println(fileName);
        // System.out.println(userName);
        file.createNewFile();
        file.setExecutable(true);
        file.setWritable(true);
        // write to file
        FileWriter myWriter = new FileWriter(fileName);
        myWriter.write((String) userName + "\n");
        myWriter.close();
        // set the database
        Map info = new HashMap<>();
        Map files = new HashMap<>();
        info.put("owner", userName);
        info.put("counter", 0);
        info.put("files", files);
        threadInfo.put(fileName, info);
        UDPSend(request, "TRUE");
        System.out.println("Thread " + (String) fileName + " created.");
    }

    private static void postMessage(String[] command, DatagramPacket request) throws Exception {
        String[] spec = command[0].split(" ");
        String threadName = spec[1];
        String userName = spec[0];
        String message = stringArrayToString(spec, 2, spec.length - 1);
        // DEBUG
        // System.out.println(threadName + " " + userName + " " + message);

        System.out.println(userName + " issued MSG command");
        // Thread name does not exist
        if (!threadInfo.containsKey(threadName)) {
            UDPSend(request, "FALSE");
            System.out.println("Thread " + (String) threadName + " does not exist.");
            return;
        }
        Map thread = (Map) threadInfo.get(threadName);
        int counter = (int) thread.get("counter");
        counter += 1;
        FileWriter myWriter = new FileWriter(threadName, true);
        myWriter.write(Integer.toString(counter) + " "
                + (String) userName + ": " + message + "\n");
        myWriter.close();
        thread.replace("counter", counter);
        // DEBUG
        // System.out.println((int) thread.get("counter"));
        UDPSend(request, "TRUE");
        System.out.println("Message posted to " + threadName + " thread");
    }

    private static void deleteMessage(String[] command, DatagramPacket request) throws Exception {
        String[] spec = command[0].split(" ");
        String userName = spec[0];
        String threadName = spec[1];
        String messageID = spec[2];
        // DEBUG
        System.out.println(threadName + " " + userName + " " + messageID);

        System.out.println(userName + " issued DLT command");

        // Thread name does not exist
        if (!threadInfo.containsKey(threadName)) {
            UDPSend(request, "NOTHREAD");
            System.out.println("Thread " + (String) threadName + " does not exist.");
            return;
        }

        File inputFile = new File(threadName);
        File tempFile = new File(threadName + "Temp");
        BufferedReader reader = new BufferedReader(new FileReader(inputFile));
        BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));
        String currentLine;
        boolean exist = false;
        boolean right = false;
        while ((currentLine = reader.readLine()) != null) {
            // Invalid message ID
            // DEBUG
            // System.out.println(currentLine);
            if (currentLine.startsWith(messageID)) {
                exist = true;
                // Check user right
                if (currentLine.startsWith(messageID + " " + userName)) {
                    right = true;
                    continue;
                }
            }
            // trim newline
            currentLine.trim();
            writer.write(currentLine + System.getProperty("line.separator"));
        }
        writer.close();
        reader.close();
        inputFile.delete();
        boolean rename = tempFile.renameTo(inputFile);
        tempFile.delete();
        if (!exist) {
            // No message ID
            UDPSend(request, "NOMESSAGEID");
            System.out.println("Message does not exist.");
            return;
        } else if (!right) {
            // No right
            UDPSend(request, "NOUSER");
            System.out.println("Message cannot be deleted.");
            return;
        } else if (rename) {
            UDPSend(request, "TRUE");
            System.out.println("Message has been deleted.");
        } else {
            System.out.println("File rename failed.");
        }
    }

    private static void editMessage(String[] command, DatagramPacket request) throws Exception {
        String[] spec = command[0].split(" ");
        String userName = spec[0];
        String threadName = spec[1];
        String messageID = spec[2];
        String message = spec[3];
        // DEBUG
        // System.out.println(threadName + " " + userName + " " + messageID + " " +
        // message);

        System.out.println(userName + " issued EDT command");

        // Thread name does not exist
        if (!threadInfo.containsKey(threadName)) {
            UDPSend(request, "NOTHREAD");
            System.out.println("Thread " + (String) threadName + " does not exist.");
            return;
        }

        File inputFile = new File(threadName);
        File tempFile = new File(threadName + "Temp");
        BufferedReader reader = new BufferedReader(new FileReader(inputFile));
        BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));
        String currentLine;
        boolean exist = false;
        boolean right = false;
        while ((currentLine = reader.readLine()) != null) {
            // Invalid message ID
            if (currentLine.startsWith(messageID)) {
                exist = true;
                // Check user right
                if (currentLine.startsWith(messageID + " " + userName)) {
                    right = true;
                    writer.write(messageID + " " + userName + ": " + message + System.getProperty("line.separator"));
                    continue;
                }
            }
            // trim newline
            currentLine.trim();
            writer.write(currentLine + System.getProperty("line.separator"));
        }
        writer.close();
        reader.close();
        inputFile.delete();
        boolean rename = tempFile.renameTo(inputFile);
        tempFile.delete();
        if (!exist) {
            // No message ID
            UDPSend(request, "NOMESSAGEID");
            System.out.println("Message does not exist.");
            return;
        } else if (!right) {
            // No right
            UDPSend(request, "NOUSER");
            System.out.println("Message cannot be deleted.");
            return;
        } else if (rename) {
            UDPSend(request, "TRUE");
            System.out.println("Message has been deleted.");
        } else {
            System.out.println("File rename failed.");
        }
    };

    private static void listThreads(String[] command, DatagramPacket request) throws Exception {
        String userName = command[0];
        System.out.println(userName + " issued LST command");
        if (threadInfo.size() == 0) {
            UDPSend(request, "FALSE");
            return;
        }
        StringBuilder sb = new StringBuilder();
        threadInfo.forEach((key, tab) -> sb.append(key + ","));
        sb.deleteCharAt(sb.length() - 1);
        // DEBUG
        // System.out.println(sb.toString());
        UDPSend(request, sb.toString());
        return;

    }

    private static void readThread(String[] command, DatagramPacket request) throws Exception {
        String[] spec = command[0].split(" ");
        String userName = spec[0];
        String threadName = spec[1];
        // DEBUG
        // System.out.println(threadName + " " + userName);

        System.out.println(userName + " issued RDT command");

        // Thread name does not exist
        if (!threadInfo.containsKey(threadName)) {
            UDPSend(request, "FALSE");
            System.out.println("Thread " + (String) threadName + " does not exist.");
            return;
        }
        Map thread = (Map) threadInfo.get(threadName);
        // Thread empty
        if ((int) thread.get("counter") == 0) {
            UDPSend(request, "EMPTY");
            return;
        }
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new FileReader(threadName));
        String currentLine;
        // get rid of the first line
        currentLine = reader.readLine();

        while ((currentLine = reader.readLine()) != null) {
            sb.append(currentLine + "\n");
        }
        sb.deleteCharAt(sb.length() - 1);
        reader.close();

        UDPSend(request, sb.toString());
        // DEBUG
        // System.out.println(sb.toString());
        // TODO: uploadFile();
        return;
    }

    private static void uploadFile(String[] command, DatagramPacket request) throws Exception {
        String[] spec = command[0].split(" ");
        String userName = spec[0];
        String threadName = spec[1];
        String fileName = spec[2];
        // DEBUG
        // System.out.println(threadName + " " + userName);

        System.out.println(userName + " issued UPD command");

        // Thread name does not exist
        if (!threadInfo.containsKey(threadName)) {
            UDPSend(request, "NOTHREAD");
            System.out.println("Thread " + (String) threadName + " does not exist.");
            return;
        }
        Map thread = (Map) threadInfo.get(threadName);
        Map files = (Map) thread.get("files");
        // File already exist
        if (files.containsKey(fileName)) {
            UDPSend(request, "FILEEXIST");
            return;
        }
        UDPSend(request, "TRUE");
        // Prepare the new file name
        String newFileName = threadName.concat("-" + fileName);
        // Receive the new file
        TCPReceive(newFileName);// DEBUG
        System.out.println("File created.");
        // Check if uploaded
        // File file = new File((String) newFileName);
        // if (file.exists()) {
        // // Update the database
        // thread.put("files", files.put(fileName, userName));
        // // Update in the thread
        // FileWriter myWriter = new FileWriter(fileName);
        // myWriter.write(userName + " uploaded " + fileName);
        // }
        return;
    }

    private static void downloadFile() {
    };

    private static void removeThread(String[] command, DatagramPacket request) throws Exception {
        String[] spec = command[0].split(" ");
        String userName = spec[0];
        String threadName = spec[1];
        // DEBUG
        System.out.println(threadName + " " + userName);

        System.out.println(userName + " issued RMV command");

        // Thread name does not exist
        if (!threadInfo.containsKey(threadName)) {
            UDPSend(request, "FALSE");
            System.out.println("Thread " + (String) threadName + " does not exist.");
            return;
        }
        // User access right fail
        if (!((String) ((Map) threadInfo.get(threadName)).get("owner")).equals(userName)) {
            UDPSend(request, "NOPERMISSION");
            System.out.println("User has no permission to remove the thread.");
            return;
        }
        File file = new File(threadName);
        file.delete();
        // TODO: remove the upload file
        System.out.println("Thread " + threadName + " has been removed.");
        return;
    }

    private static void exit(String[] command, DatagramPacket request) throws Exception {
        String userName = command[0];
        Map user = (Map) userInfo.get(userName);
        user.replace("status", "offline");
        UDPSend(request, "TRUE");
        System.out.println(userName + " exited.");
        return;
    }

    private static void UDPSend(DatagramPacket request, String sentence) throws Exception {
        // DEBUG: Simulate network delay.
        // Random random = new Random();
        // Thread.sleep((int) (random.nextDouble() * 2 * AVERAGE_DELAY));

        buffer = sentence;
        InetAddress clientHost = request.getAddress();
        int clientPort = request.getPort();
        sentence = String.join(" ", Integer.toString(ACK), Integer.toString(Seq),
                Integer.toString(sentence.length()), sentence);
        byte[] buf = sentence.getBytes();
        DatagramPacket reply = new DatagramPacket(buf, buf.length, clientHost, clientPort);
        serverSocket.send(reply);
    }

    // For resent
    private static void UDPSend(DatagramPacket request) throws Exception {
        InetAddress clientHost = request.getAddress();
        int clientPort = request.getPort();
        String sentence = String.join(" ", Integer.toString(ACK), Integer.toString(Seq),
                Integer.toString(buffer.length()), buffer);
        byte[] buf = sentence.getBytes();
        DatagramPacket reply = new DatagramPacket(buf, buf.length, clientHost, clientPort);
        serverSocket.send(reply);
    };

    private static Map UDPReceive() throws Exception {
        // Create a datagram packet to hold incomming UDP packet.
        Map response = new HashMap();
        DatagramPacket request = new DatagramPacket(new byte[1024], 1024);
        // Block until the host receives a UDP packet.
        serverSocket.receive(request);

        // DEBUG: Decide whether to reply, or simulate packet loss.
        // Random random = new Random();
        // if (random.nextDouble() < LOSS_RATE) {
        // System.out.println(" Reply not sent.");
        // return null;
        // }

        String line = null;
        // Obtain references to the packet's array of bytes.
        byte[] buf = request.getData();
        ByteArrayInputStream bais = new ByteArrayInputStream(buf);
        InputStreamReader isr = new InputStreamReader(bais);
        BufferedReader br = new BufferedReader(isr);
        line = br.readLine();

        // parse the packet
        Map messages = UDPMessageParse(line);
        int ackRecieve = Integer.parseInt((String) messages.get("ACK"));
        int seqReceive = Integer.parseInt((String) messages.get("Seq"));
        int lengthReceive = Integer.parseInt((String) messages.get("Length"));
        line = (String) messages.get("message");
        // DEBUG
        // System.out.println(line);
        // check if the packet is duplicate
        if (ACK == seqReceive + lengthReceive) {
            // This is a duplicate packet, drop it, resent packet
            System.out.println("Warning: DUP packet.");
            UDPSend(request);
            return null;
        } else {
            // Normal packet
            Seq = ackRecieve;
            ACK = seqReceive + lengthReceive;
        }

        response.put("request", request);
        response.put("content", line);
        return response;
    };

    // private static void TCPSend() throws Exception {
    // }

    private static void TCPReceive(String fileName) throws Exception {
        // Welcome socket
        ServerSocket welcomeSocketTCP = new ServerSocket(serverPort);
        // accept connection from connection queue
        Socket connectionSocket = welcomeSocketTCP.accept();
        // create read stream to get input
        InputStream inFromClient = connectionSocket.getInputStream();
        // create the stream to store the input
        OutputStream outputStream = new FileOutputStream(fileName);
        outputStream.write(inFromClient.read());
        outputStream.close();
        connectionSocket.close();
        return;
    }

    // HELPER FUNCTION
    // get rid of '\0' at the end of the function
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

    // split the command sign and the rest => ['CRT', '3331 Network is awesome']
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
        StringBuilder sb = new StringBuilder();
        for (int i = 3; i < ans.length; i++) {
            sb.append(ans[i]);
            if (i != ans.length - 1)
                sb.append(" ");
        }
        result.put("message", sb.toString());
        // DEBUG
        // for (int i = 0; i < message.size(); i++) {
        // System.out.println(message.get(i));
        // }
        // System.out.println(sb.toString());
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

    private static String stringArrayToString(String[] input, int start, int end) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i <= end; i++) {
            sb.append(input[i]);
            if (i != end)
                sb.append(" ");
        }
        return sb.toString();
    }

}
