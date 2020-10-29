package ca.on.oicr.gsi.sampuru.server;

import ca.on.oicr.gsi.sampuru.server.service.*;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.RoutingHandler;
import io.undertow.server.handlers.ExceptionHandler;
import io.undertow.util.Headers;

import java.io.PrintWriter;
import java.io.StringWriter;

public class Server {
    private static Undertow server;

    /**
     * Handles REST requests. Endpoints not included should realistically never be needed.
     */
    private final static HttpHandler ROUTES = new RoutingHandler()
            .get("/projects", ProjectService::getAllParams)
            .get("/project/{id}", ProjectService::getIdParams)
            .get("/cases", CaseService::getAllParams)
            .get("/case/{id}", CaseService::getIdParams)
            .get("/notifications", NotificationService::getAllParams) //TODO most of these are unrealistic past alpha, get all _for user_
            .get("/notification/{id}", NotificationService::getIdParams)
            .get("/qcables", QCableService::getAllParams)
            .get("/qcable/{id}", QCableService::getIdParams)
            .get("/", Server::helloWorld); //TODO: login?

    private final static HttpHandler ROOT = Handlers.exceptionHandler(ROUTES)
            .addExceptionHandler(Exception.class, Server::handleException);

    //TODO: No error handling for, eg, /qcable/10000000
    public static void main(String[] args){
        server = Undertow.builder()
                .addHttpListener(8088, "localhost") // TODO: get these from config file
                .setHandler(ROOT)
                .build();
        server.start();
    }

    private static void helloWorld(HttpServerExchange hse){
        hse.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
        hse.getResponseSender().send("You found Sampuru!");
    }

    protected static void handleException (HttpServerExchange hse){
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        Exception e = (Exception) hse.getAttachment(ExceptionHandler.THROWABLE);
        hse.setStatusCode(500); // TODO can probably set this more intelligently when we split this up by exception type
        e.printStackTrace(pw);
        hse.getResponseSender().send(sw.toString());
    }
}
