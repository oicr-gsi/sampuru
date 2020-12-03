package ca.on.oicr.gsi.sampuru.server.type;

import ca.on.oicr.gsi.sampuru.server.DBConnector;
import org.jooq.Record;
import org.jooq.TableField;

import static tables_generated.Tables.*;

public class ProjectInfoItem extends SampuruType {
    public int id;
    public String entryType;
    public String content;
    public Integer expected;
    public Integer received;

    public ProjectInfoItem(int newId, String username) throws Exception {
        getProjectInfoItemFromDb(PROJECT_INFO_ITEM.ID, newId, username);
    }

    private void getProjectInfoItemFromDb(TableField field, Object toMatch, String username) throws Exception {
        DBConnector dbConnector = new DBConnector(username);
        Record dbRecord = dbConnector.getUniqueRow(field, toMatch);
        id = dbRecord.get(PROJECT_INFO_ITEM.ID);
        entryType = dbRecord.get(PROJECT_INFO_ITEM.TYPE);
        content = dbRecord.get(PROJECT_INFO_ITEM.CONTENT);
        expected = dbRecord.get(PROJECT_INFO_ITEM.EXPECTED);
        received = dbRecord.get(PROJECT_INFO_ITEM.RECEIVED);
    }

    @Override
    public String toString(){
        return "ProjectInfoItem id: " + id
                + "\n entryType: " + entryType
                + "\n content: " + content
                + "\n expected: " + expected
                + "\n received: " + received + "\n";
    }
}
