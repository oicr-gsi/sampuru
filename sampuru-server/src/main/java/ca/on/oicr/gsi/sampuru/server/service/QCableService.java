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
        Result<Record> results = new DBConnector().execute(
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
    public List<QCable> search(String term, String username) {
        List<QCable> qcables = new LinkedList<>();
        DBConnector dbConnector = new DBConnector();
        Result<Record> results = dbConnector.execute(PostgresDSL
                .select()
                .from(QCABLE)
                .where(QCABLE.PROJECT_ID.like("%"+term+"%")
                        .and(QCABLE.PROJECT_ID.in(PostgresDSL
                                .select(USER_ACCESS.PROJECT)
                                .from(USER_ACCESS)
                                .where(USER_ACCESS.USERNAME.eq(username))))
                        .or(DBConnector.ADMIN_ROLE.in(PostgresDSL
                                .select(USER_ACCESS.PROJECT)
                                .from(USER_ACCESS)
                                .where(USER_ACCESS.USERNAME.eq(username))))));
        for(Record result: results){
            qcables.add(new QCable(result));
        }

        return qcables;
    }

    @Override
    public String toJson(Collection<? extends SampuruType> toWrite, String username) throws Exception {
        return toJson(toWrite, false, username);
    }

    public String toJson(Collection<? extends SampuruType> toWrite, boolean expand, String username) throws Exception {
        JSONArray jsonArray = new JSONArray();

        for(SampuruType item: toWrite){
            JSONObject jsonObject = new JSONObject();
            QCable qcable = (QCable)item;

            jsonObject.put("id", qcable.id);
            jsonObject.put("alias", qcable.OICRAlias);
            jsonObject.put("status", qcable.status);
            jsonObject.put("failure_reason", qcable.failureReason  == null? "null": qcable.failureReason);
            jsonObject.put("library_design", qcable.libraryDesign  == null? "null": qcable.libraryDesign);
            jsonObject.put("type", qcable.type);
            jsonObject.put("parent_id", qcable.parentId  == null? "null": qcable.parentId);

            if(expand){
                List<ChangelogEntry> changelogEntries = qcable.getChangelog(username);
                jsonObject.put("changelog", new ChangelogService().toJson(changelogEntries, username));
            } else {
                jsonObject.put("changelog", qcable.changelog);
            }

            jsonArray.add(jsonObject);
        }

        return jsonArray.toJSONString();
    }

//    public static void getAllQcablesTableParams(HttpServerExchange hse) throws Exception {
//        String username = hse.getRequestHeaders().get("X-Remote-User").element();
//        CaseService cs = new CaseService();
//        QCableService qs = new QCableService();
//        hse.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
//        hse.getResponseSender().send(qs.getTableJsonFromCases(cs.getAll(username), username));
//    }

    public static void getFilteredQcablesTableParams(HttpServerExchange hse) throws Exception {
        String username = hse.getRequestHeaders().get("X-Remote-User").element();
        QCableService qs = new QCableService();
        PathTemplateMatch ptm = hse.getAttachment(PathTemplateMatch.ATTACHMENT_KEY);
        String filterType = ptm.getParameters().get("filterType");
        String filterId = ptm.getParameters().get("filterId");
        hse.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        switch(filterType){
            case "project":
                hse.getResponseSender().send(qs.getQcableTableFromProject(filterId, username).toJSONString());
                break;
            case "case":
                hse.getResponseSender().send(qs.getQcableTableFromCase(filterId, username).toJSONString());
                break;
            default:
                throw new UnsupportedOperationException("Bad filter type "
                        + filterType +" , supported types are: project, case");
        }
    }

    private JSONArray getQcableTableFromCase(String caseId, String username){
        return buildQcableTable(new DBConnector().execute(PostgresDSL
                .select()
                .from(QCABLE_TABLE)
                .where(QCABLE_TABLE.CASE_ID.eq(caseId)
                        .and(QCABLE_TABLE.PROJECT_ID.in(PostgresDSL
                                .select(USER_ACCESS.PROJECT)
                                .from(USER_ACCESS)
                                .where(USER_ACCESS.USERNAME.eq(username))))
                        .or(DBConnector.ADMIN_ROLE.in(PostgresDSL
                                .select(USER_ACCESS.PROJECT)
                                .from(USER_ACCESS)
                                .where(USER_ACCESS.USERNAME.eq(username)))))));
    }

    private JSONArray getQcableTableFromProject(String projectId, String username){
        return buildQcableTable(new DBConnector().execute(PostgresDSL
                .select()
                .from(QCABLE_TABLE)
                .where(QCABLE_TABLE.PROJECT_ID.eq(projectId)
                        .and(QCABLE_TABLE.PROJECT_ID.in(PostgresDSL
                                .select(USER_ACCESS.PROJECT)
                                .from(USER_ACCESS)
                                .where(USER_ACCESS.USERNAME.eq(username))))
                        .or(DBConnector.ADMIN_ROLE.in(PostgresDSL
                                .select(USER_ACCESS.PROJECT)
                                .from(USER_ACCESS)
                                .where(USER_ACCESS.USERNAME.eq(username)))))));
    }

    private JSONArray buildQcableTable(Result<Record> result){
        JSONArray jsonArray = new JSONArray();
        for(Record row: result){
            JSONObject jsonObject = new JSONObject();

            jsonObject.put("project_id", row.get(QCABLE_TABLE.PROJECT_ID));
            jsonObject.put("case_id", row.get(QCABLE_TABLE.CASE_ID));
            jsonObject.put("tissue_qcable_alias", row.get(QCABLE_TABLE.TISSUE_QCABLE_ALIAS));
            jsonObject.put("tissue_qcable_status", row.get(QCABLE_TABLE.TISSUE_QCABLE_STATUS));
            jsonObject.put("extraction_qcable_alias", row.get(QCABLE_TABLE.EXTRACTION_QCABLE_ALIAS));
            jsonObject.put("extraction_qcable_status", row.get(QCABLE_TABLE.EXTRACTION_QCABLE_STATUS));
            jsonObject.put("library_preparation_qcable_alias", row.get(QCABLE_TABLE.LIBRARY_PREPARATION_QCABLE_ALIAS));
            jsonObject.put("library_preparation_qcable_status", row.get(QCABLE_TABLE.LIBRARY_PREPARATION_QCABLE_STATUS));
            jsonObject.put("low_pass_sequencing_qcable_alias", row.get(QCABLE_TABLE.LOW_PASS_SEQUENCING_QCABLE_ALIAS));
            jsonObject.put("low_pass_sequencing_qcable_status", row.get(QCABLE_TABLE.LOW_PASS_SEQUENCING_QCABLE_STATUS));
            jsonObject.put("full_depth_sequencing_qcable_alias", row.get(QCABLE_TABLE.FULL_DEPTH_SEQUENCING_QCABLE_ALIAS));
            jsonObject.put("full_depth_sequencing_qcable_status", row.get(QCABLE_TABLE.FULL_DEPTH_SEQUENCING_QCABLE_STATUS));
            jsonObject.put("informatics_interpretation_qcable_alias", row.get(QCABLE_TABLE.INFORMATICS_INTERPRETATION_QCABLE_ALIAS));
            jsonObject.put("informatics_interpretation_qcable_status", row.get(QCABLE_TABLE.INFORMATICS_INTERPRETATION_QCABLE_STATUS));
            jsonObject.put("final_report_qcable_alias", row.get(QCABLE_TABLE.FINAL_REPORT_QCABLE_ALIAS));
            jsonObject.put("final_report_qcable_status", row.get(QCABLE_TABLE.FINAL_REPORT_QCABLE_STATUS));

            jsonArray.add(jsonObject);
        }
        return jsonArray;
    }
}
