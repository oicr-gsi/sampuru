package ca.on.oicr.gsi.sampuru.server.type;

import ca.on.oicr.gsi.sampuru.server.DBConnector;
import org.jooq.Record;
import org.jooq.TableField;

import java.time.LocalDateTime;

import static tables_generated.Tables.*;

public class ChangelogEntry extends SampuruType {
    public int id;
    public LocalDateTime changeDate;
    public String content;
    public String qcableType;
    public String oicrAlias;
    public String externalName;

    public ChangelogEntry(int newId, String username) throws Exception {
        getChangelogEntryFromDb(CHANGELOG.ID, newId, username);
    }

    public ChangelogEntry(Record row) {
        id = row.get(CHANGELOG.ID);
        changeDate = row.get(CHANGELOG.CHANGE_DATE);
        content = row.get(CHANGELOG.CONTENT);
        qcableType = row.get(QCABLE.QCABLE_TYPE);
        oicrAlias = row.get(QCABLE.OICR_ALIAS);
        externalName = row.get(QCABLE.EXTERNAL_NAME);
    }

    private void getChangelogEntryFromDb(TableField field, Object toMatch, String username) throws Exception {
        DBConnector dbConnector = new DBConnector();
        Record dbRecord = dbConnector.getUniqueRow(field, toMatch);
        id = dbRecord.get(CHANGELOG.ID);
        changeDate = dbRecord.get(CHANGELOG.CHANGE_DATE);
        content = dbRecord.get(CHANGELOG.CONTENT);
        qcableType = dbRecord.get(QCABLE.QCABLE_TYPE);
        oicrAlias = dbRecord.get(QCABLE.OICR_ALIAS);
        externalName = dbRecord.get(DONOR_CASE.NAME);
    }

    @Override
    public String toString(){
        return "ChangelogEntry id: " + id
                + "\n changeDate: " + changeDate
                + "\n content: " + content
                + "\n qcableType: " + qcableType
                + "\n qcableOicrAlias: " + oicrAlias
                + "\n externalName: " + externalName + "\n";
    }
}
