import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class PingClient {

    public static void main(String[] args) throws Exception {
        List<Long> list = new ArrayList<Long>();
        if (args.length != 2) {
            System.out.println("Missing arguments");
            return;
        }
        int serverPort = Integer.parseInt(args[1]);
        // Get command line argument.
        if (args[0].compareTo("localhost") != 0) {
            System.out.println("Must use localhost.");
            return;
        }
        InetAddress IPAddress = InetAddress.getByName(args[0]);

        // create socket which connects to server
        DatagramSocket clientSocket = new DatagramSocket();
        int counter = 0;
        // Create a datagram packet to hold incomming UDP packet.
        DatagramPacket request = new DatagramPacket(new byte[1024], 1024);
        boolean timeout = false;
        long time = 0;
        int message = 3331;
        while (counter < 15) {
            // Prepare message
            try {
                long time1 = new Date().getTime();
                SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
                // Prepare packet
                String sentence = "PING " + message + " " + formatter.format(time1) + "\r\n";
                byte[] sendData = new byte[1024];
                sendData = sentence.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, serverPort);
                message++;
                // Actual send call
                clientSocket.send(sendPacket);
                // Calculate time
                // Set timeout
                clientSocket.setSoTimeout(600);

                // receive from server
                clientSocket.receive(request);
                long time2 = new Date().getTime();
                time = (time2 - time1);
            } catch (Exception e) {
                timeout = true;
            }

            // Print the recieved data.
            printData(request, counter, timeout, time);
            if (!timeout)
                list.add(time);
            timeout = false;
            counter++;
        }
        System.out.println("The maximum RTT: " + Collections.max(list) + " ms");
        System.out.println("The minimum RTT: " + Collections.min(list) + " ms");
        long average = 0;
        Iterator<Long> itr = list.iterator();
        while (itr.hasNext())
            average += itr.next();
        average /= list.size();
        System.out.println("The average RTT: " + average + " ms");
        // close the scoket
        clientSocket.close();

    } // end of main

    private static void printData(DatagramPacket request, int counter, boolean timeout, long time)
            throws Exception {
        counter++;
        if (timeout) {
            System.out.println(
                    "pint to 127.0.0.1, seq = " + counter + ", rtt = time out");
        } else {
            // Print host address and data received from it.
            System.out.println(
                    "ping to 127.0.0.1, seq = " + counter + ", rtt = " +
                            time + " ms");
        }
    }
} // end of class TCPClient
