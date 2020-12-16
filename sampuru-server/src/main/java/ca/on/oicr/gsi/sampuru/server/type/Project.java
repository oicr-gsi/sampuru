package ca.on.oicr.gsi.sampuru.server.type;

import ca.on.oicr.gsi.sampuru.server.DBConnector;
import ca.on.oicr.gsi.sampuru.server.service.CaseService;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.TableField;
import org.jooq.util.postgres.PostgresDSL;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static tables_generated.Tables.*;

public class Project extends SampuruType {
    public static final String INFO_ITEM_IDS = "info_item_ids",
            CASE_IDS = "case_ids",
            DELIVERABLE_IDS = "deliverable_ids",
            CASES_TOTAL = "cases_total",
            CASES_COMPLETED = "cases_completed",
            QCABLES_TOTAL = "qcables_total",
            QCABLES_COMPLETED = "qcables_completed";

    public String id, name, contactName, contactEmail;
    public LocalDateTime completionDate, lastUpdate;
    public List<Integer> infoItems = new LinkedList<>(), deliverables = new LinkedList<>();
    public List<String> donorCases = new LinkedList<>();
    public int casesTotal, casesCompleted, qcablesTotal, qcablesCompleted;

    public Project(String newId, String username) throws Exception {
        getProjectFromDb(PROJECT.ID, newId, username);
    }

    public Project(Record row) {
        id = row.get(PROJECT.ID);
        name = row.get(PROJECT.NAME);
        contactName = row.get(PROJECT.CONTACT_NAME);
        contactEmail = row.get(PROJECT.CONTACT_EMAIL);
        completionDate = row.get(PROJECT.COMPLETION_DATE);
        infoItems = row.get(INFO_ITEM_IDS, List.class);
        donorCases = row.get(CASE_IDS, List.class);
        deliverables = row.get(DELIVERABLE_IDS, List.class);
        casesTotal = row.get(CASES_TOTAL, Integer.class);
        casesCompleted = row.get(CASES_COMPLETED, Integer.class);
        qcablesTotal = row.get(QCABLES_TOTAL, Integer.class);
        qcablesCompleted = row.get(QCABLES_COMPLETED, Integer.class);
    }

    public static List<Project> getAll(String username) throws Exception {
        return getAll(PROJECT, Project.class, username);
    }

    public List<Case> getCases(String username) {
        List<Case> cases = new LinkedList<>();
        DBConnector dbConnector = new DBConnector();
        Result<Record> results = dbConnector.execute(PostgresDSL
                .select()
                .from(DONOR_CASE)
                .where(DONOR_CASE.PROJECT_ID.in(donorCases)
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

    public List<ProjectInfoItem> getInfoItems(String username) {
        List<ProjectInfoItem> projectInfoItems = new LinkedList<>();
        DBConnector dbConnector = new DBConnector();
        Result<Record> results = dbConnector.execute(PostgresDSL
                .select()
                .from(PROJECT_INFO_ITEM)
                .where(PROJECT_INFO_ITEM.ID.in(infoItems)
                        .and(PROJECT_INFO_ITEM.PROJECT_ID.in(PostgresDSL
                                .select(USER_ACCESS.PROJECT)
                                .from(USER_ACCESS)
                                .where(USER_ACCESS.USERNAME.eq(username)))
                        .or(DBConnector.ADMIN_ROLE.in(PostgresDSL
                                .select(USER_ACCESS.PROJECT)
                                .from(USER_ACCESS)
                                .where(USER_ACCESS.USERNAME.eq(username)))))));

        for(Record result: results){
            projectInfoItems.add(new ProjectInfoItem(result));
        }

        return projectInfoItems;
    }

    public List<Deliverable> getDeliverables(String username) {
        List<Deliverable> deliverableList = new LinkedList<>();
        DBConnector dbConnector = new DBConnector();
        Result<Record> results = dbConnector.execute(PostgresDSL
                .select()
                .from(DELIVERABLE_FILE)
                .where(DELIVERABLE_FILE.PROJECT_ID.eq(this.id)
                        .and(DELIVERABLE_FILE.PROJECT_ID.in(PostgresDSL
                                .select(USER_ACCESS.PROJECT)
                                .from(USER_ACCESS)
                                .where(USER_ACCESS.USERNAME.eq(username)))
                        .or(DBConnector.ADMIN_ROLE.in(PostgresDSL
                                .select(USER_ACCESS.PROJECT)
                                .from(USER_ACCESS)
                                .where(USER_ACCESS.USERNAME.eq(username)))))));

        for(Record result: results){
            deliverableList.add(new Deliverable(result));
        }

        return deliverableList;
    }

    private void getProjectFromDb(TableField field, Object toMatch, String username) throws Exception {
        DBConnector dbConnector = new DBConnector();

        Result<Record> results = dbConnector.execute(PostgresDSL
                .select()
                .from(PROJECT)
                .where(PROJECT.ID.eq((String)toMatch)
                        .and(PROJECT.ID.in(PostgresDSL
                                .select(USER_ACCESS.PROJECT)
                                .from(USER_ACCESS)
                                .where(USER_ACCESS.USERNAME.eq(username)))
                        .or(DBConnector.ADMIN_ROLE.in(PostgresDSL
                                .select(USER_ACCESS.PROJECT)
                                .from(USER_ACCESS)
                                .where(USER_ACCESS.USERNAME.eq(username)))))));

        if(results.isEmpty()){
            throw new Exception("Project does not exist or no permission"); // TODO: more precise exception
        } else if (results.size() > 1){
            throw new Exception("Found >1 record for Project identifier " + toMatch); // TODO: more precise exception
        }

        Record dbRecord = results.get(0);

        id = dbRecord.get(PROJECT.ID);
        name = dbRecord.get(PROJECT.NAME);
        contactName = dbRecord.get(PROJECT.CONTACT_NAME);
        contactEmail = dbRecord.get(PROJECT.CONTACT_EMAIL);
        completionDate = dbRecord.get(PROJECT.COMPLETION_DATE);

        infoItems = dbConnector.getChildIdList(PROJECT_INFO_ITEM, PROJECT_INFO_ITEM.PROJECT_ID, id).stream().map(o -> (Integer)o).collect(Collectors.toList());;
        donorCases = dbConnector.getChildIdList(DONOR_CASE, DONOR_CASE.PROJECT_ID, id).stream().map(o -> (String)o).collect(Collectors.toList());;
        deliverables = dbConnector.getChildIdList(DELIVERABLE_FILE, DELIVERABLE_FILE.PROJECT_ID, id).stream().map(o -> (Integer)o).collect(Collectors.toList());;
    }

    public List<QCable> getFailedQCables(String username) throws Exception {
        List<String> failedQCablesIds = new DBConnector().getFailedQCablesForProject(this.id, username);
        List<QCable> failedQCables = new LinkedList<>();

        for(String failureId: failedQCablesIds){
            failedQCables.add(new QCable(failureId, username));
        }

        return failedQCables;
    }

    @Override
    public String toString(){
        return "Project id: " + id
                + "\n name: " + name
                + "\n contactName: " + contactName
                + "\n contactEmail: " + contactEmail
                + "\n completionDate: " + completionDate
                + "\n infoItems: " + infoItems
                + "\n donorCases: " + donorCases
                + "\n deliverables: " + deliverables + "\n";
    }
}
