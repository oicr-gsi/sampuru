package ca.on.oicr.gsi.sampuru.server.type;

import ca.on.oicr.gsi.sampuru.server.DBConnector;
import org.jooq.Record;
import org.jooq.TableField;

import java.time.LocalDateTime;
import java.util.List;

import static tables_generated.Tables.*;


public class Deliverable extends SampuruType {
    public String content;
    public LocalDateTime expiryDate;

    public Deliverable(int newId) throws Exception {
        getDeliverableFromDb(DELIVERABLE_FILE.ID, newId);
    }

    private void getDeliverableFromDb(TableField field, Object toMatch) throws Exception {
        DBConnector dbConnector = new DBConnector();
        Record dbRecord = dbConnector.getUniqueRow(field, toMatch);
        id = dbRecord.getValue(DELIVERABLE_FILE.ID);
        content = dbRecord.getValue(DELIVERABLE_FILE.CONTENT);
        expiryDate = dbRecord.getValue(DELIVERABLE_FILE.EXPIRY_DATE);
    }
}
