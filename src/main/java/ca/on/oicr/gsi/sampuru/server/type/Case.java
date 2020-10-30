package ca.on.oicr.gsi.sampuru.server.type;

import ca.on.oicr.gsi.sampuru.server.DBConnector;
import org.jooq.Record;
import org.jooq.TableField;

import java.util.LinkedList;
import java.util.List;

import static tables_generated.Tables.*;

public class Case extends SampuruType {
    public String name;
    public List<Integer> deliverables = new LinkedList<>();
    public List<Integer> qcables = new LinkedList<>();
    public List<Integer> changelog = new LinkedList<>();

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

        deliverables = dbConnector.getChildIdList(DELIVERABLE_FILE, DELIVERABLE_FILE.CASE_ID, id);
        qcables = dbConnector.getChildIdList(QCABLE, QCABLE.CASE_ID, id);
        changelog = dbConnector.getChildIdList(CHANGELOG, CHANGELOG.CASE_ID, id);
    }

    @Override
    public String toString(){
        return "Case id: " + id
                + "\n name: " + name
                + "\n deliverables: " + deliverables
                + "\n qcables: " + qcables
                + "\n changelog: " + changelog;
    }
}
