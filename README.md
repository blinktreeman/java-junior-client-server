# Java client-server application

## Клиент
```java
package ru.letsdigit.clients;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Client extends Thread implements AutoCloseable{
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;

    public void startConnection(String ip, int port) throws IOException {
        clientSocket = new Socket(ip, port);
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    }

    public void sendMessage(String msg) {
        out.println(msg);
    }

    @Override
    public void run() {
        while (true) {
            try {
                String readLine = in.readLine();
                if (readLine == null) break;
                System.out.println(readLine);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void close() throws Exception {
        in.close();
        out.close();
        clientSocket.close();
    }
}
```
## Запуск отдельных экземпляров клиента
```java
package ru.letsdigit;

import ru.letsdigit.clients.Client;
import ru.letsdigit.server.Server;

import java.io.IOException;
import java.util.Objects;
import java.util.Scanner;

public class ClientInit {
    public static void main(String[] args) throws IOException {
        Client client = new Client();
        try {
            client.startConnection("localhost", Server.getInstance().getServerPort());
            client.start();
        } catch (IOException e) {
            System.err.println("Unable to connect: " + e.getMessage());
        }

        Scanner consoleScanner = new Scanner(System.in);
        while (true) {
            String consoleInput = consoleScanner.nextLine();
            client.sendMessage(consoleInput);
            if (Objects.equals("q", consoleInput)) {
                break;
            }
        }
    }
}
```
## Сервер
`server.java:`
```java
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
    protected static Map<Long, Socket> clients = new HashMap<>();
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
```
`ClientHandler.java`
```java
package ru.letsdigit.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class ClientHandler extends Thread {
    private final Long id;
    private final Socket clientSocket;
    protected PrintWriter out;
    protected BufferedReader in;


    public ClientHandler(Socket socket) throws IOException {
        clientSocket = socket;
        id = ++Server.clientId;
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        Server.clientHandlerList.add(this);
    }

    @Override
    public void run() {
        String inputLine;
        while (true) {
            try {
                if ((inputLine = in.readLine()) != null) {
                    if (inputLine.equals("q")) {
                        Server.clientHandlerList.forEach(ch -> ch.out.println("Client " + id + " is gone"));
                        break;
                    }

                    List<ClientHandler> destinations = getDestinations(inputLine);
                    assert destinations != null;
                    if (destinations.isEmpty()) {
                        out.println("Wrong destination id");
                    } else {
                        String finalInputLine = inputLine;
                        destinations.forEach(d -> d.out.println("Client " + id + " said: " + finalInputLine));
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        try {
            in.close();
            out.close();
            clientSocket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Определяем адресатов сообщения
     * 1. Если в начале сообщения есть '@4' - то значит отсылаем сообщение клиенту с идентификатором 4.
     * 2. Если в начале сообщения нет '@' - значит, это сообщение нужно послать остальным клиентам.
     * @param message Переданное сообщение в форме @N, где N - номер клиента-адресата
     * @return Список адресатов
     */
    private List<ClientHandler> getDestinations(String message) {
        if (message.charAt(0) == '@')
                try {
                    Long id = Long.parseLong(message.substring(1, 2));
                    return Server.clientHandlerList
                            .stream()
                            .filter(ch -> Objects.equals(ch.id, id))
                            .collect(Collectors.toList());
                } catch (NumberFormatException e) {
                    return Server.clientHandlerList
                            .stream()
                            .filter(ch -> !Objects.equals(ch.id, id))
                            .collect(Collectors.toList());
                }
        return null;
    }
}
```
## Запуск сервера
```java
public class ServerApp {

    public static void main(String[] args) {
        try {
            Server.getInstance().startServer();
        } catch (IOException e) {
            System.err.println("Cannot run server" + e.getMessage());
        }
    }
}
```