package ca.on.oicr.gsi.sampuru.server.type;

import ca.on.oicr.gsi.sampuru.server.DBConnector;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.TableField;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

import static tables_generated.Tables.*;

public class Project extends SampuruType {
    public static final String INFO_ITEM_IDS = "info_item_ids";
    public static final String CASE_IDS = "case_ids";
    public static final String DELIVERABLE_IDS = "deliverable_ids";
    public String name;
    public String contactName;
    public String contactEmail;
    public LocalDateTime completionDate;
    public List<Integer> infoItems = new LinkedList<>();
    public List<Integer> donorCases = new LinkedList<>();
    public List<Integer> deliverables = new LinkedList<>();

    public Project(int newId) throws Exception {
        getProjectFromDb(PROJECT.ID, newId);
    }

    public Project(String newName) throws Exception {
        getProjectFromDb(PROJECT.NAME, newName);
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
    }

    public static List<Project> getAll() throws Exception {
        return getAll(PROJECT, Project.class);
    }

    public List<ProjectInfoItem> getInfoItems() throws Exception {
        List<ProjectInfoItem> projectInfoItems = new LinkedList<>();
        for(Integer i: infoItems){
            projectInfoItems.add(new ProjectInfoItem(i));
        }
        return projectInfoItems;
    }

    public List<Case> getCases() throws Exception {
        List<Case> caseList = new LinkedList<>();
        for (Integer i: donorCases){
            caseList.add(new Case(i));
        }
        return caseList;
    }

    public List<Deliverable> getDeliverables() throws Exception {
        List<Deliverable> deliverableList = new LinkedList<>();
        for (Integer i: deliverables){
            deliverableList.add(new Deliverable(i));
        }
        return deliverableList;
    }

    public Integer getCasesTotal(){
        return new DBConnector().getCasesTotal(this);
    }

    public Integer getCasesCompleted(){
        return new DBConnector().getCasesCompleted(this);
    }

    public Integer getQCablesTotal(){
        return new DBConnector().getQCablesTotal(this);
    }

    public Integer getQCablesCompleted(){
        return new DBConnector().getQCablesCompleted(this);
    }

    public LocalDateTime getLastUpdate(){
        return new DBConnector().getLastUpdate(this);
    }

    private void getProjectFromDb(TableField field, Object toMatch) throws Exception {
        DBConnector dbConnector = new DBConnector();
        Record dbRecord = dbConnector.getUniqueRow(field, toMatch);
        id = dbRecord.get(PROJECT.ID);
        name = dbRecord.get(PROJECT.NAME);
        contactName = dbRecord.get(PROJECT.CONTACT_NAME);
        contactEmail = dbRecord.get(PROJECT.CONTACT_EMAIL);
        completionDate = dbRecord.get(PROJECT.COMPLETION_DATE);

        infoItems = dbConnector.getChildIdList(PROJECT_INFO_ITEM, PROJECT_INFO_ITEM.PROJECT_ID, id);
        donorCases = dbConnector.getChildIdList(DONOR_CASE, DONOR_CASE.PROJECT_ID, id);
        deliverables = dbConnector.getChildIdList(DELIVERABLE_FILE, DELIVERABLE_FILE.PROJECT_ID, id);
    }

    public List<QCable> getFailedQCables() throws Exception {
        List<Integer> failedQCablesIds = new DBConnector().getFailedQCablesForProject(this.id);
        List<QCable> failedQCables = new LinkedList<>();

        for(Integer failureId: failedQCablesIds){
            failedQCables.add(new QCable(failureId));
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
