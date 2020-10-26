package ca.on.oicr.gsi.sampuru.server.service;

import static tables_generated.Tables.*;

import ca.on.oicr.gsi.sampuru.server.DBConnector;
import ca.on.oicr.gsi.sampuru.server.type.SampuruType;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import java.sql.Connection;
import java.util.List;

public class Service<T extends SampuruType> {
    private Class<T> targetClass;

    public Service(Class<T> newTarget){
        targetClass = newTarget;
    }

    public T get(int id){
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public List<T> getAll(){
        //throw new UnsupportedOperationException("Not implemented yet");

        Connection connection = new DBConnector().getConnection();
        DSLContext query = DSL.using(connection, SQLDialect.POSTGRES);
        try {
            Result<Record> result = query.select().from().fetch();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    public List<T> search(String term){
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
