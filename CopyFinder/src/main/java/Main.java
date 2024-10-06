import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Give multicast-address (IPv4 or IPv6) in parameters");
            return;
        }

        String multicastAddress = args[0];
        try {
            MulticastDiscovery discovery = new MulticastDiscovery(multicastAddress);
            discovery.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
