package ca.on.oicr.gsi.sampuru.server.service;

import ca.on.oicr.gsi.sampuru.server.DBConnector;
import ca.on.oicr.gsi.sampuru.server.type.SampuruType;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.jooq.Result;
import org.jooq.Record;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public abstract class Service<T extends SampuruType> {
    private Class<T> targetClass;

    public Service(Class<T> newTarget){
        targetClass = newTarget;
    }

    public T get(int id) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        return targetClass.getDeclaredConstructor(int.class).newInstance(id);
    }

    public static void getIdParams(Service targetService, HttpServerExchange hse) throws Exception {
        Deque<String> idparams = hse.getQueryParameters().get("id");
        Set<SampuruType> items = new HashSet<>();
        for(String id: idparams){
            items.add(targetService.get(Integer.valueOf(id)));
        }
        hse.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        hse.getResponseSender().send(targetService.toJson(items));
    }

    public List<T> getAll() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        return (List<T>)targetClass.getDeclaredMethod("getAll").invoke(null);
    }

    public static void getAllParams(Service targetService, HttpServerExchange hse) throws Exception {
        hse.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        hse.getResponseSender().send(targetService.toJson(targetService.getAll()));
    }

    // TODO this needs to not search everything
    public abstract List<T> search(String term) throws Exception;

    public abstract String toJson(Collection<? extends  SampuruType> toWrite) throws Exception;

}
