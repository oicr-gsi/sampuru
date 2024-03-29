package ca.on.oicr.gsi.sampuru.server.service;

import ca.on.oicr.gsi.sampuru.server.Server;
import ca.on.oicr.gsi.sampuru.server.type.SampuruType;
import io.undertow.server.HttpServerExchange;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.*;

public abstract class Service<T extends SampuruType> {
    private Class<T> targetClass;

    public Service(Class<T> newTarget){
        targetClass = newTarget;
    }

    public T get(Object id, String username) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        try {
            return targetClass.getDeclaredConstructor(String.class, String.class).newInstance(id, username);
        } catch (NoSuchMethodException nsme){
            return targetClass.getDeclaredConstructor(int.class, String.class).newInstance(id, username);
        }
    }

    //TODO: do these have a purpose beyond The Old Endpoints?
    public static void getIdParams(Service targetService, HttpServerExchange hse) throws Exception {
        String username = Server.getUsername(hse);
        Deque<String> idparams = hse.getQueryParameters().get("id");
        Set<SampuruType> items = new HashSet<>();
        for(String id: idparams){
            items.add(targetService.get(id, username));
        }
        Server.sendHTTPResponse(hse, targetService.toJson(items, username));
    }

    public abstract List<T> getAll(String username) throws Exception;

    public static void getAllParams(Service targetService, HttpServerExchange hse) throws Exception {
        String username = Server.getUsername(hse);
        Server.sendHTTPResponse(hse, targetService.toJson(targetService.getAll(username), username));
    }

    public abstract List<T> search(String term, String username) throws SQLException;

    public abstract String toJson(Collection<? extends  SampuruType> toWrite, String username) throws Exception;

}
