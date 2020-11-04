package ca.on.oicr.gsi.sampuru.server.service;

import ca.on.oicr.gsi.sampuru.server.DBConnector;
import ca.on.oicr.gsi.sampuru.server.type.*;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static tables_generated.Tables.*;


public class CaseService extends Service<Case> {

    public CaseService(){
        super(Case.class);
    }

    public Case get(String name){
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public static void getIdParams(HttpServerExchange hse){
        getIdParams(hse);
    }

    public static void getAllParams(HttpServerExchange hse) throws Exception {
        getAllParams(new CaseService(), hse);
    }

    public static void getCardsParams(HttpServerExchange hse) throws Exception {
        CaseService cs = new CaseService();
        hse.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        hse.getResponseSender().send(cs.getCardJson(cs.getAll()));
    }

    public String getCardJson(List<Case> cases) throws Exception {
        JSONArray jsonArray = new JSONArray();

        for (SampuruType item: cases){
            Case caseItem = (Case)item;
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", caseItem.id);
            jsonObject.put("name", caseItem.name);

            //TODO: just get the JSON from the changelog itself
            List<ChangelogEntry> changelogForItem = caseItem.getChangelog();
            JSONArray changelogArray = new JSONArray();
            for (ChangelogEntry changelog: changelogForItem){
                JSONObject changelogJsonObject = new JSONObject();
                changelogJsonObject.put("id", changelog.id);
                changelogJsonObject.put("change_date", changelog.changeDate == null? "null": JSONObject.escape(changelog.changeDate.toString()));
                changelogJsonObject.put("content", changelog.content);
                changelogArray.add(changelogJsonObject);
            }
            jsonObject.put("changelog", changelogArray);

            jsonObject.put("bars", new DBConnector().buildCaseBars(caseItem));

            jsonArray.add(jsonObject);
        }

        return jsonArray.toJSONString();
    }

    @Override
    public List<Case> search(String term) throws Exception{
        List<Integer> ids = new DBConnector().search(DONOR_CASE, DONOR_CASE.ID, DONOR_CASE.NAME, term);
        List<Case> cases = new LinkedList<>();

        for (Integer id: ids){
            cases.add(get(id));
        }

        return cases;
    }

    @Override
    public String toJson(Collection<? extends SampuruType> toWrite) throws Exception {
        return toJson(toWrite, false);
    }

    public String toJson(Collection<? extends SampuruType> toWrite, boolean expand) throws Exception {
        JSONArray jsonArray = new JSONArray();

        for(SampuruType item: toWrite){
            JSONObject jsonObject = new JSONObject();
            Case caseItem = (Case) item;

            jsonObject.put("id", caseItem.id);
            jsonObject.put("name", caseItem.name);

            if(expand){
                jsonObject.put("deliverables", new DeliverableService().toJson(caseItem.getDeliverables()));
                jsonObject.put("qcables", new QCableService().toJson(caseItem.getQcables(), true));
                jsonObject.put("changelog", new ChangelogService().toJson(caseItem.getChangelog()));
            } else {
                jsonObject.put("deliverables", caseItem.deliverables);
                jsonObject.put("qcables", caseItem.qcables);
                jsonObject.put("changelog", caseItem.changelog);
            }

            jsonArray.add(jsonObject);
        }

        return jsonArray.toJSONString();
    }
}
