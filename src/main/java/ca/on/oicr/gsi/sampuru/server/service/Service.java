package ca.on.oicr.gsi.sampuru.server.service;

import ca.on.oicr.gsi.sampuru.server.type.Project;
import ca.on.oicr.gsi.sampuru.server.type.SampuruType;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Service<T extends SampuruType> {
    private Class<T> targetClass;

    public Service(Class<T> newTarget){
        targetClass = newTarget;
    }

    public T get(int id) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        return targetClass.getDeclaredConstructor(Integer.class).newInstance(id);
    }

    // TODO: it can only handle 1 id, how to get >1 id?
    public static void getIdParams(Service targetService, HttpServerExchange hse) throws Exception {
        Deque<String> idparams = hse.getQueryParameters().get("id"); // Why is this in query parameters when it's clearly in the URL? bug?
        Set<SampuruType> ids = new HashSet<>();
        for(String id: idparams){
            ids.add(targetService.get(Integer.valueOf(id)));
        }
        hse.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
        hse.getResponseSender().send(ids.toString());
    }

    public List<T> getAll() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        return (List<T>)targetClass.getDeclaredMethod("getAll").invoke(null);
    }

    public static void getAllParams(Service targetService, HttpServerExchange hse){
        hse.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
        try {
            hse.getResponseSender().send(targetService.getAll().toString()); // TODO: JSON, not string
        } catch (NoSuchMethodException e) {
            handleException(e, hse);
        } catch (InvocationTargetException e) {
            handleException(e, hse);
        } catch (IllegalAccessException e) {
            handleException(e, hse);
        }
    }

    public List<T> search(String term){
        throw new UnsupportedOperationException("Not implemented yet");
    }

    protected static void handleException (Exception e, HttpServerExchange hse){
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        hse.getResponseSender().send(sw.toString());
    }
}
