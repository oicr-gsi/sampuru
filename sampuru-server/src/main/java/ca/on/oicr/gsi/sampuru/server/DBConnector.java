package ca.on.oicr.gsi.sampuru.server;

import org.jooq.Record;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.jooq.util.postgres.PostgresDSL;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.postgresql.ds.PGConnectionPoolDataSource;

import java.sql.Connection;
import java.sql.SQLException;
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
    private static PGConnectionPoolDataSource pgDataSource;

    public DBConnector() {
        if (pgDataSource == null) {
            PGConnectionPoolDataSource pgDataSource = new PGConnectionPoolDataSource();
            pgDataSource.setUrl(url);
            pgDataSource.setUser(dbUser);
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

    private DSLContext getContext() {
        return getContext(getConnection());
    }

    private DSLContext getContext(Connection connection){
        return DSL.using(connection, SQLDialect.POSTGRES);
    }

    //TODO: I wanted this as a catch all to do a filter by username but it doesn't look like al's gonna be so lucky
    public Result fetch(SelectConnectByStep<Record> select){
        return getContext().fetch(select);
    }

    //TODO: filter by username, probably by removing me
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

    // TODO: look at https://www.jooq.org/doc/3.14/manual/sql-building/dynamic-sql/
    public void writeDeliverables(JSONArray deliverableArray, String username) throws Exception {
        if (deliverableArray.isEmpty()) return;

        // Front end will provide ID if deliverable is pre-existing. If no ID or ID is null, INSERT instead of UPDATE
        // Currently, nothing is being done with knownDeliverables. To be implemented (GP-2520)
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

        //TODO: Adding Collection of VALUEs to Insert is coming in jOOQ 3.15 apparently. They disapprove of this approach
        if(!unknownDeliverables.isEmpty()) {
            InsertSetStep deliverableInsertSetStep = getContext().insertInto(DELIVERABLE_FILE),
                    deliverableCaseInsertSetStep = getContext().insertInto(DELIVERABLE_CASE);
            InsertValuesStepN deliverableInsertValuesStepN = null,
                    deliverableCaseInsertValuesStepN = null;
            for(Object obj: unknownDeliverables){
                JSONObject nextDeliverable = (JSONObject) obj;
                deliverableInsertValuesStepN = deliverableInsertSetStep.values(
                        PostgresDSL.defaultValue(), // ID. DEFAULT can't be used in an expression, only as a replacement for an expression. The other not-nulls will still kill bad requests
                        checkWritePermission(nextDeliverable.get("project_id"), username),
                        checkWritePermission(nextDeliverable.get("location"), username),
                        checkWritePermission(nextDeliverable.get("notes"), username),
                        checkWritePermission(PostgresDSL.localDateTime(nextDeliverable.get("expiry_date").toString()), username)
                );

            }
            // Write the new deliverables to the database and get back the new SERIAL ids
            Result<Record> ids = deliverableInsertValuesStepN.returningResult(DELIVERABLE_FILE.ID).fetch();

            // Associate the new ids with the appropriate case ids and write to deliverables_case table
            for(int i = 0; i < ids.size(); i++){
                Integer thisId = Integer.valueOf(ids.get(i).get("id").toString());
                JSONArray casesForId = ((JSONArray) ((JSONObject)unknownDeliverables.get(i)).get("case_id"));
                for(Object obj: casesForId){
                    String strObject = (String) obj;
                    deliverableCaseInsertValuesStepN = deliverableCaseInsertSetStep.values(
                            checkWritePermission(thisId, username),
                            checkWritePermission(strObject, username)
                    );
                }
            }
            deliverableCaseInsertValuesStepN.execute();
        }
    }

    private Object checkWritePermission(Object value, String username){
        return PostgresDSL.when(ADMIN_ROLE.in(PostgresDSL.select(USER_ACCESS.PROJECT).from(USER_ACCESS).where(USER_ACCESS.USERNAME.eq(username))), value);
    }

    //TODO: filter by username, probably by rewrite
    public List<Integer> getAllIds(Table getFrom){
        List<Integer> newList = new LinkedList<>();
        Field<Integer> idField = getFrom.field("id");

        Result<Record1<Integer>> idsFromDb = getContext().select(idField).from(getFrom).fetch();

        for(Record r: idsFromDb){
            newList.add(r.get(idField));
        }

        return newList;
    }

    //TODO: filter by username?? maybe?? probably by rewrite
    public List<Object> getChildIdList(Table getFrom, TableField matchField, Object toMatch){
        return getChildIdList(getFrom, matchField, toMatch, getFrom.field("id"));
    }

    public List<Object> getChildIdList(Table getFrom, TableField matchField, Object toMatch, Field idField){
        List<Object> newList = new LinkedList<>();

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

    public int getTotalCount(Table getFrom, TableField matchField, Object toMatch) {
        Record1<Integer> countFromDb = getContext()
                .selectCount()
                .from(getFrom)
                .where(matchField.eq(toMatch))
                .fetchOne();

        assert countFromDb != null;
        return (Integer) countFromDb.getValue(0);
    }

    // TODO not checking deliverables for GP-2583. Re-instate when ready
    public int getCompletedCases(String id) {
        Record1<Integer> countFromDb = getContext()
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

    public int getCompletedQcables(String id) {
        Record1<Integer> countFromDb = getContext()
                .selectCount()
                .from(QCABLE)
                .where(QCABLE.PROJECT_ID.eq(id)
                        .and(QCABLE.STATUS.eq(DBConnector.QC_PASSED)))
                .fetchOne();

        assert countFromDb != null;
        return (Integer) countFromDb.getValue(0);
    }

    public List<String> getFailedQCablesForProject(String id, String username) {
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

