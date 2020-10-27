package ca.on.oicr.gsi.sampuru.server.service;

import ca.on.oicr.gsi.sampuru.server.type.Project;
import io.undertow.server.HttpServerExchange;

public class ProjectService extends Service {

    public ProjectService(){
        super(Project.class);
    }

    public Project get(String name){
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public static void getIdParams(HttpServerExchange hse) throws Exception {
        getIdParams(new ProjectService(), hse);
    }

    public static void getAllParams(HttpServerExchange hse){
        getAllParams(new ProjectService(), hse);
    }
}
