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

    public T get(String id) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        try {
            return targetClass.getDeclaredConstructor(String.class).newInstance(id);
        } catch (NoSuchMethodException nsme){
            return targetClass.getDeclaredConstructor(int.class).newInstance(Integer.valueOf(id));
        }
    }

    public static void getIdParams(Service targetService, HttpServerExchange hse) throws Exception {
        Deque<String> idparams = hse.getQueryParameters().get("id");
        Set<SampuruType> items = new HashSet<>();
        for(String id: idparams){
            items.add(targetService.get(id));
        }
        hse.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        hse.getResponseSender().send(targetService.toJson(items));
    }

    public abstract List<T> getAll() throws Exception;

    public static void getAllParams(Service targetService, HttpServerExchange hse) throws Exception {
        hse.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        hse.getResponseSender().send(targetService.toJson(targetService.getAll()));
    }

    // TODO this needs to be filterable
    public abstract List<T> search(String term) throws Exception;

    public abstract String toJson(Collection<? extends  SampuruType> toWrite) throws Exception;

}
