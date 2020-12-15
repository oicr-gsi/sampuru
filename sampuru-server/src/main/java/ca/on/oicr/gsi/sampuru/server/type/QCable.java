package ca.on.oicr.gsi.sampuru.server.type;

import ca.on.oicr.gsi.sampuru.server.DBConnector;
import org.jooq.Record;
import org.jooq.TableField;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static tables_generated.Tables.*;


public class QCable extends SampuruType {
    public String id;
    public static final String CHANGELOG_IDS = "changelog_ids";
    public String OICRAlias;
    public String status;
    public String failureReason;
    public String libraryDesign;
    public String type;
    public String parentId;
    public List<Integer> changelog;

    public QCable(String newId, String username) throws Exception {
        getQCableFromDb(QCABLE.ID, newId, username);
    }

    public QCable(Record row) {
        id = row.get(QCABLE.ID);
        OICRAlias = row.get(QCABLE.OICR_ALIAS);
        status = row.get(QCABLE.STATUS);
        failureReason = row.get(QCABLE.FAILURE_REASON);
        libraryDesign = row.get(QCABLE.LIBRARY_DESIGN);
        type = row.get(QCABLE.QCABLE_TYPE);
        parentId = row.get(QCABLE.PARENT_ID);
        changelog = row.get(CHANGELOG_IDS, List.class);
    }

    public static List<QCable> getAll(String username) throws Exception {
        return getAll(QCABLE, QCable.class, username);
    }

    public List<ChangelogEntry> getChangelog(String username) throws Exception {
        List<ChangelogEntry> newList = new LinkedList<>();
        for(Integer changelogId: changelog){
            newList.add(new ChangelogEntry(changelogId, username));
        }
        return newList;
    }

    private void getQCableFromDb(TableField field, Object toMatch, String username) throws Exception {
        DBConnector dbConnector = new DBConnector();
        Record dbRecord = dbConnector.getUniqueRow(field, toMatch);
        id = dbRecord.get(QCABLE.ID);
        OICRAlias = dbRecord.get(QCABLE.OICR_ALIAS);
        status = dbRecord.get(QCABLE.STATUS);
        failureReason = dbRecord.get(QCABLE.FAILURE_REASON);
        libraryDesign = dbRecord.get(QCABLE.LIBRARY_DESIGN);
        type = dbRecord.get(QCABLE.QCABLE_TYPE);
        parentId = dbRecord.get(QCABLE.PARENT_ID);

        changelog = dbConnector.getChildIdList(CHANGELOG, CHANGELOG.QCABLE_ID, id).stream().map(o -> (Integer)o).collect(Collectors.toList());;
    }

    @Override
    public String toString(){
        return "QCable id: " + id
                + "\n OICRAlias: " + OICRAlias
                + "\n status: " + status
                + "\n failureReason: " + failureReason
                + "\n libraryDesign: " + libraryDesign
                + "\n type: " + type
                + "\n parentId: " + parentId
                + "\n changelog: " + changelog + "\n";
    }
}
