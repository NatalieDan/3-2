package geekbrains.Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private final Server server;
    private final Socket socket;
    private final DataInputStream inputStream;
    private final DataOutputStream outputStream;

    private String nickName;

    public String getNickName() {
        return nickName;
    }

    public ClientHandler(Server server, Socket socket) {
        try {
            this.server = server;
            this.socket = socket;
            this.inputStream = new DataInputStream(socket.getInputStream());
            this.outputStream = new DataOutputStream(socket.getOutputStream());
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        authentication();
                        readMessages();
                    } catch (IOException exception) {
                        exception.printStackTrace();
                    } finally {
                        closeConnection();
                    }
                }
            }).start();
        } catch (IOException exception) {
            throw new RuntimeException("Проблемы при создании обработчика");
        }
    }

    public void authentication() throws IOException {
        while (true) {
            String message = inputStream.readUTF();
            if (message.startsWith(ServerCommandConstants.AUTHORIZATION)) {
                String[] authInfo = message.split(" ");
                String nickName = server.getAuthService().getNickNameByLoginAndPassword(authInfo[1], authInfo[2]);
                if (nickName != null) {
                    if (!server.isNickNameBusy(nickName)) {
                        sendMessage("/authok " + nickName);
                        StringBuilder previousMessages = new StringBuilder();
                        try (FileInputStream in = new FileInputStream("messagehistory.txt")) {
                            int x;
                            while ((x = in.read()) > -1) {
                                previousMessages.append((char)x);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        sendMessage(previousMessages.toString());
                        this.nickName = nickName;
                        server.broadcastMessage(nickName + " join the chat");
                        server.addConnectedUser(this);
                        return;
                    } else {
                        sendMessage("Учетная запись уже используется");
                    }
                } else {
                    sendMessage("Неверные логин или пароль");
                }
            }
        }
    }

    private void readMessages() throws IOException {
        while (true) {
            String messageInChat = inputStream.readUTF();
            System.out.println("from " + nickName + ": " + messageInChat);
            if(messageInChat.equals(ServerCommandConstants.SHUTDOWN)) {
                return;
            }
            server.broadcastMessage(nickName + ": " + messageInChat);
        }
    }

    public void sendMessage(String message) {
        try {
            outputStream.writeUTF(message);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private void closeConnection() {
        server.disconnectUser(this);
        server.broadcastMessage(nickName + " left the chat");
        try {
            outputStream.close();
            inputStream.close();
            socket.close();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }
}
