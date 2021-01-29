package ca.on.oicr.gsi.sampuru.server.type;

import ca.on.oicr.gsi.sampuru.server.DBConnector;
import org.jooq.Record;
import org.jooq.TableField;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static tables_generated.Tables.*;

public class Case extends SampuruType {
    public String id;
    public static final String DELIVERABLE_IDS = "deliverable_ids";
    public static final String QCABLE_IDS = "qcable_ids";
    public static final String CHANGELOG_IDS = "changelog_ids";
    public String name;
    public List<Integer> deliverables;
    public List<String> qcables;
    public List<Integer> changelog;

    public Case(String newId, String username) throws Exception {
        getCaseFromDb(DONOR_CASE.ID, newId, username);
    }

    public Case(Record row) {
        id = row.get(DONOR_CASE.ID);
        name = row.get(DONOR_CASE.NAME);

        deliverables = row.get(DELIVERABLE_IDS, List.class);
        qcables = row.get(QCABLE_IDS, List.class);
        changelog = row.get(CHANGELOG_IDS, List.class);
    }

    public static List<Case> getAll(String username) throws Exception {
        return getAll(DONOR_CASE, Case.class, username);
    }

    public List<QCable> getQcables(String username) throws Exception {
        List<QCable> newList = new LinkedList<>();
        for (String qcId: qcables){
            newList.add(new QCable(qcId, username));
        }
        return newList;
    }

    public List<Deliverable> getDeliverables(String username) throws Exception {
        List<Deliverable> newList = new LinkedList<>();
        for (Integer deliverableId: deliverables){
            newList.add(new Deliverable(deliverableId, username));
        }
        return newList;
    }

    public List<ChangelogEntry> getChangelog(String username) throws Exception {
        List<ChangelogEntry> newList = new LinkedList<>();
        for(Integer changelogId: changelog){
            newList.add(new ChangelogEntry(changelogId, username));
        }
        return newList;
    }

    private void getCaseFromDb(TableField field, Object toMatch, String username) throws Exception {
        DBConnector dbConnector = new DBConnector();
        Record dbRecord = dbConnector.getUniqueRow(field, toMatch);
        id = dbRecord.get(DONOR_CASE.ID);
        name = dbRecord.get(DONOR_CASE.NAME);

        changelog = dbConnector.getChildIdList(CHANGELOG, CHANGELOG.CASE_ID, id).stream().map(o -> (Integer)o).collect(Collectors.toList());
        qcables = dbConnector.getChildIdList(QCABLE, QCABLE.CASE_ID, id).stream().map(o -> (String)o).collect(Collectors.toList());;
        deliverables = dbConnector.getChildIdList(DELIVERABLE_CASE, DELIVERABLE_CASE.CASE_ID, id, DELIVERABLE_CASE.DELIVERABLE_ID).stream().map(o -> (Integer)o).collect(Collectors.toList());
    }

    @Override
    public String toString(){
        return "Case id: " + id
                + "\n name: " + name
                + "\n deliverables: " + deliverables
                + "\n qcables: " + qcables
                + "\n changelog: " + changelog + "\n";
    }
}
