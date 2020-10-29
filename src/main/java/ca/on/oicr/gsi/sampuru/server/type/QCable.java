package ca.on.oicr.gsi.sampuru.server.type;

import ca.on.oicr.gsi.sampuru.server.DBConnector;
import org.jooq.Record;
import org.jooq.TableField;

import java.util.List;

import static tables_generated.Tables.*;


public class QCable extends SampuruType {
    public String OICRAlias;
    public String status;
    public String failureReason;
    public String libraryDesign;
    public String type;
    public int parentId;
    public List<ChangelogEntry> changelog;

    public QCable(int newId) throws Exception {
        getQCableFromDb(QCABLE.ID, newId);
    }

    public QCable(String alias) throws Exception {
        getQCableFromDb(QCABLE.OICR_ALIAS, alias);
    }

    public static List<QCable> getAll() throws Exception {
        return getAll(QCABLE, QCable.class);
    }

    private void getQCableFromDb(TableField field, Object toMatch) throws Exception {
        DBConnector dbConnector = new DBConnector();
        Record dbRecord = dbConnector.getUniqueRow(field, toMatch);
        id = dbRecord.getValue(QCABLE.ID);
        OICRAlias = dbRecord.getValue(QCABLE.OICR_ALIAS);
        status = dbRecord.getValue(QCABLE.STATUS);
        failureReason = dbRecord.getValue(QCABLE.FAILURE_REASON);
        libraryDesign = dbRecord.getValue(QCABLE.LIBRARY_DESIGN);
        type = dbRecord.getValue(QCABLE.QCABLE_TYPE);
        parentId = dbRecord.getValue(QCABLE.PARENT_ID);

        changelog = dbConnector.getMany(CHANGELOG.ID, CHANGELOG.QCABLE_ID, id, ChangelogEntry.class);
    }
}
