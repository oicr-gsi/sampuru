package ca.on.oicr.gsi.sampuru.server;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.*;
import java.util.Properties;

// TODO: Later, let's not just have 1 connection, but a pool of them. Methods would probably be static.
public class DBConnector {
    private Properties properties = readProperties();
    private String userName = properties.getProperty("dbUser");
    private String pw = properties.getProperty("dbPassword");
    private String url = properties.getProperty("dbUrl");
    private Connection connection;

    public DBConnector(){
        try {
            connection = DriverManager.getConnection(url, userName, pw);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public Connection getConnection(){
        return connection;
    }

    private Properties readProperties() {
        try{
            FileInputStream fis = new FileInputStream(System.getProperty("user.dir") + "/src/main/resources/sampuru.properties");
            Properties properties = new Properties();
            properties.load(fis);
            return properties;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
