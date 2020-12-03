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
        String username = hse.getRequestHeaders().get("X-Remote-User").element();
        CaseService cs = new CaseService();
        ProjectService ps = new ProjectService();
        PathTemplateMatch ptm = hse.getAttachment(PathTemplateMatch.ATTACHMENT_KEY);
        //TODO: maybe in the future we'll want the opportunity for this to be blank
        String projectId = ptm.getParameters().get("projectId");
        hse.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        //TODO: username being in two places is a red flag about the # of requests being made here
        hse.getResponseSender().send(cs.getCardJson(ps.get(projectId, username).donorCases, username));
    }

    // Front end will need to group by case_id
    public String getCardJson(List<String> caseIds, String username) throws Exception {
        return new DBConnector(username).getCaseBars(caseIds).toJSONString();
    }

    @Override
    public List<Case> getAll(String username) throws Exception {
        List<Case> cases = new LinkedList<>();

        // NOTE: need to use specifically PostgresDSL.array() rather than DSL.array(). The latter breaks it
        Result<Record> results = new DBConnector(username).execute(
                PostgresDSL.select(DONOR_CASE.asterisk(),
                        PostgresDSL.array(PostgresDSL
                                .select(QCABLE.ID)
                                .from(QCABLE)
                                .where(QCABLE.CASE_ID.eq(DONOR_CASE.ID)))
                                .as(Case.QCABLE_IDS),
                        PostgresDSL.array(PostgresDSL
                                .select(DELIVERABLE_FILE.ID)
                                .from(DELIVERABLE_FILE)
                                .where(DELIVERABLE_FILE.CASE_ID.eq(DONOR_CASE.ID)))
                                .as(Case.DELIVERABLE_IDS),
                        PostgresDSL.array(PostgresDSL
                                .select(CHANGELOG.ID)
                                .from(CHANGELOG)
                                .where(CHANGELOG.CASE_ID.eq(DONOR_CASE.ID)))
                                .as(Case.CHANGELOG_IDS))
                .from(DONOR_CASE));

        for(Record result: results){
            cases.add(new Case(result));
        }

        return cases;
    }

    // TODO: Probably refactor to limit repeat code with getAll
    public List<Case> getForProject(String projectId, String username) throws Exception {
        List<Case> cases = new LinkedList<>();

        // NOTE: need to use specifically PostgresDSL.array() rather than DSL.array(). The latter breaks it
        Result<Record> results = new DBConnector(username).execute(
                PostgresDSL.select(DONOR_CASE.asterisk(),
                        PostgresDSL.array(PostgresDSL
                                .select(QCABLE.ID)
                                .from(QCABLE)
                                .where(QCABLE.CASE_ID.eq(DONOR_CASE.ID)))
                                .as(Case.QCABLE_IDS),
                        PostgresDSL.array(PostgresDSL
                                .select(DELIVERABLE_FILE.ID)
                                .from(DELIVERABLE_FILE)
                                .where(DELIVERABLE_FILE.CASE_ID.eq(DONOR_CASE.ID)))
                                .as(Case.DELIVERABLE_IDS),
                        PostgresDSL.array(PostgresDSL
                                .select(CHANGELOG.ID)
                                .from(CHANGELOG)
                                .where(CHANGELOG.CASE_ID.eq(DONOR_CASE.ID)))
                                .as(Case.CHANGELOG_IDS))
                .from(DONOR_CASE)
                .where(DONOR_CASE.PROJECT_ID.eq(projectId)));

        for(Record result: results){
            cases.add(new Case(result));
        }

        return cases;
    }

    @Override
    public List<Case> search(String term, String username) throws Exception{
        List<Integer> ids = new DBConnector(username).search(DONOR_CASE, DONOR_CASE.ID, DONOR_CASE.NAME, term).stream().map(o -> (Integer)o).collect(Collectors.toList());
        List<Case> cases = new LinkedList<>();

        for (Integer id: ids){
            cases.add(get(id, username));
        }

        return cases;
    }

    @Override
    public String toJson(Collection<? extends SampuruType> toWrite) throws Exception{
        return "no";
    }

    public String toJson(Collection<? extends SampuruType> toWrite, boolean expand) throws Exception {
        return "no!";
    }

//    @Override
//    public String toJson(Collection<? extends SampuruType> toWrite) throws Exception {
//        return toJson(toWrite, false);
//    }
//
//    public String toJson(Collection<? extends SampuruType> toWrite, boolean expand) throws Exception {
//        JSONArray jsonArray = new JSONArray();
//
//        for(SampuruType item: toWrite){
//            JSONObject jsonObject = new JSONObject();
//            Case caseItem = (Case) item;
//
//            jsonObject.put("id", caseItem.id);
//            jsonObject.put("name", caseItem.name);
//
//            if(expand){
//                jsonObject.put("deliverables", new DeliverableService().toJson(caseItem.getDeliverables()));
//                jsonObject.put("qcables", new QCableService().toJson(caseItem.getQcables(), true));
//                jsonObject.put("changelog", new ChangelogService().toJson(caseItem.getChangelog()));
//            } else {
//                jsonObject.put("deliverables", caseItem.deliverables);
//                jsonObject.put("qcables", caseItem.qcables);
//                jsonObject.put("changelog", caseItem.changelog);
//            }
//
//            jsonArray.add(jsonObject);
//        }
//
//        return jsonArray.toJSONString();
//    }
}
