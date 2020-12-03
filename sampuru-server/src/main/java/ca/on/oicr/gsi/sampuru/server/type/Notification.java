package ca.on.oicr.gsi.sampuru.server.type;

import ca.on.oicr.gsi.sampuru.server.DBConnector;
import org.jooq.Record;
import org.jooq.TableField;

import java.time.LocalDateTime;
import java.util.List;

import static tables_generated.Tables.*;


public class Notification extends SampuruType {
    public int id;
    public LocalDateTime issueDate;
    public LocalDateTime resolvedDate;
    public String content;

    public Notification(int newId, String username) throws Exception {
        getNotificationFromDb(NOTIFICATION.ID, newId, username);
    }

    public Notification(Record row) {
        id = row.get(NOTIFICATION.ID);
        issueDate = row.get(NOTIFICATION.ISSUE_DATE);
        resolvedDate = row.get(NOTIFICATION.RESOLVED_DATE);
        content = row.get(NOTIFICATION.CONTENT);
    }

    public static List<Notification> getAll(String username) throws Exception {
        return getAll(NOTIFICATION, Notification.class, username);
    }

    private void getNotificationFromDb(TableField field, Object toMatch, String username) throws Exception {
        DBConnector dbConnector = new DBConnector(username);
        Record dbRecord = dbConnector.getUniqueRow(field, toMatch);
        id = dbRecord.get(NOTIFICATION.ID);
        issueDate = dbRecord.get(NOTIFICATION.ISSUE_DATE);
        resolvedDate = dbRecord.get(NOTIFICATION.RESOLVED_DATE);
        content = dbRecord.get(NOTIFICATION.CONTENT);
    }

    @Override
    public String toString(){
        return "Notification id: " + id
                + "\n issueDate: " + issueDate
                + "\n resolvedDate: " + resolvedDate
                + "\n content: " + content + "\n";
    }
}
