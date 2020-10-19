package ca.on.oicr.gsi.sampuru.server.type;

import java.time.Instant;

public class Notification extends SampuruType {
    public Instant issueDate;
    public Instant resolvedDate;
    public String content;

    public Notification(int id){
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
