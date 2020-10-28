package ca.on.oicr.gsi.sampuru.server.type;

import ca.on.oicr.gsi.sampuru.server.DBConnector;
import org.jooq.Record;
import org.jooq.TableField;

import java.time.LocalDateTime;
import java.util.List;

import static tables_generated.Tables.*;


public class Notification extends SampuruType {
    public LocalDateTime issueDate;
    public LocalDateTime resolvedDate;
    public String content;

    public Notification(int newId) throws Exception {
        getNotificationFromDb(NOTIFICATION.ID, newId);
    }

    public static List<Notification> getAll() throws Exception {
        return getAll(NOTIFICATION, Notification.class);
    }

    private void getNotificationFromDb(TableField field, Object toMatch) throws Exception {
        DBConnector dbConnector = new DBConnector();
        Record dbRecord = dbConnector.getUniqueRow(field, toMatch);
        id = dbRecord.getValue(NOTIFICATION.ID);
        issueDate = dbRecord.getValue(NOTIFICATION.ISSUE_DATE);
        resolvedDate = dbRecord.getValue(NOTIFICATION.RESOLVED_DATE);
        content = dbRecord.getValue(NOTIFICATION.CONTENT);
    }
}
