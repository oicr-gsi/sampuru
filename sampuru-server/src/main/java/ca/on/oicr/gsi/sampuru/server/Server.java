package ca.on.oicr.gsi.sampuru.server;

import ca.on.oicr.gsi.sampuru.server.service.CaseService;
import ca.on.oicr.gsi.sampuru.server.service.NotificationService;
import ca.on.oicr.gsi.sampuru.server.service.ProjectService;
import ca.on.oicr.gsi.sampuru.server.service.QCableService;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.RoutingHandler;
import io.undertow.server.handlers.ExceptionHandler;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.handlers.resource.Resource;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.util.Headers;

import java.io.*;

public class Server {
    private static Undertow server;
    private static String resourcesRoot = "ca/on/oicr/gsi/sampuru/";

    /**
     * Handles REST requests. Endpoints not included should realistically never be needed.
     */
    private final static HttpHandler ROUTES = new RoutingHandler()
            // Normal REST endpoints
            .get("/projects", ProjectService::getAllParams)
            .get("/project/{id}", ProjectService::getIdParams)
            .get("/cases", CaseService::getAllParams)
            .get("/case/{id}", CaseService::getIdParams)
            .get("/notifications", NotificationService::getAllParams) //TODO most of these are unrealistic past alpha, get all _for user_
            .get("/notification/{id}", NotificationService::getIdParams)
            .get("/qcables", QCableService::getAllParams)
            .get("/qcable/{id}", QCableService::getIdParams)

            // Special frontend endpoints
            .get("/ui-test", Server::sendResourceOutputStream)
            .get("/active_projects", ProjectService::getActiveProjectsParams) //TODO IndexOutOfBoundsException
            .get("/completed_projects", ProjectService::getCompletedProjectsParams)
            .get("/cases_cards", CaseService::getCardsParams)
            .get("/qcables_table", QCableService::getAllQcablesTableParams)
            .get("/project_overview/{id}", ProjectService::getProjectOverviewParams)
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

    private static HttpHandler resourceHandler(ResourceManager manager){
        return new ResourceHandler(manager).addWelcomeFiles("public.html");
    }

    private static void sendResourceOutputStream(HttpServerExchange hse) throws IOException {
        ResourceManager resourceManager = new ClassPathResourceManager(Server.class.getClassLoader());
        Resource r = resourceManager.getResource("ca/on/oicr/gsi/sampuru/index.js");

        if(hse.isInIoThread()) {
            hse.dispatch(ROOT);
        }
        hse.startBlocking();
        hse.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
        add(r, hse.getOutputStream());
    }

    /** Add a file backed by a class resource*/
    private static void add(Resource r, OutputStream output) throws IOException {
        // Writing the file to the OutputStream of the exchange object
        final byte[] buf = new byte[8192];
        try(InputStream input = r.getUrl().openStream()){
            int count;
            while((count = input.read(buf, 0, buf.length)) > 0){
                output.write(buf, 0, count);
                output.flush();
            }
            output.close();
        } catch(final IOException e){
            e.printStackTrace();
        }
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
