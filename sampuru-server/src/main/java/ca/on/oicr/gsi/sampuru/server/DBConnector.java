package ca.on.oicr.gsi.sampuru.server;

import ca.on.oicr.gsi.sampuru.server.type.Case;
import ca.on.oicr.gsi.sampuru.server.type.Project;
import ca.on.oicr.gsi.sampuru.server.type.SampuruType;
import org.jooq.Record;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.postgresql.ds.PGConnectionPoolDataSource;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

import static tables_generated.Tables.*;

public class DBConnector {
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
                                .and(QCABLE.STATUS.eq("passed"))
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
                .where(QCABLE.PROJECT_ID.eq(project.id).and(QCABLE.STATUS.eq("Passed")))
                .fetch();
        return result.get(0).value1();
    }

    public JSONArray buildCaseBars(Case toExpand){
        JSONArray bars = new JSONArray();
        DSLContext context = getContext();

        // Get distinct library designs in case (each is one bar)
        // TODO: Will this get ''(blank) as a Library Design?
        Result<Record1<String>> distinctLibraryDesignsResult = context
                .selectDistinct(QCABLE.LIBRARY_DESIGN)
                .from(QCABLE)
                .where(QCABLE.CASE_ID.eq(toExpand.id))
                .fetch();
        for(Record1<String> distinctLibraryDesignRecord: distinctLibraryDesignsResult){
            JSONObject barObj = new JSONObject();
            String currentLibraryDesign = distinctLibraryDesignRecord.value1();
            barObj.put("library_design", currentLibraryDesign);

            // Get steps
            JSONArray steps = new JSONArray();
            Result<Record1<String>> distinctTypesResult = context
                    .selectDistinct(QCABLE.QCABLE_TYPE)
                    .from(QCABLE)
                    .where(QCABLE.LIBRARY_DESIGN.eq(currentLibraryDesign).and(
                            QCABLE.CASE_ID.eq(toExpand.id)
                    ))
                    .fetch();
            for(Record1<String> distinctTypeRecord: distinctTypesResult){
                JSONObject stepObj = new JSONObject();
                String currentType = distinctTypeRecord.value1();
                stepObj.put("type", currentType);

                //total count
                stepObj.put("total",
                        context.selectCount()
                            .from(QCABLE)
                            .where(QCABLE.LIBRARY_DESIGN.eq(currentLibraryDesign).and(
                                    QCABLE.CASE_ID.eq(toExpand.id)).and(
                                            QCABLE.QCABLE_TYPE.eq((currentType))
                            )).fetch().get(0).value1());

                //completed count
                stepObj.put("completed",
                        context.selectCount()
                                .from(QCABLE)
                                .where(QCABLE.LIBRARY_DESIGN.eq(currentLibraryDesign).and(
                                        QCABLE.CASE_ID.eq(toExpand.id)).and(
                                        QCABLE.QCABLE_TYPE.eq((currentType))
                                ).and(QCABLE.STATUS.eq("passed"))).fetch().get(0).value1());

                // aggregate status
                Result<Record1<String>> statusResult = context
                        .select(QCABLE.STATUS)
                        .from(QCABLE)
                        .where(QCABLE.LIBRARY_DESIGN.eq(currentLibraryDesign)
                            .and(QCABLE.CASE_ID.eq((toExpand.id)))
                            .and(QCABLE.QCABLE_TYPE.eq(currentType)))
                        .fetch();
                List<String> statuses = new LinkedList<>();
                for(Record1<String> statusRecord: statusResult){
                    statuses.add(statusRecord.value1());
                }
                String stepObjStatus = "";
                if(statuses.contains("failed")){
                    stepObjStatus = "failed";
                } else if (statuses.contains("pending")) {
                    stepObjStatus = "pending";
                } else if (statuses.contains("passed")){
                    stepObjStatus = "passed";
                } else {
                    stepObjStatus = "UNKNOWN!";
                }
                stepObj.put("status", stepObjStatus);
                steps.add(stepObj);
            }
            barObj.put("bars", steps);
            bars.add(barObj);
        }


        return bars;
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
        Integer tissuesFailedCount = context.selectCount().from(QCABLE_TABLE).where(QCABLE_TABLE.PROJECT_ID.eq(targetId).and(QCABLE_TABLE.TISSUE_QCABLE_STATUS.eq("failed"))).fetch().get(0).value1();
        receiptObject.put("failed", tissuesFailedCount);
        jsonObject.put("receipt", receiptObject);

        // EXTRACTION -> LIBRARY PREPARATION
        JSONObject extractionObject = new JSONObject();
        Integer extractionCount = context.selectCount().from(QCABLE_TABLE).where(QCABLE_TABLE.PROJECT_ID.eq(targetId).and(QCABLE_TABLE.EXTRACTION_QCABLE_ALIAS.isNotNull())).fetch().get(0).value1();
        extractionObject.put("total", extractionCount);
        Integer extractionPassedCount = context.selectCount().from(QCABLE_TABLE).where(QCABLE_TABLE.PROJECT_ID.eq(targetId).and(QCABLE_TABLE.LIBRARY_PREPARATION_QCABLE_ALIAS.isNotNull())).fetch().get(0).value1();
        extractionObject.put("library_preparation", extractionPassedCount);
        Integer extractionFailedCount = context.selectCount().from(QCABLE_TABLE).where(QCABLE_TABLE.PROJECT_ID.eq(targetId).and(QCABLE_TABLE.EXTRACTION_QCABLE_STATUS.eq("failed"))).fetch().get(0).value1();
        extractionObject.put("failed", extractionFailedCount);
        Integer extractionPendingCount = context.selectCount().from(QCABLE_TABLE).where(QCABLE_TABLE.PROJECT_ID.eq(targetId).and(QCABLE_TABLE.EXTRACTION_QCABLE_STATUS.eq("pending"))).fetch().get(0).value1();
        extractionObject.put("pending", extractionPendingCount);
        jsonObject.put("extraction", extractionObject);

        // LIBRARY PREPARATION -> LOW PASS SEQUENCING
        JSONObject libPrepObject = new JSONObject();
        Integer libPrepCount = context.selectCount().from(QCABLE_TABLE).where(QCABLE_TABLE.PROJECT_ID.eq(targetId).and(QCABLE_TABLE.LIBRARY_PREPARATION_QCABLE_ALIAS.isNotNull())).fetch().get(0).value1();
        libPrepObject.put("total", libPrepCount);
        Integer libPrepPassedCount = context.selectCount().from(QCABLE_TABLE).where(QCABLE_TABLE.PROJECT_ID.eq(targetId).and(QCABLE_TABLE.LOW_PASS_SEQUENCING_QCABLE_ALIAS.isNotNull())).fetch().get(0).value1();
        libPrepObject.put("low_pass_sequencing", libPrepPassedCount);
        Integer libPrepFailedCount = context.selectCount().from(QCABLE_TABLE).where(QCABLE_TABLE.PROJECT_ID.eq(targetId).and(QCABLE_TABLE.LIBRARY_PREPARATION_QCABLE_STATUS.eq("failed"))).fetch().get(0).value1();
        libPrepObject.put("failed", libPrepFailedCount);
        Integer libPrepPendingCount = context.selectCount().from(QCABLE_TABLE).where(QCABLE_TABLE.PROJECT_ID.eq(targetId).and(QCABLE_TABLE.LIBRARY_PREPARATION_QCABLE_STATUS.eq("pending"))).fetch().get(0).value1();
        libPrepObject.put("pending", libPrepPendingCount);
        jsonObject.put("library_preparation", libPrepObject);

        // LOW PASS SEQUENCING -> FULL DEPTH SEQUENCING
        JSONObject lowPassObject = new JSONObject();
        Integer lowPassCount = context.selectCount().from(QCABLE_TABLE).where(QCABLE_TABLE.PROJECT_ID.eq(targetId).and(QCABLE_TABLE.LOW_PASS_SEQUENCING_QCABLE_ALIAS.isNotNull())).fetch().get(0).value1();
        lowPassObject.put("total", lowPassCount);
        Integer lowPassPassedCount = context.selectCount().from(QCABLE_TABLE).where(QCABLE_TABLE.PROJECT_ID.eq(targetId).and(QCABLE_TABLE.FULL_DEPTH_SEQUENCING_QCABLE_ALIAS.isNotNull())).fetch().get(0).value1();
        lowPassObject.put("full_depth_sequencing", lowPassPassedCount);
        Integer lowPassFailedCount = context.selectCount().from(QCABLE_TABLE).where(QCABLE_TABLE.PROJECT_ID.eq(targetId).and(QCABLE_TABLE.LOW_PASS_SEQUENCING_QCABLE_STATUS.eq("failed"))).fetch().get(0).value1();
        lowPassObject.put("failed", lowPassFailedCount);
        Integer lowPassPendingCount = context.selectCount().from(QCABLE_TABLE).where(QCABLE_TABLE.PROJECT_ID.eq(targetId).and(QCABLE_TABLE.LOW_PASS_SEQUENCING_QCABLE_STATUS.eq("pending"))).fetch().get(0).value1();
        lowPassObject.put("pending", lowPassPendingCount);
        jsonObject.put("lowPass", lowPassObject);

        // FULL DEPTH SEQUENCING -> INFORMATICS INTERPRETATION
        JSONObject fullDepthObject = new JSONObject();
        Integer fullDepthCount = context.selectCount().from(QCABLE_TABLE).where(QCABLE_TABLE.PROJECT_ID.eq(targetId).and(QCABLE_TABLE.FULL_DEPTH_SEQUENCING_QCABLE_ALIAS.isNotNull())).fetch().get(0).value1();
        fullDepthObject.put("total", fullDepthCount);
        Integer fullDepthPassedCount = context.selectCount().from(QCABLE_TABLE).where(QCABLE_TABLE.PROJECT_ID.eq(targetId).and(QCABLE_TABLE.INFORMATICS_INTERPRETATION_QCABLE_ALIAS.isNotNull())).fetch().get(0).value1();
        fullDepthObject.put("informatics_interpretation", fullDepthPassedCount);
        Integer fullDepthFailedCount = context.selectCount().from(QCABLE_TABLE).where(QCABLE_TABLE.PROJECT_ID.eq(targetId).and(QCABLE_TABLE.FULL_DEPTH_SEQUENCING_QCABLE_STATUS.eq("failed"))).fetch().get(0).value1();
        fullDepthObject.put("failed", fullDepthFailedCount);
        Integer fullDepthPendingCount = context.selectCount().from(QCABLE_TABLE).where(QCABLE_TABLE.PROJECT_ID.eq(targetId).and(QCABLE_TABLE.FULL_DEPTH_SEQUENCING_QCABLE_STATUS.eq("pending"))).fetch().get(0).value1();
        fullDepthObject.put("pending", fullDepthPendingCount);
        jsonObject.put("fullDepth", fullDepthObject);

        // INFORMATICS INTERPRETATION -> FINAL REPORT
        JSONObject informaticsInterpretationObject = new JSONObject();
        Integer informaticsInterpretationCount = context.selectCount().from(QCABLE_TABLE).where(QCABLE_TABLE.PROJECT_ID.eq(targetId).and(QCABLE_TABLE.INFORMATICS_INTERPRETATION_QCABLE_ALIAS.isNotNull())).fetch().get(0).value1();
        informaticsInterpretationObject.put("total", informaticsInterpretationCount);
        Integer informaticsInterpretationPassedCount = context.selectCount().from(QCABLE_TABLE).where(QCABLE_TABLE.PROJECT_ID.eq(targetId).and(QCABLE_TABLE.FINAL_REPORT_QCABLE_ALIAS.isNotNull())).fetch().get(0).value1();
        informaticsInterpretationObject.put("informaticsInterpretation", informaticsInterpretationPassedCount);
        Integer informaticsInterpretationFailedCount = context.selectCount().from(QCABLE_TABLE).where(QCABLE_TABLE.PROJECT_ID.eq(targetId).and(QCABLE_TABLE.INFORMATICS_INTERPRETATION_QCABLE_STATUS.eq("failed"))).fetch().get(0).value1();
        informaticsInterpretationObject.put("failed", informaticsInterpretationFailedCount);
        Integer informaticsInterpretationPendingCount = context.selectCount().from(QCABLE_TABLE).where(QCABLE_TABLE.PROJECT_ID.eq(targetId).and(QCABLE_TABLE.INFORMATICS_INTERPRETATION_QCABLE_STATUS.eq("pending"))).fetch().get(0).value1();
        informaticsInterpretationObject.put("pending", informaticsInterpretationPendingCount);
        jsonObject.put("informaticsInterpretation", informaticsInterpretationObject);

        // FINAL REPORT -> COMPLETION
        JSONObject finalReportObject = new JSONObject();
        Integer finalReportCount = context.selectCount().from(QCABLE_TABLE).where(QCABLE_TABLE.PROJECT_ID.eq(targetId).and(QCABLE_TABLE.FINAL_REPORT_QCABLE_ALIAS.isNotNull())).fetch().get(0).value1();
        finalReportObject.put("total", finalReportCount);
        Integer finalReportPassedCount = context.selectCount().from(QCABLE_TABLE).where(QCABLE_TABLE.PROJECT_ID.eq(targetId).and(QCABLE_TABLE.FINAL_REPORT_QCABLE_STATUS.eq("passed"))).fetch().get(0).value1();
        finalReportObject.put("passed", finalReportPassedCount);
        Integer finalReportFailedCount = context.selectCount().from(QCABLE_TABLE).where(QCABLE_TABLE.PROJECT_ID.eq(targetId).and(QCABLE_TABLE.FINAL_REPORT_QCABLE_STATUS.eq("failed"))).fetch().get(0).value1();
        finalReportObject.put("failed", finalReportFailedCount);
        Integer finalReportPendingCount = context.selectCount().from(QCABLE_TABLE).where(QCABLE_TABLE.PROJECT_ID.eq(targetId).and(QCABLE_TABLE.FINAL_REPORT_QCABLE_STATUS.eq("pending"))).fetch().get(0).value1();
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
}
