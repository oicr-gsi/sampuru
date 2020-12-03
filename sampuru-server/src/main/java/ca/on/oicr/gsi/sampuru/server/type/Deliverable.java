package ca.on.oicr.gsi.sampuru.server.type;

import ca.on.oicr.gsi.sampuru.server.DBConnector;
import org.jooq.Record;
import org.jooq.TableField;

import java.time.LocalDateTime;

import static tables_generated.Tables.*;


public class Deliverable extends SampuruType {
    public int id;
    public String content;
    public LocalDateTime expiryDate;

    public Deliverable(int newId, String username) throws Exception {
        getDeliverableFromDb(DELIVERABLE_FILE.ID, newId, username);
    }

    public Deliverable(Record row) {
        id = row.get(DELIVERABLE_FILE.ID);
        content = row.get(DELIVERABLE_FILE.CONTENT);
        expiryDate = row.get(DELIVERABLE_FILE.EXPIRY_DATE);
    }

    private void getDeliverableFromDb(TableField field, Object toMatch, String username) throws Exception {
        DBConnector dbConnector = new DBConnector(username);
        Record dbRecord = dbConnector.getUniqueRow(field, toMatch);
        id = dbRecord.get(DELIVERABLE_FILE.ID);
        content = dbRecord.get(DELIVERABLE_FILE.CONTENT);
        expiryDate = dbRecord.get(DELIVERABLE_FILE.EXPIRY_DATE);
    }

    @Override
    public String toString(){
        return "Deliverable id: " + id
                + "\n content: " + content
                + "\n expiryDate: " + expiryDate + "\n";
    }
}
