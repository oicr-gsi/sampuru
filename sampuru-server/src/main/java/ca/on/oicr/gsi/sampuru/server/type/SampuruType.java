package ca.on.oicr.gsi.sampuru.server.type;

import ca.on.oicr.gsi.sampuru.server.DBConnector;
import org.jooq.Table;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

public abstract class SampuruType {

    protected static <T extends SampuruType> List<T> getAll(Table selfTable, Class<T> targetClass, String username)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, SQLException {
        DBConnector dbConnector = new DBConnector();
        List<Integer> ids = dbConnector.getAllIds(selfTable);
        List<T> newList = new LinkedList<>();

        for (Integer id: ids){
            newList.add(targetClass.getDeclaredConstructor(int.class, String.class).newInstance(id, username));
        }

        return newList;
    }
}
