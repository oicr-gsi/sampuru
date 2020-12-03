package ca.on.oicr.gsi.sampuru.server;

import ca.on.oicr.gsi.sampuru.server.type.Project;
import ca.on.oicr.gsi.sampuru.server.type.SampuruType;
import org.jooq.Record;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.postgresql.ds.PGConnectionPoolDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

import static tables_generated.Tables.*;

public class DBConnector {
    // From sampuru-etl
    public static final String QC_PASSED = "passed";
    public static final String QC_FAILED = "failed";
    public static final String QC_PENDING = "pending";
    private Properties properties = Server.properties;
    private String dbUser = properties.getProperty("dbUser");
    private String pw = properties.getProperty("dbPassword");
    private String url = properties.getProperty("dbUrl");
    private static PGConnectionPoolDataSource pgDataSource;
    private String username;

    public DBConnector(String newUsername) {
        if (pgDataSource == null) {
            PGConnectionPoolDataSource pgDataSource = new PGConnectionPoolDataSource();
            pgDataSource.setUrl(url);
            pgDataSource.setUser(dbUser);
            pgDataSource.setPassword(pw);
            this.pgDataSource = pgDataSource;
        }
        this.username = newUsername;
    }

    private Connection getConnection() {
        try {
            return pgDataSource.getConnection();
        } catch (SQLException se) {
            throw new RuntimeException(se);
        }
    }

    private DSLContext getContext() {
        return getContext(getConnection());
    }

    private DSLContext getContext(Connection connection){
        return DSL.using(connection, SQLDialect.POSTGRES);
    }

    public Result execute(Select select){
        return getContext().fetch(select);
    }

    //TODO: filter by username
    public Record getUniqueRow(TableField field, Object toMatch) throws Exception {
        String tableName = field.getTable().getName();
        Result<Record> rowResult = getContext().select().from(field.getTable()).where(field.eq(toMatch)).fetch();
        if(rowResult.isEmpty()){
            throw new Exception(tableName + " does not exist"); // TODO: more precise exception
        } else if (rowResult.size() > 1){
            throw new Exception("Found >1 record for "+ tableName +" identifier " + toMatch); // TODO: more precise exception
        }
        return rowResult.get(0);
    }

    /**
     * @param idField Table's own ID field, eg DONOR_CASE.ID
     * @param matchField Field on which to match to toMatch, eg DONOR_CASE.PROJECT_ID
     * @param toMatch Actual id value to match
     * @param toCreate Class<T extends SampuruType> which we would like to create a list of
     * @param <T> some SampuruType
     * @return LinkedList of SampuruType specified
     * @throws Exception
     */
    //TODO: filter by username
    public <T extends SampuruType> List<T> getMany(TableField idField, TableField matchField, Object toMatch, Class<T> toCreate)
            throws Exception {
        List<T> newList = new LinkedList<>();

        Result<Record1<Integer>> applicableIds = getContext()
                .select(idField)
                .from(idField.getTable())
                .where(matchField.eq(toMatch))
                .fetch();

        for(Record1 r: applicableIds){
            newList.add(toCreate.getDeclaredConstructor(int.class).newInstance(r.get(idField)));
        }

        return newList;
    }

    //TODO: filter by username
    public List<Integer> getAllIds(Table getFrom){
        List<Integer> newList = new LinkedList<>();
        Field<Integer> idField = getFrom.field("id");

        Result<Record1<Integer>> idsFromDb = getContext().select(idField).from(getFrom).fetch();

        for(Record r: idsFromDb){
            newList.add(r.get(idField));
        }

        return newList;
    }

    //TODO: filter by username?? maybe??
    public List<Object> getChildIdList(Table getFrom, TableField matchField, Object toMatch){
        List<Object> newList = new LinkedList<>();
        Field<Object> idField = getFrom.field("id");

        Result<Record1<Object>> idsFromDb = getContext()
                .select(idField)
                .from(getFrom)
                .where(matchField.eq(toMatch))
                .fetch();

        for(Record r: idsFromDb){
            newList.add(r.get(idField));
        }

        return newList;
    }

    //TODO: filter by username
    public List<String> getCompletedProjectIds() throws Exception {
        List<String> projectsIdsList = new LinkedList<>();

        Result<Record> completedProjects = getContext()
                .select()
                .from(PROJECT)
                .where(PROJECT.COMPLETION_DATE.isNotNull())
                .fetch();

        for(Record r: completedProjects){
            projectsIdsList.add(r.get(PROJECT.ID));
        }

        return projectsIdsList;
    }

    //TODO: filter by username
    public List<String> getActiveProjectIds() throws Exception {
        List<String> projectsIdsList = new LinkedList<>();

        Result<Record> activeProjects = getContext()
                .select()
                .from(PROJECT)
                .where(PROJECT.COMPLETION_DATE.isNull())
                .fetch();

        for(Record r: activeProjects){
            projectsIdsList.add(r.get(PROJECT.ID));
        }

        return projectsIdsList;
    }

