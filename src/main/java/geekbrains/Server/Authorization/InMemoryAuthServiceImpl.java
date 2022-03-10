package geekbrains.Server.Authorization;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemoryAuthServiceImpl implements AuthService {
    private Map<String, UserData> users;
    private static Connection connection;
    private static Statement statement;

    public InMemoryAuthServiceImpl() {
        try {
            connect();
            users = findAll();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            disconnect();
        }
    }

    private static void connect() throws SQLException {
        connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/java3","postgres","pkol");
        statement = connection.createStatement();
    }

    private static void disconnect() {
        try{
            if (connection != null) {
                connection.close();
            }
        } catch (Exception e){
            e.printStackTrace();
        }

    }

    private Map findAll() throws SQLException {
        users = new HashMap<>();
        try (ResultSet rs = statement.executeQuery("select * from clients;")) {
            while (rs.next()) {
                users.put(rs.getString(2), new UserData(rs.getString(2), rs.getString(3), rs.getString(4)));
            }
        }
        return users;
    }

    @Override
    public void start() {
        System.out.println("Сервис аутентификации инициализирован");
    }

    @Override
    public synchronized String getNickNameByLoginAndPassword(String login, String password) {
        UserData user = users.get(login);
        // Ищем пользователя по логину и паролю, если нашли то возвращаем никнэйм
        if (user != null && user.getPassword().equals(password)) {
            return user.getNickName();
        }

        return null;
    }

    @Override
    public void end() {
        System.out.println("Сервис аутентификации отключен");
    }
}
