package ca.on.oicr.gsi.sampuru.server.type;

import ca.on.oicr.gsi.sampuru.server.DBConnector;
import org.jooq.Table;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;

public abstract class SampuruType {
    public int id;

    protected static <T extends SampuruType> List<T> getAll(Table selfTable, Class<T> targetClass)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        DBConnector dbConnector = new DBConnector();
        List<Integer> ids = dbConnector.getAllIds(selfTable);
        List<T> newList = new LinkedList<>();

        for (Integer id: ids){
            newList.add(targetClass.getDeclaredConstructor(int.class).newInstance(id));
        }

        return newList;
    }
}
