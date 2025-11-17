package ca.concordia.server;

import ca.concordia.filesystem.FileSystemManager;

import java.net.ServerSocket;
import java.net.Socket;

public class FileServer {

    private final FileSystemManager fsManager;
    private final int port;

    public FileServer(int port, String fileSystemName, int totalSize) {
        this.port = port;
        this.fsManager = new FileSystemManager(fileSystemName, totalSize);
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started. Listening on port " + port + "...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Accepted client: " + clientSocket);

                ClientHandler handler = new ClientHandler(clientSocket, fsManager);
                handler.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Could not start server on port " + port);
        }
    }
}
