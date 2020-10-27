package ca.on.oicr.gsi.sampuru.server.type;

import java.util.List;

public class Case extends SampuruType {
    public String name;
    public List<Deliverable> deliverables;
    public List<QCable> qcables;
    public List<ChangelogEntry> changelog;

    public Case(int id){
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public Case(String name){
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
