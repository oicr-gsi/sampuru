package ca.on.oicr.gsi.sampuru.server.service;

import ca.on.oicr.gsi.sampuru.server.DBConnector;
import ca.on.oicr.gsi.sampuru.server.type.Case;
import ca.on.oicr.gsi.sampuru.server.type.Deliverable;
import ca.on.oicr.gsi.sampuru.server.type.SampuruType;
import io.undertow.server.HttpServerExchange;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

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

    @Override
    public List<Deliverable> search(String term) throws Exception {
        List<Integer> ids = new DBConnector().search(DELIVERABLE_FILE, DELIVERABLE_FILE.ID, DELIVERABLE_FILE.CONTENT, term);
        List<Deliverable> deliverables = new LinkedList<>();

        for (Integer id: ids){
            deliverables.add(get(id));
        }

        return deliverables;
    }

    @Override
    public String toJson(Collection<? extends SampuruType> toWrite){
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
