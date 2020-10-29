package ca.on.oicr.gsi.sampuru.server.type;

import ca.on.oicr.gsi.sampuru.server.DBConnector;
import org.jooq.Record;
import org.jooq.TableField;

import java.util.LinkedList;
import java.util.List;

import static tables_generated.Tables.*;

public class Case extends SampuruType {
    public String name;
    public List<Deliverable> deliverables = new LinkedList<>();
    public List<QCable> qcables = new LinkedList<>();
    public List<ChangelogEntry> changelog = new LinkedList<>();

    public Case(int id) throws Exception {
        getCaseFromDb(DONOR_CASE.ID, id);
    }

    public Case(String name) throws Exception {
        getCaseFromDb(DONOR_CASE.NAME, name);
    }

    public static List<Case> getAll() throws Exception {
        return getAll(DONOR_CASE, Case.class);
    }

    private void getCaseFromDb(TableField field, Object toMatch) throws Exception {
        DBConnector dbConnector = new DBConnector();
        Record dbRecord = dbConnector.getUniqueRow(field, toMatch);
        id = dbRecord.get(DONOR_CASE.ID);
        name = dbRecord.get(DONOR_CASE.NAME);

        deliverables = dbConnector.getMany(DELIVERABLE_FILE.ID, DELIVERABLE_FILE.CASE_ID, id, Deliverable.class);
        qcables = dbConnector.getMany(QCABLE.ID, QCABLE.CASE_ID, id, QCable.class);
        changelog = dbConnector.getMany(CHANGELOG.ID, CHANGELOG.CASE_ID, id, ChangelogEntry.class);
    }
}
