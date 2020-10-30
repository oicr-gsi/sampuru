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
    public Integer parentId;
    public List<Integer> changelog;

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
        id = dbRecord.get(QCABLE.ID);
        OICRAlias = dbRecord.get(QCABLE.OICR_ALIAS);
        status = dbRecord.get(QCABLE.STATUS);
        failureReason = dbRecord.get(QCABLE.FAILURE_REASON);
        libraryDesign = dbRecord.get(QCABLE.LIBRARY_DESIGN);
        type = dbRecord.get(QCABLE.QCABLE_TYPE);
        parentId = dbRecord.get(QCABLE.PARENT_ID);

        changelog = dbConnector.getChildIdList(CHANGELOG, CHANGELOG.QCABLE_ID, id);
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
                + "\n changelog: " + changelog;
    }

//    public String OICRAlias;
//    public String status;
//    public String failureReason;
//    public String libraryDesign;
//    public String type;
//    public Integer parentId;
//    public List<Integer> changelog;
}
