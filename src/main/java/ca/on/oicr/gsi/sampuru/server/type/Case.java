package ca.on.oicr.gsi.sampuru.server.type;

import ca.on.oicr.gsi.sampuru.server.DBConnector;
import org.jooq.Record;
import org.jooq.TableField;

import java.util.LinkedList;
import java.util.List;

import static tables_generated.Tables.*;

public class Case extends SampuruType {
    public static final String DELIVERABLE_IDS = "deliverable_ids";
    public static final String QCABLE_IDS = "qcable_ids";
    public static final String CHANGELOG_IDS = "changelog_ids";
    public String name;
    public List<Integer> deliverables = new LinkedList<>();
    public List<Integer> qcables = new LinkedList<>();
    public List<Integer> changelog = new LinkedList<>();

    public Case(int id) throws Exception {
        throw new UnsupportedOperationException("Implement me :("); //TODO Implement me
    }

    public Case(String name) throws Exception {
        throw new UnsupportedOperationException("Implement me :("); //TODO Implement me
    }

    public Case(Record row) throws Exception {
        id = row.get(DONOR_CASE.ID);
        name = row.get(DONOR_CASE.NAME);

        deliverables = row.get(DELIVERABLE_IDS, List.class);
        qcables = row.get(QCABLE_IDS, List.class);
        changelog = row.get(CHANGELOG_IDS, List.class);
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

    @Override
    public String toString(){
        return "Case id: " + id
                + "\n name: " + name
                + "\n deliverables: " + deliverables
                + "\n qcables: " + qcables
                + "\n changelog: " + changelog + "\n";
    }
}
