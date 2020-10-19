package type;

import java.time.Instant;
import java.util.List;

public class Project extends SampuruType {
    public String name;
    public String contactName;
    public String contactEmail;
    public Instant completionDate;
    public List<ProjectInfoItem> infoItems;
    public List<Case> donorCases;
    public List<Deliverable> deliverables;

    public Project(int id){
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public Project(String name){
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
