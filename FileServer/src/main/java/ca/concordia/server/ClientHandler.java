package ca.concordia.server;

import ca.concordia.filesystem.FileSystemManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * One thread per client.
 * Handles textual commands and delegates to FileSystemManager.
 */
public class ClientHandler extends Thread {

    private final Socket clientSocket;
    private final FileSystemManager fsManager;

    public ClientHandler(Socket clientSocket, FileSystemManager fsManager) {
        this.clientSocket = clientSocket;
        this.fsManager = fsManager;
    }

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(
                     new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("From " + clientSocket + ": " + line);
                String response = handleCommand(line);
                writer.println(response);
            }

        } catch (Exception e) {
            System.err.println("Client handler error: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (Exception ignored) {}
        }
    }

    private String handleCommand(String line) {
        try {
            if (line.trim().isEmpty()) {
                return "ERROR: empty command";
            }

            String[] parts = line.split(" ", 3);
            String command = parts[0].toUpperCase();

            switch (command) {
                case "CREATE":
                    if (parts.length < 2) return "ERROR: missing filename";
                    fsManager.createFile(parts[1]);
                    return "SUCCESS: File '" + parts[1] + "' created.";

                case "DELETE":
                    if (parts.length < 2) return "ERROR: missing filename";
                    fsManager.deleteFile(parts[1]);
                    return "SUCCESS: File '" + parts[1] + "' deleted.";

                case "WRITE":
                    if (parts.length < 3) return "ERROR: missing filename or content";
                    fsManager.writeFile(parts[1], parts[2].getBytes());
                    return "SUCCESS: Data written to '" + parts[1] + "'.";

                case "READ":
                    if (parts.length < 2) return "ERROR: missing filename";
                    byte[] data = fsManager.readFile(parts[1]);
                    return new String(data);

                case "LIST":
                    String[] files = fsManager.listFiles();
                    if (files.length == 0) return "No files.";
                    return String.join(",", files);

                case "QUIT":
                    return "SUCCESS: disconnecting.";

                default:
                    return "ERROR: Unknown command.";
            }

        } catch (IllegalArgumentException e) {
            return "ERROR: " + e.getMessage();
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR: internal server error";
        }
    }
}

