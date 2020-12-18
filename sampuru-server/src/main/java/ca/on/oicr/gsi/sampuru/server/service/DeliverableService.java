package ca.on.oicr.gsi.sampuru.server.service;

import ca.on.oicr.gsi.sampuru.server.DBConnector;
import ca.on.oicr.gsi.sampuru.server.type.Deliverable;
import ca.on.oicr.gsi.sampuru.server.type.SampuruType;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.jooq.Context;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.util.postgres.PostgresDSL;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static tables_generated.Tables.*;


public class DeliverableService extends Service<Deliverable> {

    public DeliverableService(){
        super(Deliverable.class);
    }

    public static void getIdParams(HttpServerExchange hse) throws Exception {
        getIdParams(new DeliverableService(), hse);
    }

    public static void getAllParams(HttpServerExchange hse) throws Exception {
        getAllParams(new DeliverableService(), hse);
    }

    public static void endpointDisplayParams(HttpServerExchange hse) {
        String username = hse.getRequestHeaders().get("X-Remote-User").element();
        DeliverableService ds = new DeliverableService();

        hse.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        hse.getResponseSender().send(ds.getPortalJson(username));
    }

    private String getPortalJson(String username) {
        DBConnector dbConnector = new DBConnector();
        Result<Record> deliverableResults = dbConnector.fetch(PostgresDSL
                .select()
                .from(DELIVERABLE_FILE)
                .where(DELIVERABLE_FILE.PROJECT_ID.in(PostgresDSL
                        .select(USER_ACCESS.PROJECT)
                        .from(USER_ACCESS)
                        .where(USER_ACCESS.USERNAME.eq(username))))
                .or(DBConnector.ADMIN_ROLE.in(PostgresDSL
                        .select(USER_ACCESS.PROJECT)
                        .from(USER_ACCESS)
                        .where(USER_ACCESS.USERNAME.eq(username))))
        );
        Result<Record> projectCases = dbConnector.fetch(PostgresDSL
                .select()
                .from(DONOR_CASE)
                .where(DONOR_CASE.PROJECT_ID.in(PostgresDSL
                        .select(USER_ACCESS.PROJECT)
                        .from(USER_ACCESS)
                        .where(USER_ACCESS.USERNAME.eq(username)))
                    .or(DBConnector.ADMIN_ROLE.in(PostgresDSL
                        .select(USER_ACCESS.PROJECT)
                        .from(USER_ACCESS)
                        .where(USER_ACCESS.USERNAME.eq(username))))));
        JSONObject jsonObject = new JSONObject();
        JSONArray deliverablesArray = new JSONArray();
        for(Record deliverableResult: deliverableResults){
            JSONObject deliverableObject = new JSONObject();
            deliverableObject.put("id", deliverableResult.get(DELIVERABLE_FILE.ID));
            deliverableObject.put("project_id", deliverableResult.get(DELIVERABLE_FILE.PROJECT_ID));
            deliverableObject.put("case_ids", deliverableResult.get(DELIVERABLE_FILE.CASE_ID));
            deliverableObject.put("location", deliverableResult.get(DELIVERABLE_FILE.LOCATION));
            deliverableObject.put("notes", deliverableResult.get(DELIVERABLE_FILE.NOTES));
            deliverableObject.put("expiry_date", deliverableResult.get(DELIVERABLE_FILE.EXPIRY_DATE));
            deliverablesArray.add(deliverableObject);
        }
        jsonObject.put("deliverables", deliverablesArray);

        DBConnector.JSONArrayMap projectCasesArray = new DBConnector.JSONArrayMap();
        for(Record caseResult: projectCases){
            JSONObject caseObject = new JSONObject();
            caseObject.put("id", caseResult.get(DONOR_CASE.ID));
            caseObject.put("project_id", caseResult.get(DONOR_CASE.PROJECT_ID));
            caseObject.put("name", caseResult.get(DONOR_CASE.NAME));
            JSONArray currentProjectArray = projectCasesArray.get(caseObject.get("project_id"));
            currentProjectArray.add(caseObject);
            projectCasesArray.put((String)caseObject.get("project_id"), currentProjectArray);
        }
        jsonObject.put("project_cases", projectCasesArray.toJSONObject());

        return jsonObject.toJSONString();
    }

    @Override
    public List<Deliverable> getAll(String username) throws Exception {
        List<Deliverable> deliverables = new LinkedList<>();

        Result<Record> results = new DBConnector().fetch(PostgresDSL
                .select()
                .from(DELIVERABLE_FILE));

        for(Record result: results){
            deliverables.add(new Deliverable(result));
        }
        return deliverables;
    }

    @Override
    public List<Deliverable> search(String term, String username) {
        List<Deliverable> deliverables = new LinkedList<>();
        DBConnector dbConnector = new DBConnector();
        Result<Record> results = dbConnector.fetch(PostgresDSL
                .select()
                .from(DELIVERABLE_FILE)
                .where(DELIVERABLE_FILE.LOCATION.like("%"+term+"%")
                        .and(DELIVERABLE_FILE.PROJECT_ID.in(PostgresDSL
                                .select(USER_ACCESS.PROJECT)
                                .from(USER_ACCESS)
                                .where(USER_ACCESS.USERNAME.eq(username)))
                        .or(DBConnector.ADMIN_ROLE.in(PostgresDSL
                                .select(USER_ACCESS.PROJECT)
                                .from(USER_ACCESS)
                                .where(USER_ACCESS.USERNAME.eq(username)))))));
        for(Record result: results){
            deliverables.add(new Deliverable(result));
        }

        return deliverables;
    }

    @Override
    public String toJson(Collection<? extends SampuruType> toWrite, String username){
        JSONArray jsonArray = new JSONArray();

        for(SampuruType item: toWrite){
            JSONObject jsonObject = new JSONObject();
            Deliverable deliverable = (Deliverable)item;

            jsonObject.put("id", deliverable.id);
            jsonObject.put("location", deliverable.location);
            jsonObject.put("notes", deliverable.notes);
            jsonObject.put("expiry_date", JSONObject.escape(deliverable.expiryDate.toString()));

            jsonArray.add(jsonObject);
        }

        return jsonArray.toJSONString();
    }



    // TO TEST: see file 'json curl for writing deliverables'
    public static void postDeliverableParams(HttpServerExchange hse) {
        String username = hse.getRequestHeaders().get("X-Remote-User").element();
        DeliverableService ds = new DeliverableService();
        hse.getRequestReceiver().receiveFullBytes((httpServerExchange, bytes) -> {
            String fullJson = new String(bytes);
            JSONArray jsonArray = new JSONArray();
            try {
                jsonArray = (JSONArray) new JSONParser().parse(fullJson);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            new DBConnector().writeDeliverables(jsonArray, username);
            hse.getResponseSender().send(ds.getPortalJson(username));
        });

    }
}
