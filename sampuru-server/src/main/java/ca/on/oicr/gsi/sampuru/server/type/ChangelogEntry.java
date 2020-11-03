package ca.on.oicr.gsi.sampuru.server.type;

import ca.on.oicr.gsi.sampuru.server.DBConnector;
import org.jooq.Record;
import org.jooq.TableField;

import java.time.LocalDateTime;

import static tables_generated.Tables.*;

public class ChangelogEntry extends SampuruType {
    public LocalDateTime changeDate;
    public String content;

    public ChangelogEntry(int newId) throws Exception {
        getChangelogEntryFromDb(CHANGELOG.ID, newId);
    }

    private void getChangelogEntryFromDb(TableField field, Object toMatch) throws Exception {
        DBConnector dbConnector = new DBConnector();
        Record dbRecord = dbConnector.getUniqueRow(field, toMatch);
        id = dbRecord.get(CHANGELOG.ID);
        changeDate = dbRecord.get(CHANGELOG.CHANGE_DATE);
        content = dbRecord.get(CHANGELOG.CONTENT);
    }

    @Override
    public String toString(){
        return "ChangelogEntry id: " + id
                + "\n changeDate: " + changeDate
                + "\n content: " + content + "\n";
    }
}
