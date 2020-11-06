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
        //getCaseFromDb(DONOR_CASE.ID, id);
    }

    public Case(String name) throws Exception {
        //getCaseFromDb(DONOR_CASE.NAME, name);
    }

    public Case(Record row) throws Exception {
        getCaseFromRow(row);
    }

    public static List<Case> getAll() throws Exception {
        return getAll(DONOR_CASE, Case.class);
    }

    public List<QCable> getQcables() throws Exception {
        List<QCable> newList = new LinkedList<>();
        for (Integer qcId: qcables){
            newList.add(new QCable(qcId));
        }
        return newList;
    }

    public List<Deliverable> getDeliverables() throws Exception {
        List<Deliverable> newList = new LinkedList<>();
        for (Integer deliverableId: deliverables){
            newList.add(new Deliverable(deliverableId));
        }
        return newList;
    }

    public List<ChangelogEntry> getChangelog() throws Exception {
        List<ChangelogEntry> newList = new LinkedList<>();
        for(Integer changelogId: changelog){
            newList.add(new ChangelogEntry(changelogId));
        }
        return newList;
    }

    private void getCaseFromRow(Record dbRecord) throws Exception {
        id = dbRecord.get(DONOR_CASE.ID);
        name = dbRecord.get(DONOR_CASE.NAME);

        deliverables = dbRecord.get("deliverable_file_ids", List.class);
        qcables = dbRecord.get("qcable_ids", List.class);
        changelog = dbRecord.get("changelog_ids", List.class);
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
