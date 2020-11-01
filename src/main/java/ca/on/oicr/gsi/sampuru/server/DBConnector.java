package ca.on.oicr.gsi.sampuru.server;

import ca.on.oicr.gsi.sampuru.server.type.Case;
import ca.on.oicr.gsi.sampuru.server.type.Project;
import ca.on.oicr.gsi.sampuru.server.type.QCable;
import ca.on.oicr.gsi.sampuru.server.type.SampuruType;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import static tables_generated.Tables.*;

// TODO: Later, let's not just have 1 connection, but a pool of them. Methods would probably be static.
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
        Result<Record1<Integer>> result = getContext()
                .selectCount()
                .from(DONOR_CASE)
                .where(DONOR_CASE.PROJECT_ID.eq(project.id).and(true)) //TODO oh god how do i calculate whether it's complete
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
                                ).and(QCABLE.STATUS.eq("Passed"))).fetch().get(0).value1());

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
                if(statuses.contains("Failed")){
                    stepObjStatus = "Failed";
                } else if (statuses.contains("Not Ready")) { // TODO: Is that the correct term? check ETL
                    stepObjStatus = "Pending";
                } else if (statuses.contains("Passed")){
                    stepObjStatus = "Passed";
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
        DSLContext context = getContext();
        Result<Record1<LocalDateTime>> result = context
                .selectDistinct(CHANGELOG.CHANGE_DATE)
                .from(CHANGELOG)
                .where(
                        CHANGELOG.CASE_ID.in(
                                context.select(DONOR_CASE.ID)
                                        .from(DONOR_CASE)
                                        .where(DONOR_CASE.PROJECT_ID.eq(project.id))))
                .orderBy(CHANGELOG.CHANGE_DATE.desc())
                .fetch();
        return result.get(0).value1();
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
}
