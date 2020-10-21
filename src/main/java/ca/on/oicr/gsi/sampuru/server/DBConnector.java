package ca.on.oicr.gsi.sampuru.server;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import static tables_generated.Tables.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.*;
import java.util.Properties;

public class DBConnector {
    Properties properties = readProperties();
    String userName = properties.getProperty("dbUser");
    String pw = properties.getProperty("dbPassword");
    String url = properties.getProperty("dbUrl");

    public void connect(){
        try(Connection conn = DriverManager.getConnection(url, userName, pw)){
            DSLContext create = DSL.using(conn, SQLDialect.POSTGRES);
            Result<Record> result = create.select().from(PROJECT).fetch();

            for (Record r: result){
                Integer id = r.getValue(PROJECT.ID);
                System.out.println("ID: " + id);
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
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
