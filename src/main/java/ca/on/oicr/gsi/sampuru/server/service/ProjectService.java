package ca.on.oicr.gsi.sampuru.server.service;

import ca.on.oicr.gsi.sampuru.server.type.Project;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

public class ProjectService extends Service {

    public ProjectService(){
        super(Project.class);
    }

    public Project get(String name){
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public static void getAllParams(HttpServerExchange hse){
        ProjectService ps = new ProjectService();
        hse.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
        try {
            hse.getResponseSender().send(ps.getAll().toString()); // TODO: JSON, not string
        } catch (NoSuchMethodException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            hse.getResponseSender().send(sw.toString());
        } catch (InvocationTargetException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            hse.getResponseSender().send(sw.toString());
        } catch (IllegalAccessException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            hse.getResponseSender().send(sw.toString());
        }
    }

    // TODO: it can only handle 1 id
    public static void getIdParams(HttpServerExchange hse){
        ProjectService ps = new ProjectService();
        Deque<String> idparams = hse.getQueryParameters().get("id"); // Why is this in query parameters when it's clearly in the URL? bug?
        Set<Integer> ids = new HashSet<>();
        for(String id: idparams){
            ids.add(Integer.valueOf(id));
        }
        hse.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
        hse.getResponseSender().send("hey " + ids);
    }
}
