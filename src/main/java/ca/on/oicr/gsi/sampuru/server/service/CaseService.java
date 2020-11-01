package ca.on.oicr.gsi.sampuru.server.service;

import ca.on.oicr.gsi.sampuru.server.DBConnector;
import ca.on.oicr.gsi.sampuru.server.type.*;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Collection;
import java.util.List;

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

            List<ChangelogEntry> changelogForItem = caseItem.getChangelog();
            JSONArray changelogArray = new JSONArray();
            for (ChangelogEntry changelog: changelogForItem){
                JSONObject changelogJsonObject = new JSONObject();
                changelogJsonObject.put("id", changelog.id);
                changelogJsonObject.put("change_date", changelog.changeDate);
                changelogJsonObject.put("content", changelog.content);
                changelogArray.add(changelogJsonObject);
            }
            jsonObject.put("changelog", changelogArray);

            jsonObject.put("bars", new DBConnector().buildCaseBars(caseItem));

            jsonArray.add(jsonObject);
        }

        return jsonArray.toJSONString();
    }

    public String toJson(Collection<? extends SampuruType> toWrite) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
