import java.io.IOException;
import java.net.*;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

public class MulticastDiscovery {
    private static final int PORT = 25565;
    private static final long PID = ProcessHandle.current().pid();
    private static final int BUFFER_SIZE = 256;
    private static final String MESSAGE = "DISCOVERY_REQUEST";
    private final Map<Long, Map.Entry<InetAddress, Long>> activeCopies = new HashMap<>();
    private final InetAddress groupAddress;

    public MulticastDiscovery(String multicastAddress) throws UnknownHostException {
        this.groupAddress = InetAddress.getByName(multicastAddress);
    }

    public void start() throws IOException {
        try (MulticastSocket socket = new MulticastSocket(PORT)) {
            socket.setReuseAddress(true);
            socket.joinGroup(groupAddress);
            new Thread(() -> receiveDiscoveryRequests(socket)).start();
            new Thread(() -> sendDiscoveryRequests(socket)).start();
            removeInactiveCopies();
        }
    }

    private void receiveDiscoveryRequests(MulticastSocket socket) {
        byte[] buffer = new byte[BUFFER_SIZE];
        while (true) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String receivedMessage = new String(packet.getData(), 0, packet.getLength());
                String[] parts = receivedMessage.split(" ");
                if (parts.length == 2 && MESSAGE.equals(parts[0])) {
                    InetAddress senderAddress = packet.getAddress();
                    long senderPid = Long.parseLong(parts[1]);
                    synchronized (activeCopies) {
                        if (activeCopies.putIfAbsent(senderPid, new AbstractMap.SimpleEntry<>(senderAddress, System.currentTimeMillis())) == null) {
                            System.out.println("New copy detected: " + senderAddress + " " + senderPid);
                            printActiveCopies();
                        }
                        else {
                            activeCopies.get(senderPid).setValue(System.currentTimeMillis());
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    private void sendDiscoveryRequests(MulticastSocket socket) {
        while (true) {
            String message = MESSAGE + " " + PID;
            byte[] messageBytes = message.getBytes();
            DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length, groupAddress, PORT);
            try {
                socket.send(packet);
                Thread.sleep(3000); // Отправляем запрос раз в 3 секунды
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    private void removeInactiveCopies() {
        while (true) {
            try {
                long curTime = System.currentTimeMillis();
                synchronized (activeCopies) {
                    if (activeCopies.entrySet().removeIf(entry -> curTime - entry.getValue().getValue() > 5000)) {
                        System.out.println("It looks like some copies have been terminated...");
                        printActiveCopies();
                    }
                }
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    private void printActiveCopies() {
        synchronized (activeCopies) {
            System.out.println("Active copies:");
            for (var entry: activeCopies.entrySet()) {
                System.out.println(" - " + entry.getValue().getKey() + " " + entry.getKey());
            }
        }
    }
}
