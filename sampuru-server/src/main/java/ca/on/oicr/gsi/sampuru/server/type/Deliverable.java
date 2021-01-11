package ca.on.oicr.gsi.sampuru.server.type;

import ca.on.oicr.gsi.sampuru.server.DBConnector;
import org.jooq.Record;
import org.jooq.TableField;

import java.time.LocalDateTime;

import static tables_generated.Tables.*;


public class Deliverable extends SampuruType {
    public int id;
    public String location;
    public String notes;
    public LocalDateTime expiryDate;

    public Deliverable(int newId, String username) throws Exception {
        getDeliverableFromDb(DELIVERABLE_FILE.ID, newId, username);
    }

    public Deliverable(Record row) {
        id = row.get(DELIVERABLE_FILE.ID);
        location = row.get(DELIVERABLE_FILE.LOCATION);
        notes = row.get(DELIVERABLE_FILE.NOTES);
        expiryDate = row.get(DELIVERABLE_FILE.EXPIRY_DATE);
    }

    private void getDeliverableFromDb(TableField field, Object toMatch, String username) throws Exception {
        DBConnector dbConnector = new DBConnector();
        Record dbRecord = dbConnector.getUniqueRow(field, toMatch);
        id = dbRecord.get(DELIVERABLE_FILE.ID);
        location = dbRecord.get(DELIVERABLE_FILE.LOCATION);
        notes = dbRecord.get(DELIVERABLE_FILE.NOTES);
        expiryDate = dbRecord.get(DELIVERABLE_FILE.EXPIRY_DATE);
    }

    @Override
    public String toString(){
        return "Deliverable id: " + id
                + "\n location: " + location
                + "\n notes: " + notes
                + "\n expiryDate: " + expiryDate + "\n";
    }
}
