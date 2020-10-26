package ca.on.oicr.gsi.sampuru.server;

import ca.on.oicr.gsi.sampuru.server.type.SampuruType;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.impl.DSL;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.*;
import java.util.LinkedList;
import java.util.List;
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

    public DSLContext getContext(){
        return getContext(getConnection());
    }

    public DSLContext getContext(Connection connection){
        return DSL.using(connection, SQLDialect.POSTGRES);
    }

    public Record getUniqueRow(TableField field, Object toMatch) throws Exception {
        String tableName = field.getTable().getName();
        Result<Record> rowResult = getContext().select().from(field.getTable()).where(field.eq(toMatch)).fetch();
        if(rowResult.isEmpty()){
            throw new Exception(tableName + " does not exist"); // TODO: more precise exception
        } else if (rowResult.size() > 1){
            throw new Exception("Found >1 record for "+ tableName +" identifier " + toMatch); // TODO: more precise exception
        }
        return rowResult.get(0);
    }

    /**
     *
     * @param idField Table's own ID field, eg DONOR_CASE.ID
     * @param matchField Field on which to match to toMatch, eg DONOR_CASE.PROJECT_ID
     * @param toMatch Actual id value to match
     * @param toCreate Class<T extends SampuruType> which we would like to create a list of
     * @param <T> some SampuruType
     * @return LinkedList of SampuruType specified
     * @throws Exception
     */
    public <T extends SampuruType> List<T> getMany(TableField idField, TableField matchField, Object toMatch, Class<T> toCreate)
            throws Exception {
        List<T> newList = new LinkedList<>();

        Result<Record1<Integer>> applicableIds = getContext()
                .select(idField)
                .from(idField.getTable())
                .where(matchField.eq(toMatch))
                .fetch();

        for(Record1 r: applicableIds){
            newList.add(toCreate.getDeclaredConstructor(Integer.class).newInstance(r.getValue(idField)));
        }

        return newList;
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
