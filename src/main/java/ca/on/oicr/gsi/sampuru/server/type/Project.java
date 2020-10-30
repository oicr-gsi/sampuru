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
    public List<Integer> infoItems = new LinkedList<>();
    public List<Integer> donorCases = new LinkedList<>();
    public List<Integer> deliverables = new LinkedList<>();

    public Project(int newId) throws Exception {
        getProjectFromDb(PROJECT.ID, newId);
    }

    public Project(String newName) throws Exception {
        getProjectFromDb(PROJECT.NAME, newName);
    }

    public static List<Project> getAll() throws Exception {
        return getAll(PROJECT, Project.class);
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

    @Override
    public String toString(){
        return "Project id: " + id
                + "\n name: " + name
                + "\n contactName: " + contactName
                + "\n contactEmail: " + contactEmail
                + "\n completionDate: " + completionDate
                + "\n infoItems: " + infoItems
                + "\n donorCases: " + donorCases
                + "\n deliverables: " + deliverables;
    }
}
