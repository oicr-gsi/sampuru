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
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.util.Headers;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;

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
            .get("/ui-test", resourceHandler1("", (int) TimeUnit.HOURS.toSeconds(4)))
            .get("/active_projects", ProjectService::getActiveProjectsParams) //TODO IndexOutOfBoundsException
            .get("/completed_projects", ProjectService::getCompletedProjectsParams)
            .get("/cases_cards", CaseService::getCardsParams)
            .get("/qcables_table", QCableService::getAllQcablesTableParams)
            .get("/project_overview/{id}", ProjectService::getProjectOverviewParams)
            .get("/", Server::helloWorld); //TODO: login?

    private final static HttpHandler ROOT = Handlers.exceptionHandler(ROUTES)
            .addExceptionHandler(Exception.class, Server::handleException);


    private final static HttpHandler ROOT2 = Handlers.path()
            .addPrefixPath("/api", Handlers.exceptionHandler(ROUTES)
                    .addExceptionHandler(Exception.class, Server::handleException))

            .addExactPath("/", Handlers.redirect("/static"))

            .addPrefixPath("/static", resourceHandler1("", (int) TimeUnit.HOURS.toSeconds(4)));


    //TODO: No error handling for, eg, /qcable/10000000
    public static void main(String[] args){
        server = Undertow.builder()
                .addHttpListener(8088, "localhost") // TODO: get these from config file
                .setHandler(ROOT2)
                .build();
        server.start();
    }

    private static void helloWorld(HttpServerExchange hse){
        hse.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
        hse.getResponseSender().send("You found Sampuru!");
    }

    /*
    private static void handleResourceRequest(HttpServerExchange hse) throws IOException { //todo: handle exceptions
        if(hse.isInIoThread()) {
            hse.dispatch(ROOT2);
            return;
        }
        hse.startBlocking();
        hse.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/javascript");
        add("index.js", hse.getOutputStream());
        //hse.getResponseSender().send(ByteBuffer.wrap(Files.readAllBytes(jsFile)));
    }*/

    private final static ResourceHandler resourceHandler() {
        return new ResourceHandler(
                new ClassPathResourceManager(Server.class.getClassLoader())
        ).setWelcomeFiles("public/index.html");

    }

    /** Add a file backed by a class resource*/ /*
    private static void add(String resource, OutputStream output) throws IOException {
        ResourceManager resourceManager = new ClassPathResourceManager(Server.class.getClassLoader());
        String pathToResource = resourcesRoot + resource;
        Resource r = resourceManager.getResource(pathToResource);

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
    }*/

    private static HttpHandler resourceHandler1(String prefix, int cacheTime) {
        //String path = Paths.get(resourcesRoot, prefix).toString();
        //ResourceManager resourceManager = new FileResourceManager(new File(path), 1024 * 1024);
        ResourceManager resourceManager = new ClassPathResourceManager(Server.class.getClassLoader(), prefix);

        ResourceHandler handler = new ResourceHandler(resourceManager);
        handler.setWelcomeFiles("public/index.html");
        handler.setCacheTime(cacheTime);
        return handler;
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
