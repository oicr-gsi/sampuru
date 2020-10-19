package ca.on.oicr.gsi.sampuru.server.type;

import java.time.Instant;

public class ChangelogEntry extends SampuruType {
    public Instant changeDate;
    public String content;

    public ChangelogEntry(int id){
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
