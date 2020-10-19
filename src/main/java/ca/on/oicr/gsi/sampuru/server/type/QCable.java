package type;

import java.util.List;

public class QCable extends SampuruType {
    public String OICRAlias;
    public String status;
    public String failureReason;
    public List<ChangelogEntry> changelog;

    public QCable(int id){
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public QCable(String alias){
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
