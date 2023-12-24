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
