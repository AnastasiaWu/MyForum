import java.util.*;
import java.io.*;
import java.net.*;
import java.nio.*;
// userInfo{
//  {
//      userName: hans,
//      psw: falcon*solo,
//      status: online,
//   },{
//      userName: yoda,
//      psw: wise@!man,
//      status: offline,
//   },
// }

// threadInfo {
//      threadName: {
//          owner:,
//          counter:,    
//      },
//  }
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
    private static final int AVERAGE_DELAY = 1000; // milliseconds

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

        // Authentication
        readCredentials();
        System.out.println("Waiting for clients.");
        while (true) {
            if (authentication()) {
                break;
            }
        }

        // Interaction with user
        while (true) {
            Map response = UDPReceive();
            if (response == null)
                continue;
            DatagramPacket request = (DatagramPacket) response.get("request");
            String content = castResponse((String) response.get("content"));
            Map commands = commandParse(content);
            String command = (String) commands.get("command");
            String[] spec = (String[]) commands.get("value");
            switch (command) {
                case "CRT":
                    createThread(spec, request);
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
                    if (code == 0)
                        return;
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

    private static boolean authentication() throws Exception {
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
            System.out.println("Thread " + (String) fileName + " exists");
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
        info.put("owner", userName);
        info.put("counter", 0);
        threadInfo.put(fileName, info);
        UDPSend(request, "TRUE");
        System.out.println("Thread " + (String) fileName + " created.");
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

    private static void UDPSend(DatagramPacket request, String sentence) throws Exception {
        // DEBUG: Simulate network delay.
        Random random = new Random();
        Thread.sleep((int) (random.nextDouble() * 2 * AVERAGE_DELAY));

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

    private static void TCPSend() throws Exception {
    };

    private static void TCPReceive() throws Exception {
    };

    // HELPER FUNCTION
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
}
