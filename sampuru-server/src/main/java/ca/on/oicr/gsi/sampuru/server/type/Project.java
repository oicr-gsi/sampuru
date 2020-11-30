package ca.on.oicr.gsi.sampuru.server.type;

import ca.on.oicr.gsi.sampuru.server.DBConnector;
import ca.on.oicr.gsi.sampuru.server.service.CaseService;
import org.jooq.Record;
import org.jooq.TableField;

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

    public Project(String newId) throws Exception {
        getProjectFromDb(PROJECT.ID, newId);
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
        return new CaseService().getForProject(this.id);
    }

    public List<Deliverable> getDeliverables() throws Exception {
        List<Deliverable> deliverableList = new LinkedList<>();
        for (Integer i: deliverables){
            deliverableList.add(new Deliverable(i));
        }
        return deliverableList;
    }

    private void getProjectFromDb(TableField field, Object toMatch) throws Exception {
        DBConnector dbConnector = new DBConnector();
        Record dbRecord = dbConnector.getUniqueRow(field, toMatch);
        id = dbRecord.get(PROJECT.ID);
        name = dbRecord.get(PROJECT.NAME);
        contactName = dbRecord.get(PROJECT.CONTACT_NAME);
        contactEmail = dbRecord.get(PROJECT.CONTACT_EMAIL);
        completionDate = dbRecord.get(PROJECT.COMPLETION_DATE);

        infoItems = dbConnector.getChildIdList(PROJECT_INFO_ITEM, PROJECT_INFO_ITEM.PROJECT_ID, id).stream().map(o -> (Integer)o).collect(Collectors.toList());;
        donorCases = dbConnector.getChildIdList(DONOR_CASE, DONOR_CASE.PROJECT_ID, id).stream().map(o -> (String)o).collect(Collectors.toList());;
        deliverables = dbConnector.getChildIdList(DELIVERABLE_FILE, DELIVERABLE_FILE.PROJECT_ID, id).stream().map(o -> (Integer)o).collect(Collectors.toList());;
    }

    public List<QCable> getFailedQCables() throws Exception {
        List<String> failedQCablesIds = new DBConnector().getFailedQCablesForProject(this.id);
        List<QCable> failedQCables = new LinkedList<>();

        for(String failureId: failedQCablesIds){
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
