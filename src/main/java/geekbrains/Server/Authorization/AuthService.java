package geekbrains.Server.Authorization;

public interface AuthService {
    void start();
    String getNickNameByLoginAndPassword(String login, String password);
    void end();
}
