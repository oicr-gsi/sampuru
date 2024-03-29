package ca.on.oicr.gsi.sampuru.server;

import ca.on.oicr.gsi.sampuru.server.service.*;
import ca.on.oicr.gsi.sampuru.server.type.SampuruType;
import ca.on.oicr.gsi.prometheus.LatencyHistogram;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.RoutingHandler;
import io.undertow.server.handlers.BlockingHandler;
import io.undertow.server.handlers.ExceptionHandler;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.PathTemplateMatch;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import io.prometheus.client.hotspot.DefaultExports;
import io.undertow.util.StatusCodes;

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
                    // Special frontend endpoints. I've killed the normal REST endpoints
                    .get("/active_projects", monitor(ProjectService::getActiveProjectsParams))
                    .get("/completed_projects", monitor(ProjectService::getCompletedProjectsParams))
                    .get("/cases_cards/{projectId}", monitor(CaseService::getCardsParams))
                    .get("/qcables_table/{filterType}/{filterId}", monitor(QCableService::getFilteredQcablesTableParams))
                    .get("/project_overview/{id}", monitor(ProjectService::getProjectOverviewParams))
                    .get("/notifications-active", monitor(NotificationService::getActiveParams))
                    .get("/notifications-historic", monitor(NotificationService::getAllParams)) // This is the same as the all endpoint was, just want naming consistency
                    .get("/changelogs/{filterType}/{filterId}", monitor(ChangelogService::getFilteredChangelogs))
                    .get("/deliverables", monitor(DeliverableService::endpointDisplayParams))
                    .get("/search/{type}/{term}", monitor(Server::doSearch))
                    .get("/home", Server::helloWorld)
                    .post("/update_deliverable", monitor(new BlockingHandler(DeliverableService::postDeliverableParams)))
            )
            .addPrefixPath("/", new ResourceHandler(new ClassPathResourceManager(Server.class.getClassLoader(), "static"))
                    .setWelcomeFiles("index.html"))
            .addExactPath("/metrics", monitor(new BlockingHandler(Server::metrics)));

    private final static HttpHandler ROOT = Handlers.exceptionHandler(ROUTES)
            .addExceptionHandler(Exception.class, Server::handleException);

    private static final LatencyHistogram RESPONSE_TIME =
        new LatencyHistogram(
            "sampuru_http_response_time", "The response time to serve a query", "url");

    // see https://stackoverflow.com/questions/39742014/routing-template-format-for-undertow
    private static void doSearch(HttpServerExchange hse) throws Exception {
        String username = getUsername(hse);
        Service service = null;
        PathTemplateMatch ptm = hse.getAttachment(PathTemplateMatch.ATTACHMENT_KEY);
        String type = ptm.getParameters().get("type");
        String term = ptm.getParameters().get("term").toUpperCase(Locale.ROOT);
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
        sendHTTPResponse(hse, service.toJson(list, username));
    }

    //TODO: No error handling for, eg, /qcable/10000000
    public static void main(String[] args){
        DefaultExports.initialize();
        readProperties();
        server = Undertow.builder()
                .addHttpListener(Integer.valueOf(properties.getProperty("hostPort")), properties.getProperty("hostAddress"))
                .setHandler(ROOT)
                .build();
        server.start();
    }

    private static void metrics(HttpServerExchange exchange) {
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, TextFormat.CONTENT_TYPE_004);
        exchange.setStatusCode(StatusCodes.OK);
        try (final var os = exchange.getOutputStream();
             final var writer = new PrintWriter(os)) {
            TextFormat.write004(writer, CollectorRegistry.defaultRegistry.metricFamilySamples());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static HttpHandler monitor(HttpHandler handler) {
        return exchange -> {
            final var url = exchange.getAttachment(PathTemplateMatch.ATTACHMENT_KEY);
            try (final var ignored =
                RESPONSE_TIME.start(url == null ? "unknown" : url.getMatchedTemplate())) {
                handler.handleRequest(exchange);
            }
        };
    }

    private static void helloWorld(HttpServerExchange hse){
        String name = getUsername(hse);
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

    public static void sendHTTPResponse(HttpServerExchange hse, String body){
        sendHTTPResponse(hse, 200, body);
    }

    public static void sendHTTPResponse(HttpServerExchange hse, int status, String body){
        hse.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        hse.getResponseHeaders().put(HttpString.tryFromString("X-Common-Name"), hse.getRequestHeaders().get("X-Common-Name").element());
        hse.setStatusCode(status);
        hse.getResponseSender().send(body);
    }

    public static String getUsername(HttpServerExchange hse){
        return hse.getRequestHeaders().get("X-Remote-User").element();
    }
}
