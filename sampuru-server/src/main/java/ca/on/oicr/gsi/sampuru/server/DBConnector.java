package ca.on.oicr.gsi.sampuru.server;

import ca.on.oicr.gsi.sampuru.server.service.ServiceUtils;
import com.zaxxer.hikari.HikariConfig;
import org.jooq.Record;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.jooq.util.postgres.PostgresDSL;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import com.zaxxer.hikari.HikariDataSource;

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

    public static final Param<String> ADMIN_ROLE = PostgresDSL.val("ADMINISTRATOR");

    private Properties properties = Server.properties;
    private String dbUser = properties.getProperty("dbUser");
    private String pw = properties.getProperty("dbPassword");
    private String url = properties.getProperty("dbUrl");
    private static HikariDataSource pgDataSource;

    public DBConnector() {
        if (pgDataSource == null) {
            HikariDataSource pgDataSource = new HikariDataSource();
            HikariConfig config = new HikariConfig();
            pgDataSource.setJdbcUrl(url);
            pgDataSource.setUsername(dbUser);
            pgDataSource.setPassword(pw);
            this.pgDataSource = pgDataSource;
        }
    }

    private Connection getConnection() {
        try {
            return pgDataSource.getConnection();
        } catch (SQLException se) {
            throw new RuntimeException(se);
        }
    }

    private DSLContext getContext(Connection connection){
        return DSL.using(connection, SQLDialect.POSTGRES);
    }

    //TODO: I wanted this as a catch all to do a filter by username but it doesn't look like al's gonna be so lucky
    public Result fetch(SelectConnectByStep<Record> select) throws SQLException {
        try(final Connection connection = getConnection()){
            return getContext(connection).fetch(select);
        }
    }

    public <T> Result fetch(SelectSeekStep1<Record, T> select) throws SQLException {
        try(final Connection connection = getConnection()) {
            return getContext(connection).fetch(select);
        }
    }

    //TODO: filter by username, probably by removing me
    public Record getUniqueRow(TableField field, Object toMatch) throws Exception {
        String tableName = field.getTable().getName();
        try(final Connection connection = getConnection()){
            Result<Record> rowResult = getContext(connection).select().from(field.getTable()).where(field.eq(toMatch)).fetch();
            if(rowResult.isEmpty()){
                throw new Exception(tableName + " does not exist"); // TODO: more precise exception
            } else if (rowResult.size() > 1){
                throw new Exception("Found >1 record for "+ tableName +" identifier " + toMatch); // TODO: more precise exception
            }
            return rowResult.get(0);
        }
    }

    // TODO: look at https://www.jooq.org/doc/3.14/manual/sql-building/dynamic-sql/
    public void writeDeliverables(JSONArray deliverableArray, String username) throws Exception {
        if (deliverableArray.isEmpty()) return;

        // Front end will provide ID if deliverable is pre-existing. If no ID or ID is null, INSERT instead of UPDATE
        JSONArray knownDeliverables = new JSONArray(),
                unknownDeliverables = new JSONArray();
        for(Object obj: deliverableArray){
            if(!(obj instanceof JSONObject)){
                // TODO: More precise exception
                throw new Exception("Got something other than a JSONObject in the JSONArray coming into " +
                        "writeDeliverables(): " + obj.toString());
            }
            JSONObject jsonObject = (JSONObject) obj;
            if(jsonObject.containsKey("id")){
                knownDeliverables.add(jsonObject);
            } else {
                unknownDeliverables.add(jsonObject);
            }
        }

        if(!unknownDeliverables.isEmpty()) {
            try(final Connection connection = getConnection()){
                // Write the new deliverables to the database and get back the new SERIAL ids
                // Wrapped in transaction to ensure rollback on bad case_id
                getContext(connection).transaction(configuration -> {
                    // Format JSONArray into a List of Rows so it can be passed to .valuesOfRows
                    List<Row4<String, String, String, LocalDateTime>> formattedUnknownDeliverables = new ArrayList<>();
                    for(Object obj: unknownDeliverables) {
                        JSONObject nextDeliverable = (JSONObject) obj;
                        Row4<String, String, String, LocalDateTime> row = DSL.row(nextDeliverable.get("project_id").toString(), nextDeliverable.get("location").toString(), nextDeliverable.get("notes").toString(), LocalDateTime.parse(nextDeliverable.get("expiry_date").toString(), ServiceUtils.DATE_TIME_FORMATTER));
                        formattedUnknownDeliverables.add(row);
                    }

                    // Jooq 3.15 introduced .valuesOfRows for adding collection of values to Insert statements safely
                    Result<Record1<Integer>> deliverableIds = PostgresDSL.using(configuration).insertInto(DELIVERABLE_FILE, DELIVERABLE_FILE.PROJECT_ID, DELIVERABLE_FILE.LOCATION, DELIVERABLE_FILE.NOTES, DELIVERABLE_FILE.EXPIRY_DATE)
                        .valuesOfRows(formattedUnknownDeliverables)
                        .returningResult(DELIVERABLE_FILE.ID).fetch();

                    // Associate the new ids with the appropriate case ids and write to deliverable_case table
                    List<Row2<String, Integer>> deliverableCase = new ArrayList<>();
                    for(int i = 0; i < deliverableIds.size(); i++) {
                        Integer deliverableId = Integer.valueOf(Objects.requireNonNull(deliverableIds.get(i).get("id")).toString());
                        JSONArray casesForId = ((JSONArray) ((JSONObject)unknownDeliverables.get(i)).get("case_id"));
                        for(Object obj: casesForId) {
                            String caseId = (String) obj;
                            Row2<String, Integer> row = DSL.row(caseId, deliverableId);
                            deliverableCase.add(row);
                        }
                    }

                    PostgresDSL.using(configuration).insertInto(DELIVERABLE_CASE, DELIVERABLE_CASE.CASE_ID, DELIVERABLE_CASE.DELIVERABLE_ID)
                        .valuesOfRows(deliverableCase)
                        .execute();
                });
            }
        }

        // Update the existing deliverables
        if(!knownDeliverables.isEmpty()) {
            try (final Connection connection = getConnection()) {
                getContext(connection).transaction(configuration -> {
                    InsertSetStep deliverableCaseInsertSetStep = PostgresDSL.using(configuration).insertInto(DELIVERABLE_CASE);
                    InsertValuesStepN deliverableCaseInsertValuesStepN = null;
                    for (Object obj : knownDeliverables) {
                        JSONObject nextDeliverable = (JSONObject) obj;
                        Integer deliverableId = Math.toIntExact((Long) nextDeliverable.get("id"));
                        PostgresDSL.using(configuration).update(DELIVERABLE_FILE)
                            .set(DELIVERABLE_FILE.PROJECT_ID, (String) nextDeliverable.get("project_id"))
                            .set(DELIVERABLE_FILE.LOCATION, (String) nextDeliverable.get("location"))
                            .set(DELIVERABLE_FILE.NOTES, (String) nextDeliverable.get("notes"))
                            .set(DELIVERABLE_FILE.EXPIRY_DATE, PostgresDSL.localDateTime(nextDeliverable.get("expiry_date").toString()))
                            .where(DELIVERABLE_FILE.ID.eq(deliverableId)
                                .and(ADMIN_ROLE.in(PostgresDSL.select(USER_ACCESS.PROJECT).from(USER_ACCESS).where(USER_ACCESS.USERNAME.eq(username)))))
                            .execute();


                        // drop the old deliverable-case associations and rebuild
                        PostgresDSL.using(configuration).delete(DELIVERABLE_CASE)
                            .where(DELIVERABLE_CASE.DELIVERABLE_ID.eq(Math.toIntExact((Long) nextDeliverable.get("id")))
                                .and(ADMIN_ROLE.in(PostgresDSL.select(USER_ACCESS.PROJECT).from(USER_ACCESS).where(USER_ACCESS.USERNAME.eq(username)))))
                            .execute();
                        JSONArray casesForId = ((JSONArray) nextDeliverable.get("case_id"));

                        for (Object caseObj : casesForId) {
                            String caseId = (String) caseObj;
                            deliverableCaseInsertValuesStepN =
                                deliverableCaseInsertSetStep.values(checkWritePermission(deliverableId, username),
                                    checkWritePermission(caseId, username));
                        }
                    }
                    deliverableCaseInsertValuesStepN.execute();
                });
            }
        }
    }

    private Object checkWritePermission(Object value, String username){
        return PostgresDSL.when(ADMIN_ROLE.in(PostgresDSL.select(USER_ACCESS.PROJECT).from(USER_ACCESS).where(USER_ACCESS.USERNAME.eq(username))), value);
    }

    //TODO: filter by username, probably by rewrite
    public List<Integer> getAllIds(Table getFrom) throws SQLException {
        List<Integer> newList = new LinkedList<>();
        Field<Integer> idField = (Field<Integer>) getFrom.field("id");

        try(final Connection connection = getConnection()){
            Result<Record1<Integer>> idsFromDb = getContext(connection).select(idField).from(getFrom).fetch();

            for(Record r: idsFromDb){
                newList.add(r.get(idField));
            }
        }

        return newList;
    }

    //TODO: filter by username?? maybe?? probably by rewrite
    public List<Object> getChildIdList(Table getFrom, TableField matchField, Object toMatch) throws SQLException {
        return getChildIdList(getFrom, matchField, toMatch, getFrom.field("id"));
    }

    public List<Object> getChildIdList(Table getFrom, TableField matchField, Object toMatch, Field idField) throws SQLException {
        List<Object> newList = new LinkedList<>();

        try(final Connection connection = getConnection()){
            Result<Record1<Object>> idsFromDb = getContext(connection)
                    .select(idField)
                    .from(getFrom)
                    .where(matchField.eq(toMatch))
                    .fetch();

            for(Record r: idsFromDb){
                newList.add(r.get(idField));
            }
        }

        return newList;
    }

    public int getTotalCount(Table getFrom, TableField matchField, Object toMatch) throws SQLException {
        try(final Connection connection = getConnection()){
            Record1<Integer> countFromDb = getContext(connection)
                    .selectCount()
                    .from(getFrom)
                    .where(matchField.eq(toMatch))
                    .fetchOne();

            assert countFromDb != null;
            return (Integer) countFromDb.getValue(0);
        }
    }

    // TODO not checking deliverables for GP-2583. Re-instate when ready
    public int getCompletedCases(String id) throws SQLException {
        try(final Connection connection = getConnection()){
            Record1<Integer> countFromDb = getContext(connection)
                    .selectCount()
                    .from(PostgresDSL
                            .selectDistinct(QCABLE.CASE_ID)
                            .from(QCABLE)
                            .where(QCABLE.CASE_ID.in(PostgresDSL
                                    .select(DONOR_CASE.ID)
                                    .from(DONOR_CASE)
                                    .where(DONOR_CASE.PROJECT_ID.eq(id)))
                                    .and(QCABLE.QCABLE_TYPE.eq("final_report"))
                                    .and(QCABLE.STATUS.eq(DBConnector.QC_PASSED))))
                    .fetchOne();

            assert countFromDb != null;
            return (Integer) countFromDb.getValue(0);
        }
    }

    public int getCompletedQcables(String id) throws SQLException {
        try(final Connection connection = getConnection()){
            Record1<Integer> countFromDb = getContext(connection)
                    .selectCount()
                    .from(QCABLE)
                    .where(QCABLE.PROJECT_ID.eq(id)
                            .and(QCABLE.STATUS.eq(DBConnector.QC_PASSED)))
                    .fetchOne();

            assert countFromDb != null;
            return (Integer) countFromDb.getValue(0);
        }
    }

    public List<String> getFailedQCablesForProject(String id, String username) throws SQLException {
        List<String> ids = new LinkedList<>();
        Result<Record> result = fetch(PostgresDSL
                .select()
                .from(QCABLE)
                .where(QCABLE.PROJECT_ID.eq(id)
                .and(QCABLE.FAILURE_REASON.isNotNull()))
                .and(QCABLE.PROJECT_ID.in(PostgresDSL.select(USER_ACCESS.PROJECT).from(USER_ACCESS).where(USER_ACCESS.USERNAME.eq(username)))
                        .or(ADMIN_ROLE.in(PostgresDSL.select(USER_ACCESS.PROJECT).from(USER_ACCESS).where(USER_ACCESS.USERNAME.eq(username))))));

        for(Record r: result){
            ids.add(r.get(QCABLE.ID));
        }

        return ids;
    }


    public JSONObject getSankeyTransitions(String projectId, String username) throws SQLException {
        JSONObject jsonObject = new JSONObject();
        Result<Record> shouldBeSingularResult = fetch(PostgresDSL
                .select()
                .from(SANKEY_TRANSITION)
                .where(SANKEY_TRANSITION.PROJECT_ID.eq(projectId))
                .and(SANKEY_TRANSITION.PROJECT_ID.in(PostgresDSL.select(USER_ACCESS.PROJECT).from(USER_ACCESS).where(USER_ACCESS.USERNAME.eq(username)))
                .or(DBConnector.ADMIN_ROLE.in(PostgresDSL.select(USER_ACCESS.PROJECT).from(USER_ACCESS).where(USER_ACCESS.USERNAME.eq(username))))));

        if(shouldBeSingularResult.isEmpty()) return jsonObject;
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

        // LIBRARY PREPARATION -> LIBRARY QUALIFICATION
        JSONObject libPrepObject = new JSONObject();
        libPrepObject.put("total", result.get(SANKEY_TRANSITION.LIBRARY_PREPARATION_TOTAL));
        libPrepObject.put("library_qualification", result.get(SANKEY_TRANSITION.LIBRARY_PREPARATION_LIBRARY_QUALIFICATION));
        libPrepObject.put("failed", result.get(SANKEY_TRANSITION.LIBRARY_PREPARATION_FAILED));
        libPrepObject.put("pending", result.get(SANKEY_TRANSITION.LIBRARY_PREPARATION_PENDING));
        jsonObject.put("library_preparation", libPrepObject);

        // LIBRARY QUALIFICATION -> FULL DEPTH SEQUENCING
        JSONObject libQualObject = new JSONObject();
        libQualObject.put("total", result.get(SANKEY_TRANSITION.LIBRARY_QUALIFICATION_TOTAL));
        libQualObject.put("full_depth_sequencing", result.get(SANKEY_TRANSITION.LIBRARY_QUALIFICATION_FULL_DEPTH_SEQUENCING));
        libQualObject.put("failed", result.get(SANKEY_TRANSITION.LIBRARY_QUALIFICATION_FAILED));
        libQualObject.put("pending", result.get(SANKEY_TRANSITION.LIBRARY_QUALIFICATION_PENDING));
        jsonObject.put("library_qualification", libQualObject);

        // FULL DEPTH SEQUENCING -> INFORMATICS INTERPRETATION
        JSONObject fullDepthObject = new JSONObject();
        fullDepthObject.put("total", result.get(SANKEY_TRANSITION.FULL_DEPTH_SEQUENCING_TOTAL));
        fullDepthObject.put("informatics_interpretation", result.get(SANKEY_TRANSITION.FULL_DEPTH_SEQUENCING_INFORMATICS_INTERPRETATION));
        fullDepthObject.put("failed", result.get(SANKEY_TRANSITION.FULL_DEPTH_SEQUENCING_FAILED));
        fullDepthObject.put("pending", result.get(SANKEY_TRANSITION.FULL_DEPTH_SEQUENCING_PENDING));
        jsonObject.put("full_depth_sequencing", fullDepthObject);

        // INFORMATICS INTERPRETATION -> DRAFT REPORT
        JSONObject informaticsInterpretationObject = new JSONObject();
        informaticsInterpretationObject.put("total", result.get(SANKEY_TRANSITION.INFORMATICS_INTERPRETATION_TOTAL));
        informaticsInterpretationObject.put("draft_report", result.get(SANKEY_TRANSITION.INFORMATICS_INTERPRETATION_DRAFT_REPORT));
        informaticsInterpretationObject.put("failed", result.get(SANKEY_TRANSITION.INFORMATICS_INTERPRETATION_FAILED));
        informaticsInterpretationObject.put("pending", result.get(SANKEY_TRANSITION.INFORMATICS_INTERPRETATION_PENDING));
        jsonObject.put("informatics_interpretation", informaticsInterpretationObject);

        // DRAFT REPORT -> FINAL REPORT
        JSONObject draftReportObject = new JSONObject();
        draftReportObject.put("total", result.get(SANKEY_TRANSITION.DRAFT_REPORT_TOTAL));
        draftReportObject.put("final_report", result.get(SANKEY_TRANSITION.DRAFT_REPORT_FINAL_REPORT));
        draftReportObject.put("failed", result.get(SANKEY_TRANSITION.DRAFT_REPORT_FAILED));
        draftReportObject.put("pending", result.get(SANKEY_TRANSITION.DRAFT_REPORT_PENDING));
        jsonObject.put("draft_report", draftReportObject);

        // FINAL REPORT -> COMPLETION
        JSONObject finalReportObject = new JSONObject();
        finalReportObject.put("total", result.get(SANKEY_TRANSITION.FINAL_REPORT_TOTAL));
        finalReportObject.put("passed", result.get(SANKEY_TRANSITION.FINAL_REPORT_PASSED));
        finalReportObject.put("failed", result.get(SANKEY_TRANSITION.FINAL_REPORT_FAILED));
        finalReportObject.put("pending", result.get(SANKEY_TRANSITION.FINAL_REPORT_PENDING));
        jsonObject.put("final_report", finalReportObject);

        return jsonObject;
    }

    public JSONObject getCasesPerQcGate(String projectId, String username) throws SQLException {
        JSONObject jsonObject = new JSONObject();
        Result<Record> shouldBeSingularResult = fetch(PostgresDSL
            .select()
            .from(CASES_PER_QUALITY_GATE)
            .where(CASES_PER_QUALITY_GATE.PROJECT_ID.eq(projectId))
            .and(CASES_PER_QUALITY_GATE.PROJECT_ID.in(PostgresDSL.select(USER_ACCESS.PROJECT).from(USER_ACCESS).where(USER_ACCESS.USERNAME.eq(username)))
                .or(DBConnector.ADMIN_ROLE.in(PostgresDSL.select(USER_ACCESS.PROJECT).from(USER_ACCESS).where(USER_ACCESS.USERNAME.eq(username))))));

        if(shouldBeSingularResult.isEmpty()) return jsonObject;
        if(shouldBeSingularResult.size() > 1) throw new SQLException(">1 row retrieved for cases per qc gate for project ID " + projectId);
        Record result = shouldBeSingularResult.get(0);

        jsonObject.put("receipt", result.get(CASES_PER_QUALITY_GATE.RECEIPT_INSPECTION_COMPLETED_CASES));
        jsonObject.put("extraction", result.get(CASES_PER_QUALITY_GATE.EXTRACTION_COMPLETED_CASES));
        jsonObject.put("library_preparation", result.get(CASES_PER_QUALITY_GATE.LIBRARY_PREPARATION_COMPLETED_CASES));
        jsonObject.put("library_qualification", result.get(CASES_PER_QUALITY_GATE.LIBRARY_QUALIFICATION_COMPLETED_CASES));
        jsonObject.put("full_depth_sequencing", result.get(CASES_PER_QUALITY_GATE.FULL_DEPTH_SEQUENCING_COMPLETED_CASES));
        jsonObject.put("informatics_interpretation", result.get(CASES_PER_QUALITY_GATE.INFORMATICS_INTERPRETATION_COMPLETED_CASES));
        jsonObject.put("draft_report", result.get(CASES_PER_QUALITY_GATE.DRAFT_REPORT_COMPLETED_CASES));
        jsonObject.put("final_report", result.get(CASES_PER_QUALITY_GATE.FINAL_REPORT_COMPLETED_CASES));

        return jsonObject;
    }

    public static class JSONArrayMap extends HashMap<String, JSONArray>{
        @Override
        public JSONArray get(Object key) {
            if(!this.containsKey(key)){
                if(!(key instanceof String)) throw new UnsupportedOperationException("JSONArrayMap needs String for key, got " + key.getClass());
                this.put((String)key, new JSONArray());
            }
            return super.get(key);
        }

        public JSONObject toJSONObject(){
            JSONObject jsonObject = new JSONObject();
            for(String key: this.keySet()){
                jsonObject.put(key, this.get(key));
            }
            return jsonObject;
        }
    }

    public static class JSONObjectMap extends HashMap<String, JSONObject>{
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

