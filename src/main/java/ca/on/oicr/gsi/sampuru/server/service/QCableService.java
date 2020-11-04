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


public class QCableService extends Service<QCable> {

    public QCableService(){
        super(QCable.class);
    }

    public QCable get(String alias){
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public static void getIdParams(HttpServerExchange hse) throws Exception {
        getIdParams(new QCableService(), hse);
    }

    public static void getAllParams(HttpServerExchange hse) throws Exception {
        getAllParams(new QCableService(), hse);
    }

    @Override
    public List<QCable> search(String term) throws Exception {
        List<Integer> ids = new DBConnector().search(QCABLE, QCABLE.ID, QCABLE.OICR_ALIAS, term);
        List<QCable> qcables = new LinkedList<>();

        for (Integer id: ids){
            qcables.add(get(id));
        }

        return qcables;
    }

    @Override
    public String toJson(Collection<? extends SampuruType> toWrite) throws Exception {
        return toJson(toWrite, false);
    }

    public String toJson(Collection<? extends SampuruType> toWrite, boolean expand) throws Exception {
        JSONArray jsonArray = new JSONArray();

        for(SampuruType item: toWrite){
            JSONObject jsonObject = new JSONObject();
            QCable qcable = (QCable)item;

            jsonObject.put("id", qcable.id);
            jsonObject.put("status", qcable.status);
            jsonObject.put("failure_reason", qcable.failureReason  == null? "null": qcable.failureReason);
            jsonObject.put("library_design", qcable.libraryDesign  == null? "null": qcable.libraryDesign);
            jsonObject.put("type", qcable.type);
            jsonObject.put("parent_id", qcable.parentId  == null? "null": qcable.parentId);

            if(expand){
                List<ChangelogEntry> changelogEntries = qcable.getChangelog();
                jsonObject.put("changelog", new ChangelogService().toJson(changelogEntries));
            } else {
                jsonObject.put("changelog", qcable.changelog);
            }

            jsonArray.add(jsonObject);
        }

        return jsonArray.toJSONString();
    }

    public static void getCaseQcablesTableParams(HttpServerExchange hse) throws Exception {
        // TODO: get id, see Service TODO
        int id = 0;
        CaseService cs = new CaseService();
        QCableService qs = new QCableService();
        List<Case> cases = new LinkedList<>();
        //TODO: multiple
        cases.add(cs.get(id));
        hse.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        hse.getResponseSender().send(qs.getTableJson(cases));
    }

    public static void getAllQcablesTableParams(HttpServerExchange hse) throws Exception {
        CaseService cs = new CaseService();
        QCableService qs = new QCableService();
        hse.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        hse.getResponseSender().send(qs.getTableJson(cs.getAll()));
    }

    public String getTableJson(List<Case> cases) throws Exception {
        List<Integer> ids = new LinkedList<>();
        for (Case donorCase: cases){
            ids.add(donorCase.id);
        }
        return new DBConnector().getQcableTable(ids).toJSONString();
    }
}
