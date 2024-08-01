import java.net.*;
import java.io.*;
import java.util.*;

public class GroupChat {
    private static final String TERMINATE = "Exit";
    static String userName;
    static volatile boolean isRunning = true;

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: <multicast-host> <port-number>");
        } else {
            try {
                InetAddress multicastGroup = InetAddress.getByName(args[0]);
                int portNumber = Integer.parseInt(args[1]);
                Scanner scanner = new Scanner(System.in);

                System.out.print("Enter your name: ");
                userName = scanner.nextLine();

                MulticastSocket multicastSocket = new MulticastSocket(portNumber);
                multicastSocket.setTimeToLive(0);  // For local network usage
                multicastSocket.joinGroup(multicastGroup);

                Thread readThread = new Thread(new MessageReceiver(multicastSocket, multicastGroup, portNumber));
                readThread.start();

                System.out.println("Start typing messages...\n");
                while (isRunning) {
                    String userMessage = scanner.nextLine();
                    if (userMessage.equalsIgnoreCase(GroupChat.TERMINATE)) {
                        isRunning = false;
                        multicastSocket.leaveGroup(multicastGroup);
                        multicastSocket.close();
                        break;
                    }
                    userMessage = userName + ": " + userMessage;
                    byte[] messageBuffer = userMessage.getBytes();
                    DatagramPacket datagramPacket = new DatagramPacket(messageBuffer, messageBuffer.length, multicastGroup, portNumber);
                    multicastSocket.send(datagramPacket);
                }
            } catch (SocketException e) {
                System.err.println("Error: Unable to create socket.");
                e.printStackTrace();
            } catch (IOException e) {
                System.err.println("Error: I/O operation failed.");
                e.printStackTrace();
            }
        }
    }
}

class MessageReceiver implements Runnable {
    private MulticastSocket multicastSocket;
    private InetAddress multicastGroup;
    private int portNumber;
    private static final int BUFFER_SIZE = 1000;

    MessageReceiver(MulticastSocket multicastSocket, InetAddress multicastGroup, int portNumber) {
        this.multicastSocket = multicastSocket;
        this.multicastGroup = multicastGroup;
        this.portNumber = portNumber;
    }

    @Override
    public void run() {
        while (GroupChat.isRunning) {
            byte[] buffer = new byte[BUFFER_SIZE];
            DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length, multicastGroup, portNumber);
            try {
                multicastSocket.receive(datagramPacket);
                String receivedMessage = new String(buffer, 0, datagramPacket.getLength(), "UTF-8");
                if (!receivedMessage.startsWith(GroupChat.userName)) {
                    System.out.println(receivedMessage);
                }
            } catch (IOException e) {
                System.out.println("Multicast socket closed.");
            }
        }
    }
}
