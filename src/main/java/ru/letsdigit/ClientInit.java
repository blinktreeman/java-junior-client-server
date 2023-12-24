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
