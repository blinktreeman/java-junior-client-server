package ru.letsdigit.server;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Server {
    private static Server server;
    private final int SERVER_PORT = 65123;
    protected static Long clientId = 0L;
    protected static List<ClientHandler> clientHandlerList = new ArrayList<>();

    public static Server getInstance() {
        if (server == null) {
            server = new Server();
        }
        return server;
    }

    public int getServerPort() {
        return SERVER_PORT;
    }

    public void startServer() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            while (true) {
                // В цикле запускаем новый поток для каждого клиента
                new ClientHandler(serverSocket.accept()).start();
            }
        }
    }
}

