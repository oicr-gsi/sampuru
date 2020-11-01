package ca.on.oicr.gsi.sampuru.server.service;

import ca.on.oicr.gsi.sampuru.server.DBConnector;
import ca.on.oicr.gsi.sampuru.server.type.Case;
import ca.on.oicr.gsi.sampuru.server.type.QCable;
import ca.on.oicr.gsi.sampuru.server.type.SampuruType;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.jooq.Result;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;


import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

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

    // TODO implement
    public String toJson(Collection<? extends SampuruType> toWrite) {
        throw new UnsupportedOperationException("Not implemented yet");
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
        JSONArray jsonArray = new JSONArray();

        // Each row in table is a Case
        for (Case tableRow: cases){
            JSONObject row = new JSONObject();
            row.put("case_id", tableRow.id);
            row.put("case_name", tableRow.name);

            // All the qcables
            List<QCable> qcables = tableRow.getQcables();
            JSONArray qcableArray = new JSONArray();
            for (QCable qcable: qcables){
                JSONObject qcableObj = new JSONObject();
                qcableObj.put("id", qcable.id);
                qcableObj.put("alias", qcable.OICRAlias);
                qcableObj.put("status", qcable.status);
                qcableObj.put("failure_reason", qcable.failureReason);
                qcableObj.put("library_design", qcable.libraryDesign);
                qcableObj.put("type", qcable.type);
                qcableArray.add(qcableObj);
            }
            row.put("qcables", qcableArray);
            jsonArray.add(row);
        }
        return jsonArray.toJSONString();
    }
}
