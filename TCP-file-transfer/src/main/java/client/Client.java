package client;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Client {
    private final String relativePath;
    private final String absolutePath;
    private final int port;
    private final String serverHost;
    private static final long MAX_SIZE = 1024L * 1024 * 1024 * 1024;

    public Client(String relativePath, String serverHost, int port) throws IOException {
        this.relativePath = relativePath;
        File file = new File(relativePath);
        this.absolutePath = file.getAbsolutePath();
        this.serverHost = serverHost;
        this.port = port;
        checkFile(file);
    }

    private void checkFile(File file) throws IOException {
        if (!file.exists()) {
            throw new IllegalArgumentException("File does not exist: " + relativePath);
        }
        if (file.getName().length() > 4096) {
            throw new IllegalArgumentException("Too long file name: " + file.getName());
        }
        if (Files.size(file.toPath()) > MAX_SIZE) {
            throw new IllegalArgumentException("File size exceeds 1 terabyte: " + relativePath);
        }
    }

    public void start() throws IOException {
        try (Socket socket = new Socket(serverHost, port);
             DataInputStream dis = new DataInputStream(socket.getInputStream());
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             FileInputStream fis = new FileInputStream(absolutePath)) {

            // Отправка размера файла
            long fileSize = Files.size(Paths.get(absolutePath));
            dos.writeLong(fileSize);

            // Отправка длины имени файла и имени файла
            byte[] fileNameBytes = new File(relativePath).getName().getBytes();
            dos.writeInt(fileNameBytes.length);
            dos.write(fileNameBytes);

            // Отправка содержимого файла
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                dos.write(buffer, 0, bytesRead);
            }

            // Получение результата от сервера
            boolean success = dis.readBoolean();
            if (success) {
                System.out.println("File was successfully sent and saved on server.");
            } else {
                System.out.println("Error: couldn't save file on server.");
            }
        }
    }
}