    //TODO: filter by username??
    public JSONArray getCaseBars(List<String> caseIdsToExpand){
        JSONObjectMap cards = new JSONObjectMap();
        DSLContext context = getContext();
        Result<Record> cardResults = context
                .select()
                .from(CASE_CARD)
                .where(CASE_CARD.CASE_ID.in(caseIdsToExpand))
                .fetch();
        for(Record result: cardResults){
            String thisId = result.get(CASE_CARD.CASE_ID);
            JSONObject currentCard = cards.get(thisId);
            if(null == currentCard.get("bars")) currentCard.put("bars", new JSONArray());
            JSONArray currentBars = (JSONArray)currentCard.get("bars");

            currentCard.put("id", thisId); // This will probably overwrite things many times, w/e
            currentCard.put("name", result.get(CASE_CARD.CASE_NAME)); // same
            JSONObject thisBar = new JSONObject();
            thisBar.put("library_design", result.get(CASE_CARD.LIBRARY_DESIGN) == null? "": result.get(CASE_CARD.LIBRARY_DESIGN));
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

        Result<Record> changelogResults = context
                .select()
                .from(CHANGELOG)
                .where(CHANGELOG.CASE_ID.in(caseIdsToExpand))
                .fetch();
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

        return cards.toArray();
    }

    // TODO: do we need to worry about failures?
    private String determineStepStatus(Long completed, Long total){
        if(total == 0) return "not started";
        if(total == completed) return "passed";
        return "pending";
    }

    public LocalDateTime getLastUpdate(Project project) {
        //TODO: this is a placeholder value
        return LocalDateTime.now();
//        DSLContext context = getContext();
//        Result<Record1<LocalDateTime>> result = context
//                .selectDistinct(CHANGELOG.CHANGE_DATE)
//                .from(CHANGELOG)
//                .where(
//                        CHANGELOG.CASE_ID.in(
//                                context.select(DONOR_CASE.ID)
//                                        .from(DONOR_CASE)
//                                        .where(DONOR_CASE.PROJECT_ID.eq(project.id))))
//                .orderBy(CHANGELOG.CHANGE_DATE.desc())
//                .fetch();
//        return result.get(0).value1();
    }

    //TODO: filter by username??
    public List<String> getFailedQCablesForProject(String id) {
        List<String> ids = new LinkedList<>();
        Result<Record1<String>> result = getContext()
                .select(QCABLE.ID)
                .from(QCABLE)
                .where(QCABLE.PROJECT_ID.eq(id)
                .and(QCABLE.FAILURE_REASON.isNotNull()))
                .fetch();

        for(Record1<String> r: result){
            ids.add(r.value1());
        }

        return ids;
    }

    //TODO: filter by username
    public JSONObject getSankeyTransitions(String projectId) throws SQLException {
        JSONObject jsonObject = new JSONObject();
        Result<Record> shouldBeSingularResult = getContext()
                .select()
                .from(SANKEY_TRANSITION)
                .where(SANKEY_TRANSITION.PROJECT_ID.eq(projectId))
                .fetch();
        if(shouldBeSingularResult.size() > 1) throw new SQLException(">1 row retrieved for sankey transitions with project id " + projectId);
        Record result = shouldBeSingularResult.get(0);

        // RECEIPT -> EXTRACTION
        JSONObject receiptObject = new JSONObject();
        receiptObject.put("total", result.get(SANKEY_TRANSITION.RECEIPT_TOTAL));
        receiptObject.put("extraction", result.get(SANKEY_TRANSITION.RECEIPT_EXTRACTION));
        receiptObject.put("failed", result.get(SANKEY_TRANSITION.RECEIPT_FAILED));
        jsonObject.put("receipt", receiptObject);

        // EXTRACTION -> LIBRARY PREPARATION
        JSONObject extractionObject = new JSONObject();
        extractionObject.put("total", result.get(SANKEY_TRANSITION.EXTRACTION_TOTAL));
        extractionObject.put("library_preparation", result.get(SANKEY_TRANSITION.EXTRACTION_LIBRARY_PREPARATION));
        extractionObject.put("failed", result.get(SANKEY_TRANSITION.EXTRACTION_FAILED));
        extractionObject.put("pending", result.get(SANKEY_TRANSITION.EXTRACTION_PENDING));
        jsonObject.put("extraction", extractionObject);

        // LIBRARY PREPARATION -> LOW PASS SEQUENCING
        JSONObject libPrepObject = new JSONObject();
        libPrepObject.put("total", result.get(SANKEY_TRANSITION.LIBRARY_PREPARATION_TOTAL));
        libPrepObject.put("low_pass_sequencing", result.get(SANKEY_TRANSITION.LIBRARY_PREPARATION_LOW_PASS_SEQUENCING));
        libPrepObject.put("failed", result.get(SANKEY_TRANSITION.LIBRARY_PREPARATION_FAILED));
        libPrepObject.put("pending", result.get(SANKEY_TRANSITION.LIBRARY_PREPARATION_PENDING));
        jsonObject.put("library_preparation", libPrepObject);

        // LOW PASS SEQUENCING -> FULL DEPTH SEQUENCING
        JSONObject lowPassObject = new JSONObject();
        lowPassObject.put("total", result.get(SANKEY_TRANSITION.LOW_PASS_SEQUENCING_TOTAL));
        lowPassObject.put("full_depth_sequencing", result.get(SANKEY_TRANSITION.LOW_PASS_SEQUENCING_FULL_DEPTH_SEQUENCING));
        lowPassObject.put("failed", result.get(SANKEY_TRANSITION.LOW_PASS_SEQUENCING_FAILED));
        lowPassObject.put("pending", result.get(SANKEY_TRANSITION.LOW_PASS_SEQUENCING_PENDING));
        jsonObject.put("low_pass_sequencing", lowPassObject);

        // FULL DEPTH SEQUENCING -> INFORMATICS INTERPRETATION
        JSONObject fullDepthObject = new JSONObject();
        fullDepthObject.put("total", result.get(SANKEY_TRANSITION.FULL_DEPTH_SEQUENCING_TOTAL));
        fullDepthObject.put("informatics_interpretation", result.get(SANKEY_TRANSITION.FULL_DEPTH_SEQUENCING_INFORMATICS_INTERPRETATION));
        fullDepthObject.put("failed", result.get(SANKEY_TRANSITION.FULL_DEPTH_SEQUENCING_FAILED));
        fullDepthObject.put("pending", result.get(SANKEY_TRANSITION.FULL_DEPTH_SEQUENCING_PENDING));
        jsonObject.put("full_depth_sequencing", fullDepthObject);

        // INFORMATICS INTERPRETATION -> FINAL REPORT
        JSONObject informaticsInterpretationObject = new JSONObject();
        informaticsInterpretationObject.put("total", result.get(SANKEY_TRANSITION.INFORMATICS_INTERPRETATION_TOTAL));
        informaticsInterpretationObject.put("final_report", result.get(SANKEY_TRANSITION.INFORMATICS_INTERPRETATION_FINAL_REPORT));
        informaticsInterpretationObject.put("failed", result.get(SANKEY_TRANSITION.INFORMATICS_INTERPRETATION_FAILED));
        informaticsInterpretationObject.put("pending", result.get(SANKEY_TRANSITION.INFORMATICS_INTERPRETATION_PENDING));
        jsonObject.put("informatics_interpretation", informaticsInterpretationObject);

        // FINAL REPORT -> COMPLETION
        JSONObject finalReportObject = new JSONObject();
        finalReportObject.put("total", result.get(SANKEY_TRANSITION.FINAL_REPORT_TOTAL));
        finalReportObject.put("passed", result.get(SANKEY_TRANSITION.FINAL_REPORT_PASSED));
        finalReportObject.put("failed", result.get(SANKEY_TRANSITION.FINAL_REPORT_FAILED));
        finalReportObject.put("pending", result.get(SANKEY_TRANSITION.FINAL_REPORT_PENDING));
        jsonObject.put("final_report", finalReportObject);

        return jsonObject;
    }

    //TODO: filter by username
    public JSONArray getQcableTable(List<String> caseIds){
        JSONArray jsonArray = new JSONArray();
        Result<Record> result = getContext()
                .select()
                .from(QCABLE_TABLE)
                .where(QCABLE_TABLE.CASE_ID.in(caseIds))
                .fetch();

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

    //TODO: filter by username
    public List<Object> search(Table targetTable, TableField idField, TableField contentField, String term){
        List<Object> items = new LinkedList<>();
        Result<Record> results = getContext()
                .selectDistinct(idField)
                .from(targetTable)
                .where(contentField.like("%"+term+"%")).fetch();
        for(Record record: results){
            items.add((record.get(idField)));
        }
        return items;
    }

    private class JSONArrayMap extends HashMap<String, JSONArray>{
        @Override
        public JSONArray get(Object key) {
            if(!this.containsKey(key)){
                if(!(key instanceof String)) throw new UnsupportedOperationException("JSONArrayMap needs String for key, got " + key.getClass());
                this.put((String)key, new JSONArray());
            }
            return super.get(key);
        }
    }

    private class JSONObjectMap extends HashMap<String, JSONObject>{
        @Override
        public JSONObject get(Object key) {
            if(!this.containsKey(key)){
                if(!(key instanceof String)) throw new UnsupportedOperationException("JSONObjectMap needs String for key, got " + key.getClass());
                this.put((String)key, new JSONObject());
            }
            return super.get(key);
        }

        public JSONArray toArray(){
            JSONArray target = new JSONArray();
            for (JSONObject object: this.values()){
                target.add(object);
            }
            return target;
        }
    }
}
