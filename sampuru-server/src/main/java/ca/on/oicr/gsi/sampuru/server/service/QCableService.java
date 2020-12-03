package ca.on.oicr.gsi.sampuru.server.service;

import ca.on.oicr.gsi.sampuru.server.DBConnector;
import ca.on.oicr.gsi.sampuru.server.type.*;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.PathTemplateMatch;
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
    public List<QCable> getAll(String username) {
        List<QCable> qcables = new LinkedList<>();

        // NOTE: need to use specifically PostgresDSL.array() rather than DSL.array(). The latter breaks it
        Result<Record> results = new DBConnector(username).execute(
                PostgresDSL.select(QCABLE.asterisk(),
                        PostgresDSL.array(PostgresDSL
                                .select(CHANGELOG.ID)
                                .from(CHANGELOG)
                                .where(CHANGELOG.CASE_ID.eq(QCABLE.ID)))
                                .as(QCable.CHANGELOG_IDS))
                .from(QCABLE));

        for(Record result: results){
            qcables.add(new QCable(result));
        }

        return qcables;
    }

    @Override
    public List<QCable> search(String term, String username) throws Exception {
        List<String> ids = new DBConnector(username).search(QCABLE, QCABLE.ID, QCABLE.OICR_ALIAS, term).stream().map(o -> (String)o).collect(Collectors.toList());
        List<QCable> qcables = new LinkedList<>();

        for (String id: ids){
            qcables.add(get(id));
        }

        return qcables;
    }

    @Override
    public String toJson(Collection<? extends SampuruType> toWrite) throws Exception {
        return toJson(toWrite, false);
    }

    public String toJson(Collection<? extends SampuruType> toWrite, boolean expand) throws Exception {
        return "what are you doing";
//        JSONArray jsonArray = new JSONArray();
//
//        for(SampuruType item: toWrite){
//            JSONObject jsonObject = new JSONObject();
//            QCable qcable = (QCable)item;
//
//            jsonObject.put("id", qcable.id);
//            jsonObject.put("alias", qcable.OICRAlias);
//            jsonObject.put("status", qcable.status);
//            jsonObject.put("failure_reason", qcable.failureReason  == null? "null": qcable.failureReason);
//            jsonObject.put("library_design", qcable.libraryDesign  == null? "null": qcable.libraryDesign);
//            jsonObject.put("type", qcable.type);
//            jsonObject.put("parent_id", qcable.parentId  == null? "null": qcable.parentId);
//
//            if(expand){
//                List<ChangelogEntry> changelogEntries = qcable.getChangelog(username);
//                jsonObject.put("changelog", new ChangelogService().toJson(changelogEntries));
//            } else {
//                jsonObject.put("changelog", qcable.changelog);
//            }
//
//            jsonArray.add(jsonObject);
//        }
//
//        return jsonArray.toJSONString();
    }

    public static void getAllQcablesTableParams(HttpServerExchange hse) throws Exception {
        String name = hse.getRequestHeaders().get("X-Remote-User").element();
        CaseService cs = new CaseService();
        QCableService qs = new QCableService();
        hse.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        hse.getResponseSender().send(qs.getTableJsonFromCases(cs.getAll(name), name));
    }

    public static void getFilteredQcablesTableParams(HttpServerExchange hse) throws Exception {
        String name = hse.getRequestHeaders().get("X-Remote-User").element();
        QCableService qs = new QCableService();
        List<String> cases = new LinkedList<>();
        PathTemplateMatch ptm = hse.getAttachment(PathTemplateMatch.ATTACHMENT_KEY);
        String filterType = ptm.getParameters().get("filterType");
        String filterId = ptm.getParameters().get("filterId");
        hse.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        switch(filterType){
            case "project":
                ProjectService ps = new ProjectService();
                cases = ps.get(filterId, name).donorCases;
                break;
            case "case":
                cases.add(filterId);
                break;
            default:
                throw new UnsupportedOperationException("Bad filter type "
                        + filterType +" , supported types are: project, case");
        }
        hse.getResponseSender().send(qs.getTableJsonFromIds(cases, name));
    }

    public String getTableJsonFromCases(List<Case> cases, String username) throws Exception {
        List<String> ids = new LinkedList<>();
        for (Case donorCase: cases){
            ids.add(donorCase.id);
        }
        return new DBConnector(username).getQcableTable(ids).toJSONString();
    }

    public String getTableJsonFromIds(List<String> caseIds, String username) throws Exception {
        return new DBConnector(username).getQcableTable(caseIds).toJSONString();
    }
}
