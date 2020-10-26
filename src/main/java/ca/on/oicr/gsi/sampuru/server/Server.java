package ca.on.oicr.gsi.sampuru.server;

import ca.on.oicr.gsi.sampuru.server.service.ProjectService;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.RoutingHandler;
import io.undertow.util.Headers;

public class Server {
    private static Undertow server;

    private final static HttpHandler ROUTES = new RoutingHandler()
            .get("/projects", ProjectService::getAllParams)
            .get("/project/{id}", ProjectService::getIdParams)
            .get("/", Server::helloWorld);

    public static void main(String[] args){
        server = Undertow.builder()
                .addHttpListener(8088, "localhost") // TODO: get these from config file
                .setHandler(ROUTES)
                .build();
        server.start();
    }

    private static void helloWorld(HttpServerExchange hse){
        hse.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
        hse.getResponseSender().send("You found Sampuru!");
    }
}
