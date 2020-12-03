package ca.on.oicr.gsi.sampuru.server.service;

import ca.on.oicr.gsi.sampuru.server.type.SampuruType;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public abstract class Service<T extends SampuruType> {
    private Class<T> targetClass;

    public Service(Class<T> newTarget){
        targetClass = newTarget;
    }

    //TODO: filter by username
    public T get(Object id, String username) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        try {
            return targetClass.getDeclaredConstructor(String.class).newInstance(id);
        } catch (NoSuchMethodException nsme){
            return targetClass.getDeclaredConstructor(int.class).newInstance(id);
        }
    }

    //TODO: do these have a purpose beyond The Old Endpoints?
    public static void getIdParams(Service targetService, HttpServerExchange hse) throws Exception {
        String username = hse.getRequestHeaders().get("X-Remote-User").element();
        Deque<String> idparams = hse.getQueryParameters().get("id");
        Set<SampuruType> items = new HashSet<>();
        for(String id: idparams){
            items.add(targetService.get(id, username));
        }
        hse.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        hse.getResponseSender().send(targetService.toJson(items));
    }

    public abstract List<T> getAll(String username) throws Exception;

    //TODO: filter by username
    public static void getAllParams(Service targetService, HttpServerExchange hse) throws Exception {
        String username = hse.getRequestHeaders().get("X-Remote-User").element();
        hse.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        hse.getResponseSender().send(targetService.toJson(targetService.getAll(username)));
    }

    public abstract List<T> search(String term, String username) throws Exception;

    public abstract String toJson(Collection<? extends  SampuruType> toWrite) throws Exception;

}
