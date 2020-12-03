package ca.on.oicr.gsi.sampuru.server;

import ca.on.oicr.gsi.sampuru.server.service.*;
import ca.on.oicr.gsi.sampuru.server.type.SampuruType;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.RoutingHandler;
import io.undertow.server.handlers.ExceptionHandler;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.util.Headers;
import io.undertow.util.PathTemplateMatch;

import java.io.*;
import java.util.*;

public class Server {
    private static Undertow server;
    public static Properties properties;

    /**
     * Handles REST requests. Endpoints not included should realistically never be needed.
     */
    private final static HttpHandler ROUTES = Handlers.path()
            .addPrefixPath("/api", new RoutingHandler()
                    // Normal REST endpoints. P sure ALL of these are tech debt. Deeply hateful
//                    .get("/projects", ProjectService::getAllParams)
//                    .get("/project/{id}", ProjectService::getIdParams) // TODO: update to use Row probably
//                    .get("/cases", CaseService::getAllParams)
//                    .get("/case/{id}", CaseService::getIdParams)
//                    .get("/notifications", NotificationService::getAllParams) //TODO most of these are unrealistic past alpha, get all _for user_
//                    .get("/notification/{id}", NotificationService::getIdParams)
//                    .get("/qcables", QCableService::getAllParams)
//                    .get("/qcable/{id}", QCableService::getIdParams)

                    // Special frontend endpoints
                    .get("/active_projects", ProjectService::getActiveProjectsParams)
                    .get("/completed_projects", ProjectService::getCompletedProjectsParams)
                    .get("/cases_cards/{projectId}", CaseService::getCardsParams)
                    .get("/qcables_table", QCableService::getAllQcablesTableParams)
                    .get("/qcables_table/{filterType}/{filterId}", QCableService::getFilteredQcablesTableParams)
                    .get("/project_overview/{id}", ProjectService::getProjectOverviewParams)
                    .get("/notifications-active", NotificationService::getActiveParams)
                    .get("/notifications-historic", NotificationService::getAllParams) // This is the same as the all endpoint, just want naming consistency
                    .get("/search/{type}/{term}", Server::doSearch)
                    .get("/home", Server::helloWorld)
            )
            .addPrefixPath("/", new ResourceHandler(new ClassPathResourceManager(Server.class.getClassLoader(), "static"))
                    .setWelcomeFiles("index.html"));

    // see https://stackoverflow.com/questions/39742014/routing-template-format-for-undertow
    private static void doSearch(HttpServerExchange hse) throws Exception {
        String username = hse.getRequestHeaders().get("X-Remote-User").element();
        Service service = null;
        PathTemplateMatch ptm = hse.getAttachment(PathTemplateMatch.ATTACHMENT_KEY);
        String type = ptm.getParameters().get("type");
        String term = ptm.getParameters().get("term");
        List<? extends SampuruType> list;

        switch(type){
            case "project":
                service = new ProjectService();
                break;
            case "case":
                service = new CaseService();
                break;
            case "changelog":
                service = new ChangelogService();
                break;
            case "deliverable":
                service = new DeliverableService();
                break;
            case "notification":
                service = new NotificationService();
                break;
            case "qcable":
                service = new QCableService();
                break;
            default:
                throw new Exception("Invalid search type " + type);
        }
        list = service.search(term, username);
        hse.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        hse.getResponseSender().send(service.toJson(list));
    }

    private final static HttpHandler ROOT = Handlers.exceptionHandler(ROUTES)
            .addExceptionHandler(Exception.class, Server::handleException);

    //TODO: No error handling for, eg, /qcable/10000000
    public static void main(String[] args){
        readProperties();
        server = Undertow.builder()
                .addHttpListener(Integer.valueOf(properties.getProperty("hostPort")), properties.getProperty("hostAddress"))
                .setHandler(ROOT)
                .build();
        server.start();
    }

    private static void helloWorld(HttpServerExchange hse){
        String name = hse.getRequestHeaders().get("X-Remote-User").element();
        hse.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
        hse.getResponseSender().send("You found Sampuru! Good job, " + name);
    }

    protected static void handleException (HttpServerExchange hse){
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        Exception e = (Exception) hse.getAttachment(ExceptionHandler.THROWABLE);
        hse.setStatusCode(500); // TODO can probably set this more intelligently when we split this up by exception type
        e.printStackTrace(pw);
        hse.getResponseSender().send(sw.toString());
    }

    private static void readProperties() {
        try{
            InputStream is = Server.class.getResourceAsStream("/sampuru.properties");
            properties = new Properties();
            properties.load(is);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
