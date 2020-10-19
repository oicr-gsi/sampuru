package ca.on.oicr.gsi.sampuru.server.service;

import ca.on.oicr.gsi.sampuru.server.type.Project;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

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
        hse.getResponseSender().send(ps.getAll().toString());
    }
}
