import java.util.*;
import java.io.*;
import java.net.*;

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

public class Server {
    static ArrayList threadInfo = new ArrayList<HashMap>();
    static ArrayList userInfo = new ArrayList<HashMap>();
    static ArrayList clientThread = new ArrayList<HashMap>();

    static DatagramSocket serverSocket;
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
        if (args.length != 1) {
            System.out.println("Required arguments: port");
            return;
        }
        serverPort = Integer.parseInt(args[0]);

        // Socket settle
        serverSocket = new DatagramSocket(serverPort);

        // Authentication
        readCredentials();
        while (true) {
            if (authentication()) {
                System.out.println("Welcome to the forum");
                break;
            }
        }

        // Interaction with user
        while (true) {
            String command = showMenu();
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
                user.put("username", userNP[0]);
                user.put("psw", userNP[1]);
                user.put("status", "offline");
                userInfo.add(user);
            }

        } catch (IOException e) {
            System.out.println("ERROR: credentials.txt does not exist.");
        }
    }

    private static boolean authentication() throws Exception {
        // Create a datagram packet to hold incomming UDP packet.
        DatagramPacket request = new DatagramPacket(new byte[1024], 1024);
        // Block until the host receives a UDP packet.
        serverSocket.receive(request);
        String response = UDPReceive(request);
        response = castResponse(response);
        String[] ans = response.split(" ");
        // Match the username
        if (userLoginInfoCorrect(ans[0], ans[1])) {
            UDPSend(request, "TRUE");
            System.out.println(ans[0] + " successful login.");
            return true;
        }
        UDPSend(request, "FALSE");
        return false;
    };

    private static boolean userLoginInfoCorrect(String name, String psw) {
        Map user = getUserByName(name);
        if (user != null && psw.equals(user.get("psw"))) {
            user.replace("status", "online");
            return true;
        }
        return false;

    };

    private static String showMenu() {
        return null;
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

    private static void UDPSend(DatagramPacket request, String sentence) throws Exception {
        InetAddress clientHost = request.getAddress();
        int clientPort = request.getPort();
        byte[] buf = sentence.getBytes();
        DatagramPacket reply = new DatagramPacket(buf, buf.length, clientHost, clientPort);
        serverSocket.send(reply);
    };

    private static String UDPReceive(DatagramPacket request) throws Exception {
        String line = null;
        // Obtain references to the packet's array of bytes.
        byte[] buf = request.getData();
        ByteArrayInputStream bais = new ByteArrayInputStream(buf);
        InputStreamReader isr = new InputStreamReader(bais);
        BufferedReader br = new BufferedReader(isr);
        line = br.readLine();
        return line;
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

    private static Map getUserByName(String name) {
        Iterator iter = userInfo.iterator();
        while (iter.hasNext()) {
            Map user = (HashMap) iter.next();
            if (user.get("username").equals(name))
                return user;
        }
        return null;
    }
}
