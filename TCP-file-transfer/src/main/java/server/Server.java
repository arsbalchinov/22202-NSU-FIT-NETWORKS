package server;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private final int port;

    public Server(int port) {
        this.port = port;
        File uploadDir = new File("src/main/java/server/uploads");
        if (!uploadDir.exists()) {
            if (uploadDir.mkdirs()) {
                System.out.println("Created uploads directory: " + uploadDir.getAbsolutePath());
            }
        }
    }

    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on port " + port);

            ExecutorService executor = Executors.newCachedThreadPool();
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Client connected");
                executor.execute(new ClientHandler(socket));
            }
        } catch (IOException e) {
            System.out.println("Error in Server.start: " + e.getMessage());
        }
    }

    private class ClientHandler implements Runnable {
        private final Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                DataInputStream dis = new DataInputStream(socket.getInputStream());
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                long fileSize = dis.readLong();     //Получаем размер файла
                System.out.println("Server got size of file");

                int fileNameLength = dis.readInt(); //Получаем длину имени файла
                System.out.println("Server got length of filename");
                byte[] fileNameBytes = new byte[fileNameLength];
                dis.readFully(fileNameBytes);       //Получаем имя файла
                System.out.println("Server got file name");
                String fileName = new String(fileNameBytes);

                File uploadDir = new File("src/main/java/server/uploads");
                File file = createFile("src/main/java/server/uploads/", fileName);

                Path uploadPath = uploadDir.toPath().toAbsolutePath();
                if (!file.toPath().toAbsolutePath().startsWith(uploadPath)) {
                    System.out.println("Incorrect file path: " + fileName);
                    System.out.println(uploadDir.toPath().toAbsolutePath());
                    System.out.println(file.toPath().toAbsolutePath());
                    dos.writeBoolean(false);
                    return;
                }

                try (FileOutputStream fos = new FileOutputStream(file.getAbsolutePath())) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    long lastBytesRead = 0;
                    long totalBytesRead = 0;
                    long elapsedTime, totalElapsedTime;
                    double instantSpeed, averageSpeed;

                    long startTime = System.currentTimeMillis();
                    long lastReportTime = startTime;

                    while (totalBytesRead < fileSize && (bytesRead = dis.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                        long currentTime = System.currentTimeMillis();
                        totalBytesRead += bytesRead;

                        elapsedTime = currentTime - lastReportTime;
                        if (elapsedTime >= 3000) {
                            totalElapsedTime = currentTime - startTime;
                            instantSpeed = (totalBytesRead - lastBytesRead) / (elapsedTime / 1000.0);
                            averageSpeed = totalBytesRead / (totalElapsedTime / 1000.0);

                            System.out.println("Instant speed: " + instantSpeed + " byte/s");
                            System.out.println("Average speed: " + averageSpeed + " byte/s");

                            lastReportTime = currentTime;
                            lastBytesRead = totalBytesRead;
                        }
                    }

                    totalElapsedTime = System.currentTimeMillis() - startTime;
                    if (totalElapsedTime < 3000) {
                        averageSpeed = totalBytesRead / (totalElapsedTime / 1000.0);
                        System.out.println("Average speed: " + averageSpeed + " byte/s");
                    }
                }

                if (file.length() == fileSize) {
                    dos.writeBoolean(true);
                    System.out.println("File " + fileName + " successfully received and saved.");
                }
                else {
                    dos.writeBoolean(false);
                    System.out.println("Error getting file " + fileName + ". Size mismatch.");
                }
            } catch (IOException e) {
                System.out.println("Error handling client: " + e.getMessage());
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    System.out.println("Error on socket close: " + e.getMessage());
                }
            }
        }
    }
    private File createFile(String dirPath, String fileName){
        int n = 1;
        String name = dirPath + fileName;
        File file = new File(name);
        if (file.exists()){
            boolean res = false;
            while (!res) {
                res = file.renameTo(new File(dirPath + fileName + "(" + n + ")"));
                n++;
            }
        }
        return file;
    }
}