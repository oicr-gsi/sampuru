package ca.on.oicr.gsi.sampuru.server.service;

import ca.on.oicr.gsi.sampuru.server.DBConnector;
import ca.on.oicr.gsi.sampuru.server.type.Deliverable;
import ca.on.oicr.gsi.sampuru.server.type.Project;
import ca.on.oicr.gsi.sampuru.server.type.SampuruType;
import io.undertow.server.HttpServerExchange;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.util.postgres.PostgresDSL;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

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

    @Override
    public List<Deliverable> getAll(String username) throws Exception {
        List<Deliverable> deliverables = new LinkedList<>();

        Result<Record> results = new DBConnector().execute(PostgresDSL.select()
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
        Result<Record> results = dbConnector.execute(PostgresDSL
                .select()
                .from(DELIVERABLE_FILE)
                .where(DELIVERABLE_FILE.CONTENT.like("%"+term+"%")
                        .and(DELIVERABLE_FILE.PROJECT_ID.in(PostgresDSL
                                .select(USER_ACCESS.PROJECT)
                                .from(USER_ACCESS)
                                .where(USER_ACCESS.USERNAME.eq(username))))
                        .or(DBConnector.ADMIN_ROLE.in(PostgresDSL
                                .select(USER_ACCESS.PROJECT)
                                .from(USER_ACCESS)
                                .where(USER_ACCESS.USERNAME.eq(username))))));
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
            jsonObject.put("content", deliverable.content);
            jsonObject.put("expiry_date", JSONObject.escape(deliverable.expiryDate.toString()));

            jsonArray.add(jsonObject);
        }

        return jsonArray.toJSONString();
    }
}
