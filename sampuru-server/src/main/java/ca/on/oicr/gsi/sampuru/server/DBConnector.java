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
    private String userName = properties.getProperty("dbUser");
    private String pw = properties.getProperty("dbPassword");
    private String url = properties.getProperty("dbUrl");
    private static PGConnectionPoolDataSource pgDataSource;

    public DBConnector() {
        if (pgDataSource == null) {
            PGConnectionPoolDataSource pgDataSource = new PGConnectionPoolDataSource();
            pgDataSource.setUrl(url);
            pgDataSource.setUser(userName);
            pgDataSource.setPassword(pw);
            this.pgDataSource = pgDataSource;
        }
    }

    public Connection getConnection() {
        try {
            return pgDataSource.getConnection();
        } catch (SQLException se) {
            throw new RuntimeException(se);
        }
    }

    public DSLContext getContext() {
        return getContext(getConnection());
    }

    public DSLContext getContext(Connection connection){
        return DSL.using(connection, SQLDialect.POSTGRES);
    }

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

    public List<Integer> getAllIds(Table getFrom){
        List<Integer> newList = new LinkedList<>();
        Field<Integer> idField = getFrom.field("id");

        Result<Record1<Integer>> idsFromDb = getContext().select(idField).from(getFrom).fetch();

        for(Record r: idsFromDb){
            newList.add(r.get(idField));
        }

        return newList;
    }

    public List<Integer> getChildIdList(Table getFrom, TableField matchField, Object toMatch){
        List<Integer> newList = new LinkedList<>();
        Field<Integer> idField = getFrom.field("id");

        Result<Record1<Integer>> idsFromDb = getContext()
                .select(idField)
                .from(getFrom)
                .where(matchField.eq(toMatch))
                .fetch();

        for(Record r: idsFromDb){
            newList.add(r.get(idField));
        }

        return newList;
    }

    public List<Integer> getCompletedProjectIds() throws Exception {
        List<Integer> projectsIdsList = new LinkedList<>();

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

    public List<Integer> getActiveProjectIds() throws Exception {
        List<Integer> projectsIdsList = new LinkedList<>();

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

    // TODO: these aren't killer for now, but would be great to move this to a field of Project to save on db connections
    public Integer getCasesTotal(Project project) {
        Result<Record1<Integer>> result = getContext()
                .selectCount()
                .from(DONOR_CASE)
                .where(DONOR_CASE.PROJECT_ID.eq(project.id))
                .fetch();
        return result.get(0).value1();
    }

    public Integer getCasesCompleted(Project project) {
        DSLContext context = getContext();
        Result<Record1<Integer>> result = context
                .selectCount()
                .from(
                        context.selectDistinct(QCABLE.CASE_ID)
                        .from(QCABLE)
                        .where(QCABLE.CASE_ID
                                .in(project.donorCases)
                                .and(QCABLE.QCABLE_TYPE.eq("final_report"))
                                .and(QCABLE.STATUS.eq(QC_PASSED))
                                .andExists(context
                                        .select(DELIVERABLE_FILE.ID)
                                        .from(DELIVERABLE_FILE)
                                        .where(DELIVERABLE_FILE.CASE_ID
                                                .in(context.select(QCABLE.CASE_ID)
                                                        .from(QCABLE)
                                                        .where(QCABLE.CASE_ID
                                                                .in(project.donorCases)
                                                                .and(QCABLE.QCABLE_TYPE.eq("final_report")))))))
                )
                .fetch();
        return result.get(0).value1();
    }


    public Integer getQCablesTotal(Project project) {
        Result<Record1<Integer>> result = getContext()
                .selectCount()
                .from(QCABLE)
                .where(QCABLE.PROJECT_ID.eq(project.id))
                .fetch();
        return result.get(0).value1();
    }

    public Integer getQCablesCompleted(Project project) {
        Result<Record1<Integer>> result = getContext()
                .selectCount()
                .from(QCABLE)
                .where(QCABLE.PROJECT_ID.eq(project.id).and(QCABLE.STATUS.eq(QC_PASSED)))
                .fetch();
        return result.get(0).value1();
    }

    public JSONArray getCaseBars(List<Integer> caseIdsToExpand){
        JSONObjectMap cards = new JSONObjectMap();
        DSLContext context = getContext();
        Result<Record> cardResults = context
                .select()
                .from(CASE_CARD)
                .where(CASE_CARD.CASE_ID.in(caseIdsToExpand))
                .fetch();
        for(Record result: cardResults){
            Integer thisId = result.get(CASE_CARD.CASE_ID);
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

            libraryPrepStep.put("type", "libraryPrep");
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
            Integer thisId = result.get(CASE_CARD.CASE_ID);
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

    public List<Integer> getFailedQCablesForProject(int id) {
        List<Integer> ids = new LinkedList<>();
        Result<Record1<Integer>> result = getContext()
                .select(QCABLE.ID)
                .from(QCABLE)
                .where(QCABLE.PROJECT_ID.eq(id)
                .and(QCABLE.FAILURE_REASON.isNotNull()))
                .fetch();

        for(Record1<Integer> r: result){
            ids.add(r.value1());
        }

        return ids;
    }

    // TODO: This could probably be a loop if I thought about it for a second
    // Logic is kind of wonky because ETL currently sets lots of things to 'pending' regardless of their children
    // TODO: This is almost sure to kill the db, isn't it
    public JSONObject buildSankeyTransitions(Project project) {
        Integer targetId = project.id;
        JSONObject jsonObject = new JSONObject();
        DSLContext context = getContext();

        // RECEIPT -> EXTRACTION
        JSONObject receiptObject = new JSONObject();
        Integer tissuesCount = context.selectCount().from(QCABLE_TABLE).where(QCABLE_TABLE.PROJECT_ID.eq(targetId).and(QCABLE_TABLE.TISSUE_QCABLE_ALIAS.isNotNull())).fetch().get(0).value1();
        receiptObject.put("total", tissuesCount);
        Integer tissuesPassedCount = context.selectCount().from(QCABLE_TABLE).where(QCABLE_TABLE.PROJECT_ID.eq(targetId).and(QCABLE_TABLE.EXTRACTION_QCABLE_ALIAS.isNotNull())).fetch().get(0).value1();
        receiptObject.put("extraction", tissuesPassedCount);
        Integer tissuesFailedCount = context.selectCount().from(QCABLE_TABLE).where(QCABLE_TABLE.PROJECT_ID.eq(targetId).and(QCABLE_TABLE.TISSUE_QCABLE_STATUS.eq(QC_FAILED))).fetch().get(0).value1();
        receiptObject.put("failed", tissuesFailedCount);
        jsonObject.put("receipt", receiptObject);

        // EXTRACTION -> LIBRARY PREPARATION
        JSONObject extractionObject = new JSONObject();
        Integer extractionCount = context.selectCount().from(QCABLE_TABLE).where(QCABLE_TABLE.PROJECT_ID.eq(targetId).and(QCABLE_TABLE.EXTRACTION_QCABLE_ALIAS.isNotNull())).fetch().get(0).value1();
        extractionObject.put("total", extractionCount);
        Integer extractionPassedCount = context.selectCount().from(QCABLE_TABLE).where(QCABLE_TABLE.PROJECT_ID.eq(targetId).and(QCABLE_TABLE.LIBRARY_PREPARATION_QCABLE_ALIAS.isNotNull())).fetch().get(0).value1();
        extractionObject.put("library_preparation", extractionPassedCount);
        Integer extractionFailedCount = context.selectCount().from(QCABLE_TABLE).where(QCABLE_TABLE.PROJECT_ID.eq(targetId).and(QCABLE_TABLE.EXTRACTION_QCABLE_STATUS.eq(QC_FAILED))).fetch().get(0).value1();
        extractionObject.put("failed", extractionFailedCount);
        Integer extractionPendingCount = context.selectCount().from(QCABLE_TABLE).where(QCABLE_TABLE.PROJECT_ID.eq(targetId).and(QCABLE_TABLE.EXTRACTION_QCABLE_STATUS.eq(QC_PENDING))).fetch().get(0).value1();
        extractionObject.put("pending", extractionPendingCount);
        jsonObject.put("extraction", extractionObject);

        // LIBRARY PREPARATION -> LOW PASS SEQUENCING
        JSONObject libPrepObject = new JSONObject();
        Integer libPrepCount = context.selectCount().from(QCABLE_TABLE).where(QCABLE_TABLE.PROJECT_ID.eq(targetId).and(QCABLE_TABLE.LIBRARY_PREPARATION_QCABLE_ALIAS.isNotNull())).fetch().get(0).value1();
        libPrepObject.put("total", libPrepCount);
        Integer libPrepPassedCount = context.selectCount().from(QCABLE_TABLE).where(QCABLE_TABLE.PROJECT_ID.eq(targetId).and(QCABLE_TABLE.LOW_PASS_SEQUENCING_QCABLE_ALIAS.isNotNull())).fetch().get(0).value1();
        libPrepObject.put("low_pass_sequencing", libPrepPassedCount);
        Integer libPrepFailedCount = context.selectCount().from(QCABLE_TABLE).where(QCABLE_TABLE.PROJECT_ID.eq(targetId).and(QCABLE_TABLE.LIBRARY_PREPARATION_QCABLE_STATUS.eq(QC_FAILED))).fetch().get(0).value1();
        libPrepObject.put("failed", libPrepFailedCount);
        Integer libPrepPendingCount = context.selectCount().from(QCABLE_TABLE).where(QCABLE_TABLE.PROJECT_ID.eq(targetId).and(QCABLE_TABLE.LIBRARY_PREPARATION_QCABLE_STATUS.eq(QC_PENDING))).fetch().get(0).value1();
        libPrepObject.put("pending", libPrepPendingCount);
        jsonObject.put("library_preparation", libPrepObject);

        // LOW PASS SEQUENCING -> FULL DEPTH SEQUENCING
        JSONObject lowPassObject = new JSONObject();
        Integer lowPassCount = context.selectCount().from(QCABLE_TABLE).where(QCABLE_TABLE.PROJECT_ID.eq(targetId).and(QCABLE_TABLE.LOW_PASS_SEQUENCING_QCABLE_ALIAS.isNotNull())).fetch().get(0).value1();
        lowPassObject.put("total", lowPassCount);
        Integer lowPassPassedCount = context.selectCount().from(QCABLE_TABLE).where(QCABLE_TABLE.PROJECT_ID.eq(targetId).and(QCABLE_TABLE.FULL_DEPTH_SEQUENCING_QCABLE_ALIAS.isNotNull())).fetch().get(0).value1();
        lowPassObject.put("full_depth_sequencing", lowPassPassedCount);
        Integer lowPassFailedCount = context.selectCount().from(QCABLE_TABLE).where(QCABLE_TABLE.PROJECT_ID.eq(targetId).and(QCABLE_TABLE.LOW_PASS_SEQUENCING_QCABLE_STATUS.eq(QC_FAILED))).fetch().get(0).value1();
        lowPassObject.put("failed", lowPassFailedCount);
        Integer lowPassPendingCount = context.selectCount().from(QCABLE_TABLE).where(QCABLE_TABLE.PROJECT_ID.eq(targetId).and(QCABLE_TABLE.LOW_PASS_SEQUENCING_QCABLE_STATUS.eq(QC_PENDING))).fetch().get(0).value1();
        lowPassObject.put("pending", lowPassPendingCount);
        jsonObject.put("lowPass", lowPassObject);

        // FULL DEPTH SEQUENCING -> INFORMATICS INTERPRETATION
        JSONObject fullDepthObject = new JSONObject();
        Integer fullDepthCount = context.selectCount().from(QCABLE_TABLE).where(QCABLE_TABLE.PROJECT_ID.eq(targetId).and(QCABLE_TABLE.FULL_DEPTH_SEQUENCING_QCABLE_ALIAS.isNotNull())).fetch().get(0).value1();
        fullDepthObject.put("total", fullDepthCount);
        Integer fullDepthPassedCount = context.selectCount().from(QCABLE_TABLE).where(QCABLE_TABLE.PROJECT_ID.eq(targetId).and(QCABLE_TABLE.INFORMATICS_INTERPRETATION_QCABLE_ALIAS.isNotNull())).fetch().get(0).value1();
        fullDepthObject.put("informatics_interpretation", fullDepthPassedCount);
        Integer fullDepthFailedCount = context.selectCount().from(QCABLE_TABLE).where(QCABLE_TABLE.PROJECT_ID.eq(targetId).and(QCABLE_TABLE.FULL_DEPTH_SEQUENCING_QCABLE_STATUS.eq(QC_FAILED))).fetch().get(0).value1();
        fullDepthObject.put("failed", fullDepthFailedCount);
        Integer fullDepthPendingCount = context.selectCount().from(QCABLE_TABLE).where(QCABLE_TABLE.PROJECT_ID.eq(targetId).and(QCABLE_TABLE.FULL_DEPTH_SEQUENCING_QCABLE_STATUS.eq(QC_PENDING))).fetch().get(0).value1();
        fullDepthObject.put("pending", fullDepthPendingCount);
        jsonObject.put("fullDepth", fullDepthObject);

        // INFORMATICS INTERPRETATION -> FINAL REPORT
        JSONObject informaticsInterpretationObject = new JSONObject();
        Integer informaticsInterpretationCount = context.selectCount().from(QCABLE_TABLE).where(QCABLE_TABLE.PROJECT_ID.eq(targetId).and(QCABLE_TABLE.INFORMATICS_INTERPRETATION_QCABLE_ALIAS.isNotNull())).fetch().get(0).value1();
        informaticsInterpretationObject.put("total", informaticsInterpretationCount);
        Integer informaticsInterpretationPassedCount = context.selectCount().from(QCABLE_TABLE).where(QCABLE_TABLE.PROJECT_ID.eq(targetId).and(QCABLE_TABLE.FINAL_REPORT_QCABLE_ALIAS.isNotNull())).fetch().get(0).value1();
        informaticsInterpretationObject.put("informaticsInterpretation", informaticsInterpretationPassedCount);
        Integer informaticsInterpretationFailedCount = context.selectCount().from(QCABLE_TABLE).where(QCABLE_TABLE.PROJECT_ID.eq(targetId).and(QCABLE_TABLE.INFORMATICS_INTERPRETATION_QCABLE_STATUS.eq(QC_FAILED))).fetch().get(0).value1();
        informaticsInterpretationObject.put("failed", informaticsInterpretationFailedCount);
        Integer informaticsInterpretationPendingCount = context.selectCount().from(QCABLE_TABLE).where(QCABLE_TABLE.PROJECT_ID.eq(targetId).and(QCABLE_TABLE.INFORMATICS_INTERPRETATION_QCABLE_STATUS.eq(QC_PENDING))).fetch().get(0).value1();
        informaticsInterpretationObject.put("pending", informaticsInterpretationPendingCount);
        jsonObject.put("informaticsInterpretation", informaticsInterpretationObject);

        // FINAL REPORT -> COMPLETION
        JSONObject finalReportObject = new JSONObject();
        Integer finalReportCount = context.selectCount().from(QCABLE_TABLE).where(QCABLE_TABLE.PROJECT_ID.eq(targetId).and(QCABLE_TABLE.FINAL_REPORT_QCABLE_ALIAS.isNotNull())).fetch().get(0).value1();
        finalReportObject.put("total", finalReportCount);
        Integer finalReportPassedCount = context.selectCount().from(QCABLE_TABLE).where(QCABLE_TABLE.PROJECT_ID.eq(targetId).and(QCABLE_TABLE.FINAL_REPORT_QCABLE_STATUS.eq(QC_PASSED))).fetch().get(0).value1();
        finalReportObject.put("passed", finalReportPassedCount);
        Integer finalReportFailedCount = context.selectCount().from(QCABLE_TABLE).where(QCABLE_TABLE.PROJECT_ID.eq(targetId).and(QCABLE_TABLE.FINAL_REPORT_QCABLE_STATUS.eq(QC_FAILED))).fetch().get(0).value1();
        finalReportObject.put("failed", finalReportFailedCount);
        Integer finalReportPendingCount = context.selectCount().from(QCABLE_TABLE).where(QCABLE_TABLE.PROJECT_ID.eq(targetId).and(QCABLE_TABLE.FINAL_REPORT_QCABLE_STATUS.eq(QC_PENDING))).fetch().get(0).value1();
        finalReportObject.put("pending", finalReportPendingCount);
        jsonObject.put("finalReport", finalReportObject);

        return jsonObject;
    }

    public JSONArray getQcableTable(List<Integer> caseIds){
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

    public List<Integer> search(Table targetTable, TableField idField, TableField contentField, String term){
        List<Integer> items = new LinkedList<>();
        Result<Record> results = getContext()
                .selectDistinct(idField)
                .from(targetTable)
                .where(contentField.like("%"+term+"%")).fetch();
        for(Record record: results){
            items.add((Integer)record.get(idField));
        }
        return items;
    }

    private class JSONArrayMap extends HashMap<Integer, JSONArray>{
        @Override
        public JSONArray get(Object key) {
            if(!this.containsKey(key)){
                if(!(key instanceof Integer)) throw new UnsupportedOperationException("JSONArrayMap needs Integer for key, got " + key.getClass());
                this.put((Integer)key, new JSONArray());
            }
            return super.get(key);
        }
    }

    private class JSONObjectMap extends HashMap<Integer, JSONObject>{
        @Override
        public JSONObject get(Object key) {
            if(!this.containsKey(key)){
                if(!(key instanceof Integer)) throw new UnsupportedOperationException("JSONObjectMap needs Integer for key, got " + key.getClass());
                this.put((Integer)key, new JSONObject());
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
