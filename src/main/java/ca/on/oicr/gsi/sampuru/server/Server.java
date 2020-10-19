package ca.on.oicr.gsi.sampuru.server;

import ca.on.oicr.gsi.sampuru.server.service.ProjectService;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.RoutingHandler;

public class Server {
    private static Undertow server;

    private final static HttpHandler ROUTES = new RoutingHandler()
            .get("/projects", ProjectService::getAllParams);

    public static void main(String[] args){
        server = Undertow.builder()
                .addHttpListener(8088, "localhost") // TODO: get these from config file
                .setHandler(ROUTES)
                .build();
        server.start();
    }
}
