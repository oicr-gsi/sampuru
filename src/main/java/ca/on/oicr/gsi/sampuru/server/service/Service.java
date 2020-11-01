package ca.on.oicr.gsi.sampuru.server.service;

import ca.on.oicr.gsi.sampuru.server.type.Project;
import ca.on.oicr.gsi.sampuru.server.type.SampuruType;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import java.io.PrintWriter;
import java.io.StringWriter;
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

    // TODO: it can only handle 1 id, how to get >1 id?
    public static void getIdParams(Service targetService, HttpServerExchange hse) throws Exception {
        Deque<String> idparams = hse.getQueryParameters().get("id"); // Why is this in query parameters when it's clearly in the URL? bug?
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
        hse.getResponseSender().send(targetService.toJson(targetService.getAll())); // TODO: JSON, not string
    }

    public List<T> search(String term){
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public abstract String toJson(Collection<? extends  SampuruType> toWrite) throws Exception;

}
