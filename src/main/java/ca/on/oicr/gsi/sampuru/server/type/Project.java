package ca.on.oicr.gsi.sampuru.server.type;

import ca.on.oicr.gsi.sampuru.server.DBConnector;
import org.jooq.Record;
import org.jooq.TableField;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

import static tables_generated.Tables.*;

public class Project extends SampuruType {
    public String name;
    public String contactName;
    public String contactEmail;
    public LocalDateTime completionDate;
    public List<ProjectInfoItem> infoItems = new LinkedList<>();
    public List<Case> donorCases = new LinkedList<>();
    public List<Deliverable> deliverables = new LinkedList<>();

    public Project(int newId) throws Exception {
        getProjectFromDb(PROJECT.ID, newId);
    }

    public Project(String newName) throws Exception {
        getProjectFromDb(PROJECT.NAME, newName);
    }

    public static List<Project> getAll() throws Exception {
        DBConnector dbConnector = new DBConnector();
        List<Integer> ids = dbConnector.getAllIds(PROJECT);
        List<Project> newList = new LinkedList<>();

        for (Integer id: ids){
            newList.add(new Project(id));
        }

        return newList;
    }

    private void getProjectFromDb(TableField field, Object toMatch) throws Exception {
        DBConnector dbConnector = new DBConnector();
        Record dbRecord = dbConnector.getUniqueRow(field, toMatch);
        id = dbRecord.getValue(PROJECT.ID);
        name = dbRecord.getValue(PROJECT.NAME);
        contactName = dbRecord.getValue(PROJECT.CONTACT_NAME);
        contactEmail = dbRecord.getValue(PROJECT.CONTACT_EMAIL);
        completionDate = dbRecord.getValue(PROJECT.COMPLETION_DATE);

        infoItems = dbConnector.getMany(PROJECT_INFO_ITEM.ID, PROJECT_INFO_ITEM.PROJECT_ID, id, ProjectInfoItem.class);
        donorCases = dbConnector.getMany(DONOR_CASE.ID, DONOR_CASE.PROJECT_ID, id, Case.class);
        deliverables = dbConnector.getMany(DELIVERABLE_FILE.ID, DELIVERABLE_FILE.PROJECT_ID, id, Deliverable.class);
    }
}
