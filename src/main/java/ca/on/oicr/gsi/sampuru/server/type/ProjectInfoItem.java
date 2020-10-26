package ca.on.oicr.gsi.sampuru.server.type;

import ca.on.oicr.gsi.sampuru.server.DBConnector;
import org.jooq.Record;
import org.jooq.TableField;

import static tables_generated.Tables.*;

public class ProjectInfoItem extends SampuruType {
    public String entryType;
    public String content;
    public int expected;
    public int received;

    public ProjectInfoItem(int newId) throws Exception {
        getProjectInfoItemFromDb(PROJECT_INFO_ITEM.ID, newId);
    }

    private void getProjectInfoItemFromDb(TableField field, Object toMatch) throws Exception {
        DBConnector dbConnector = new DBConnector();
        Record dbRecord = dbConnector.getUniqueRow(field, toMatch);
        id = dbRecord.getValue(PROJECT_INFO_ITEM.ID);
        entryType = dbRecord.getValue(PROJECT_INFO_ITEM.TYPE);
        content = dbRecord.getValue(PROJECT_INFO_ITEM.CONTENT);
        expected = dbRecord.getValue(PROJECT_INFO_ITEM.EXPECTED);
        received = dbRecord.getValue(PROJECT_INFO_ITEM.RECEIVED);
    }
}
