package client;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java client.Main <file_path> <server_host> <port>");
            return;
        }
        String filePath = args[0];
        String serverHost = args[1];
        int port = Integer.parseInt(args[2]);

        try {
            Client client = new Client(filePath, serverHost, port);
            client.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}