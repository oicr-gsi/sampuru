package ca.on.oicr.gsi.sampuru.server;

import ca.on.oicr.gsi.sampuru.server.type.Case;
import ca.on.oicr.gsi.sampuru.server.type.Project;
import ca.on.oicr.gsi.sampuru.server.type.SampuruType;
import org.jooq.Record;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

import static tables_generated.Tables.*;

// TODO: jOOQ's claim is that it closes connections automatically, no need for connection pool. But aren't there still resource issues?
// https://blog.jooq.org/tag/connection-pool/
public class DBConnector {
    private Properties properties = readProperties();
    private String userName = properties.getProperty("dbUser");
    private String pw = properties.getProperty("dbPassword");
    private String url = properties.getProperty("dbUrl");
    private Connection connection;

    public DBConnector(){
        try {
            connection = DriverManager.getConnection(url, userName, pw);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public Connection getConnection(){
        return connection;
    }

    public DSLContext getContext(){
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

    // TODO: this is just getAllIds with a WHERE clause, can we refactor this
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
                } else if (statuses.contains("Not Ready")) { // TODO: Is that the correct term? check ETL
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

    private Properties readProperties() {
        try{
            FileInputStream fis = new FileInputStream(System.getProperty("user.dir") + "/src/main/resources/sampuru.properties");
            Properties properties = new Properties();
            properties.load(fis);
            return properties;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
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

    public JSONArray buildSankeyTransitions(Project project) {
        JSONArray jsonArray = new JSONArray();
        List<String> gates = Arrays.asList(
                "receipt_inspection",
                "extraction",
                "library_preparation",
                "low_pass_sequencing",
                "full_depth_sequencing",
                "informatics_interpretation",
                "final_report");
        Map<String, String> columns = new HashMap<>();
        columns.put("receipt_inspection", "tissue_qcable");
        columns.put("extraction", "extraction_qcable");
        columns.put("library_preparation", "library_preparation_qcable");
        columns.put("low_pass_sequencing", "low_pass_sequencing_qcable");
        columns.put("full_depth_sequencing", "full_depth_sequencing_qcable");
        columns.put("informatics_interpretation", "informatics_interpretation_qcable");
        columns.put("final_report", "final_report_qcable");
        Result<Record> result = getContext()
                .select()
                .from(QCABLE_TABLE)
                .where(QCABLE_TABLE.PROJECT_ID.eq(project.id))
                .fetch();

        for(int i = 0; i < gates.size(); i++){
            //TODO: how do i actually translate this to json

        }


        return jsonArray;
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
}
