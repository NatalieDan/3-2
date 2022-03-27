package geekbrains.Server;

import geekbrains.CommonConstants;
import geekbrains.Server.Authorization.AuthService;
import geekbrains.Server.Authorization.InMemoryAuthServiceImpl;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Server {
    private final AuthService authService;

    private List<ClientHandler> connectedUsers;

    public Server() {
        authService = new InMemoryAuthServiceImpl();
        try (ServerSocket server = new ServerSocket(CommonConstants.SERVER_PORT)) {
            authService.start();
            connectedUsers = new ArrayList<>();
            while (true) {
                System.out.println("Сервер ожидает подключения");
                Socket socket = server.accept();
                System.out.println("Клиент подключился");
                new ClientHandler(this, socket);
            }
        } catch (IOException exception) {
            System.out.println("Ошибка в работе сервера");
            exception.printStackTrace();
        } finally {
            if (authService != null) {
                authService.end();
            }
        }
    }

    public AuthService getAuthService() {
        return authService;
    }

    public boolean isNickNameBusy(String nickName) {
        for(ClientHandler handler: connectedUsers) {
            if (handler.getNickName().equals(nickName)) {
                return true;
            }
        }

        return false;
    }

    public synchronized void broadcastMessage(String message) {
        String[] personalMessage = message.split(" ");
        StringBuilder textInMessage = new StringBuilder();
        for (int i = 3; i < personalMessage.length; i++){
            textInMessage.append(personalMessage[i] + " ");
        }
        if (personalMessage[1].equals(ServerCommandConstants.PERSONAL_MESSAGE)){
            for(ClientHandler handler: connectedUsers) {
                if (personalMessage[2].equals(handler.getNickName())){
                    handler.sendMessage("personal from " + personalMessage[0] + " " + textInMessage);
                }
            }
        } else {
            try (FileOutputStream out = new FileOutputStream("messagehistory.txt",true)) {
                out.write(message.getBytes(StandardCharsets.UTF_8));
                out.write('\n');
            } catch (IOException e) {
                e.printStackTrace();
            }
            for(ClientHandler handler: connectedUsers) {
                handler.sendMessage(message);
            }
        }
    }

    public synchronized void addConnectedUser(ClientHandler handler) {
        connectedUsers.add(handler);
    }

    public synchronized void disconnectUser(ClientHandler handler) {
        connectedUsers.remove(handler);
    }

}
