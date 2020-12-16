package ca.on.oicr.gsi.sampuru.server.service;

import ca.on.oicr.gsi.sampuru.server.DBConnector;
import ca.on.oicr.gsi.sampuru.server.type.*;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.PathTemplateMatch;
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
        PathTemplateMatch ptm = hse.getAttachment(PathTemplateMatch.ATTACHMENT_KEY);
        //TODO: maybe in the future we'll want the opportunity for this to be blank
        String projectId = ptm.getParameters().get("projectId");
        hse.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        hse.getResponseSender().send(cs.getCardJsonForProject(projectId, username));
    }

    // Front end will need to group by case_id
    // Refactor me if we ever need a different set of Cards than just by Project
    public String getCardJsonForProject(String projectId, String username) throws Exception {
        DBConnector dbConnector = new DBConnector();
        DBConnector.JSONObjectMap cards = new DBConnector.JSONObjectMap();
        Result<Record> cardResults = dbConnector.execute(PostgresDSL
                .select()
                .from(CASE_CARD)
                .where(CASE_CARD.CASE_ID.in(PostgresDSL
                        .select(DONOR_CASE.ID)
                        .from(DONOR_CASE)
                        .where(DONOR_CASE.PROJECT_ID.eq(projectId))
                        .and(DONOR_CASE.PROJECT_ID.in(PostgresDSL
                                .select(USER_ACCESS.PROJECT)
                                .from(USER_ACCESS)
                                .where(USER_ACCESS.USERNAME.eq(username)))
                        .or(DBConnector.ADMIN_ROLE
                                .in(PostgresDSL
                                        .select(USER_ACCESS.PROJECT)
                                        .from(USER_ACCESS)
                                        .where(USER_ACCESS.USERNAME.eq(username))))))));

        for(Record result: cardResults){
            String thisId = result.get(CASE_CARD.CASE_ID);
            JSONObject currentCard = cards.get(thisId);
            if(null == currentCard.get("bars")) currentCard.put("bars", new JSONArray());
            JSONArray currentBars = (JSONArray)currentCard.get("bars");

            currentCard.put("id", thisId); // This will probably overwrite things many times, w/e
            currentCard.put("name", result.get(CASE_CARD.CASE_NAME)); // same
            JSONObject thisBar = new JSONObject();
            thisBar.put("library_design", result.get(CASE_CARD.LIBRARY_DESIGN));
            JSONArray steps = new JSONArray();
            JSONObject receiptStep = new JSONObject(),
                    extractionStep = new JSONObject(),
                    libraryPrepStep = new JSONObject(),
                    lowPassStep = new JSONObject(),
                    fullDepthStep = new JSONObject(),
                    informaticsStep = new JSONObject(),
                    finalReportStep = new JSONObject();

            receiptStep.put("type", "receipt");
            receiptStep.put("total", result.get(CASE_CARD.TISSUE_TOTAL));
            receiptStep.put("completed", result.get(CASE_CARD.TISSUE_COMPLETED));
            receiptStep.put("status", determineStepStatus((Long)receiptStep.get("completed"), (Long)receiptStep.get("total")));
            steps.add(receiptStep);

            extractionStep.put("type", "extraction");
            extractionStep.put("total", result.get(CASE_CARD.EXTRACTION_TOTAL));
            extractionStep.put("completed", result.get(CASE_CARD.EXTRACTION_COMPLETED));
            extractionStep.put("status", determineStepStatus((Long)extractionStep.get("completed"), (Long)extractionStep.get("total")));
            steps.add(extractionStep);

            libraryPrepStep.put("type", "library_prep");
            libraryPrepStep.put("total", result.get(CASE_CARD.LIBRARY_PREPARATION_TOTAL));
            libraryPrepStep.put("completed", result.get(CASE_CARD.LIBRARY_PREPARATION_COMPLETED));
            libraryPrepStep.put("status", determineStepStatus((Long)libraryPrepStep.get("completed"), (Long)libraryPrepStep.get("total")));
            steps.add(libraryPrepStep);

            lowPassStep.put("type", "low_pass");
            lowPassStep.put("total", result.get(CASE_CARD.LOW_PASS_SEQUENCING_TOTAL));
            lowPassStep.put("completed", result.get(CASE_CARD.LOW_PASS_SEQUENCING_COMPLETED));
            lowPassStep.put("status", determineStepStatus((Long)lowPassStep.get("completed"), (Long)lowPassStep.get("total")));
            steps.add(lowPassStep);

            fullDepthStep.put("type", "full_depth");
            fullDepthStep.put("total", result.get(CASE_CARD.FULL_DEPTH_SEQUENCING_TOTAL));
            fullDepthStep.put("completed", result.get(CASE_CARD.FULL_DEPTH_SEQUENCING_COMPLETED));
            fullDepthStep.put("status", determineStepStatus((Long)fullDepthStep.get("completed"), (Long)fullDepthStep.get("total")));
            steps.add(fullDepthStep);

            informaticsStep.put("type", "informatics");
            informaticsStep.put("total", result.get(CASE_CARD.INFORMATICS_INTERPRETATION_TOTAL));
            informaticsStep.put("completed", result.get(CASE_CARD.INFORMATICS_INTERPRETATION_COMPLETED));
            informaticsStep.put("status", determineStepStatus((Long)informaticsStep.get("completed"), (Long)informaticsStep.get("total")));
            steps.add(informaticsStep);

            finalReportStep.put("type", "final_report");
            finalReportStep.put("total", result.get(CASE_CARD.FINAL_REPORT_TOTAL));
            finalReportStep.put("completed", result.get(CASE_CARD.FINAL_REPORT_COMPLETED));
            finalReportStep.put("status", determineStepStatus((Long)finalReportStep.get("completed"), (Long)finalReportStep.get("total")));
            steps.add(finalReportStep);

            thisBar.put("steps", steps);
            currentBars.add(thisBar);
            currentCard.put("bars", currentBars);
            cards.put(thisId, currentCard);
        }

        // TODO: don't like that we're making a second request! fix somehow eventually maybe
        Result<Record> changelogResults = dbConnector.execute(PostgresDSL
                .select()
                .from(CHANGELOG)
                .where(CHANGELOG.CASE_ID.in(PostgresDSL
                        .select(DONOR_CASE.ID)
                        .from(DONOR_CASE)
                        .where(DONOR_CASE.PROJECT_ID.eq(projectId))
                        .and(DONOR_CASE.PROJECT_ID.in(PostgresDSL
                                .select(USER_ACCESS.PROJECT)
                                .from(USER_ACCESS)
                                .where(USER_ACCESS.USERNAME.eq(username)))
                        .or(DBConnector.ADMIN_ROLE
                                .in(PostgresDSL
                                        .select(USER_ACCESS.PROJECT)
                                        .from(USER_ACCESS)
                                        .where(USER_ACCESS.USERNAME.eq(username))))))));
        for(Record result: changelogResults){
            String thisId = result.get(CASE_CARD.CASE_ID);
            JSONObject currentCard = cards.get(thisId);
            if(null == currentCard.get("changelog")) currentCard.put("changelog", new JSONArray());
            JSONArray currentChangelog = (JSONArray)currentCard.get("changelog");
            JSONObject changelogEntry = new JSONObject();

            changelogEntry.put("id", result.get(CHANGELOG.ID));
            changelogEntry.put("change_date", result.get(CHANGELOG.CHANGE_DATE));
            changelogEntry.put("content", result.get(CHANGELOG.CONTENT));

            currentChangelog.add(changelogEntry);
            currentCard.put("changelog", currentChangelog);
            cards.put(thisId, currentCard);
        }

        return cards.toArray().toJSONString();
    }

    // TODO: do we need to worry about failures?
    private String determineStepStatus(Long completed, Long total){
        if(total == 0) return "not started";
        if(total == completed) return "passed";
        return "pending";
    }

    @Override
    public List<Case> getAll(String username) throws Exception {
        List<Case> cases = new LinkedList<>();

        // NOTE: need to use specifically PostgresDSL.array() rather than DSL.array(). The latter breaks it
        Result<Record> results = new DBConnector().execute(
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
                .where(DONOR_CASE.PROJECT_ID.in(PostgresDSL
                        .select(USER_ACCESS.PROJECT)
                        .from(USER_ACCESS)
                        .where(USER_ACCESS.USERNAME.eq(username)))));

        for(Record result: results){
            cases.add(new Case(result));
        }

        return cases;
    }

    // TODO: Probably refactor to limit repeat code with getAll
    public List<Case> getForProject(String projectId, String username) throws Exception {
        List<Case> cases = new LinkedList<>();

        // NOTE: need to use specifically PostgresDSL.array() rather than DSL.array(). The latter breaks it
        Result<Record> results = new DBConnector().execute(
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
    public List<Case> search(String term, String username) {
        List<Case> cases = new LinkedList<>();
        DBConnector dbConnector = new DBConnector();
        Result<Record> results = dbConnector.execute(PostgresDSL
                .select(DONOR_CASE.asterisk(),
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
                .where(DONOR_CASE.PROJECT_ID.like("%"+term+"%")
                        .and(DONOR_CASE.PROJECT_ID.in(PostgresDSL
                                .select(USER_ACCESS.PROJECT)
                                .from(USER_ACCESS)
                                .where(USER_ACCESS.USERNAME.eq(username)))
                        .or(DBConnector.ADMIN_ROLE.in(PostgresDSL
                                .select(USER_ACCESS.PROJECT)
                                .from(USER_ACCESS)
                                .where(USER_ACCESS.USERNAME.eq(username)))))));
        for(Record result: results){
            cases.add(new Case(result));
        }

        return cases;
    }

    @Override
    public String toJson(Collection<? extends SampuruType> toWrite, String username) throws Exception {
        return toJson(toWrite, false, username);
    }

    public String toJson(Collection<? extends SampuruType> toWrite, boolean expand, String username) throws Exception {
        JSONArray jsonArray = new JSONArray();

        for(SampuruType item: toWrite){
            JSONObject jsonObject = new JSONObject();
            Case caseItem = (Case) item;

            jsonObject.put("id", caseItem.id);
            jsonObject.put("name", caseItem.name);

            if(expand){
                jsonObject.put("deliverables", new DeliverableService().toJson(caseItem.getDeliverables(username), username));
                jsonObject.put("qcables", new QCableService().toJson(caseItem.getQcables(username), true, username));
                jsonObject.put("changelog", new ChangelogService().toJson(caseItem.getChangelog(username), username));
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
