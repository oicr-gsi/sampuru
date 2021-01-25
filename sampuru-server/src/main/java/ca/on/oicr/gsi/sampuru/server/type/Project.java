package ca.on.oicr.gsi.sampuru.server.type;

import ca.on.oicr.gsi.sampuru.server.DBConnector;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.TableField;
import org.jooq.util.postgres.PostgresDSL;

import java.time.LocalDateTime;
import java.util.Arrays;
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

    public String id, name, contactName, contactEmail, description, pipeline, referenceGenome;
    public LocalDateTime createdDate, completionDate, lastUpdate;
    public List<Integer> infoItems = new LinkedList<>(), deliverables = new LinkedList<>();
    public List<String> donorCases = new LinkedList<>(), kits = new LinkedList<>();
    public int casesTotal, casesCompleted, qcablesTotal, qcablesCompleted;

    public Project(String newId, String username) throws Exception {
        getProjectFromDb(PROJECT.ID, newId, username);
    }

    public Project(Record row) {
        id = row.get(PROJECT.ID);
        name = row.get(PROJECT.NAME);
        description = row.get(PROJECT.DESCRIPTION);
        pipeline = row.get(PROJECT.PIPELINE);
        referenceGenome = row.get(PROJECT.REFERENCE_GENOME);
        contactName = row.get(PROJECT.CONTACT_NAME);
        contactEmail = row.get(PROJECT.CONTACT_EMAIL);
        createdDate = row.get(PROJECT.CREATED_DATE);
        completionDate = row.get(PROJECT.COMPLETION_DATE);
        kits = row.get(PROJECT.KITS, List.class);
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
        Result<Record> results = dbConnector.fetch(PostgresDSL
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
        Result<Record> results = dbConnector.fetch(PostgresDSL
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
        Result<Record> results = dbConnector.fetch(PostgresDSL
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

        Result<Record> results = dbConnector.fetch(PostgresDSL
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
        createdDate = dbRecord.get(PROJECT.CREATED_DATE);
        completionDate = dbRecord.get(PROJECT.COMPLETION_DATE);
        description = dbRecord.get(PROJECT.DESCRIPTION);
        pipeline = dbRecord.get(PROJECT.PIPELINE);
        referenceGenome = dbRecord.get(PROJECT.REFERENCE_GENOME);
        kits = dbRecord.get(PROJECT.KITS) != null ?
                Arrays.stream(dbRecord.get(PROJECT.KITS)).collect(Collectors.toList()) : new LinkedList<>();

        casesTotal = dbConnector.getTotalCount(DONOR_CASE, DONOR_CASE.PROJECT_ID, id);
        casesCompleted = dbConnector.getCompletedCases(id);
        qcablesTotal = dbConnector.getTotalCount(QCABLE, QCABLE.PROJECT_ID, id);
        qcablesCompleted = dbConnector.getCompletedQcables(id);
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
                + "\n description: " + description
                + "\n pipeline: " + pipeline
                + "\n kits: " + kits
                + "\n referenceGenome: " + referenceGenome
                + "\n createdDate: " + createdDate
                + "\n completionDate: " + completionDate
                + "\n infoItems: " + infoItems
                + "\n donorCases: " + donorCases
                + "\n deliverables: " + deliverables + "\n";
    }
}
