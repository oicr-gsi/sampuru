package ca.on.oicr.gsi.sampuru.server.service;

import ca.on.oicr.gsi.sampuru.server.DBConnector;
import ca.on.oicr.gsi.sampuru.server.type.*;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Collection;
import java.util.Deque;
import java.util.List;

public class ProjectService extends Service<Project> {

    public ProjectService(){
        super(Project.class);
    }

    public Project get(String name){
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public static void getIdParams(HttpServerExchange hse) throws Exception {
        getIdParams(new ProjectService(), hse);
    }

    public static void getAllParams(HttpServerExchange hse) throws Exception {
        getAllParams(new ProjectService(), hse);
    }

    public List<Project> getCompletedProjects() throws Exception {
        return Project.getCompleted();
    }

    public static void getCompletedProjectsParams(HttpServerExchange hse) throws Exception {
        ProjectService ps = new ProjectService();
        hse.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        hse.getResponseSender().send(ps.getCompletedProjectsJson());
    }

    private String getCompletedProjectsJson() throws Exception {
        List<Project> completedProjects = getCompletedProjects();
        JSONArray jsonArray = new JSONArray();
        for (Project completedProject: completedProjects){
            JSONObject projectObject = new JSONObject();
            projectObject.put("id", completedProject.id);
            projectObject.put("name", completedProject.name);
            projectObject.put("completion_date", completedProject.completionDate == null? "null": JSONObject.escape(completedProject.completionDate.toString()));
            jsonArray.add(projectObject);
        }
        return jsonArray.toJSONString();
    }

    public List<Project> getActiveProjects() throws Exception {
        return Project.getActive();
    }

    public static void getActiveProjectsParams(HttpServerExchange hse) throws Exception {
        ProjectService ps = new ProjectService();
        hse.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        hse.getResponseSender().send(ps.getActiveProjectsJson(ps.getActiveProjects()));
    }

    private String getActiveProjectsJson(List<Project> activeProjects) throws Exception {
        JSONArray jsonArray = new JSONArray();
        for(Project activeProject: activeProjects) {
            JSONObject projectObject = new JSONObject();
            projectObject.put("id", activeProject.id);
            projectObject.put("name", activeProject.name);
            projectObject.put("last_update", JSONObject.escape(activeProject.getLastUpdate().toString()));
            projectObject.put("cases_total", activeProject.getCasesTotal());
            projectObject.put("cases_completed", activeProject.getCasesCompleted());
            projectObject.put("qcables_total", activeProject.getQCablesTotal());
            projectObject.put("qcables_completed", activeProject.getQCablesCompleted());
            jsonArray.add(projectObject);
        }
        return jsonArray.toJSONString();
    }

    @Override
    public String toJson(Collection<? extends SampuruType> toWrite) throws Exception {
        return toJson(toWrite, false);
    }

    public String toJson(Collection<? extends SampuruType> toWrite, boolean expand) throws Exception {
        JSONArray jsonArray = new JSONArray();

        for(SampuruType item: toWrite){
            Project project = (Project)item;
            JSONObject jsonObject = new JSONObject();

            jsonObject.put("id", project.id);
            jsonObject.put("name", project.name);
            jsonObject.put("contact_name", project.contactName == null? "null": project.contactName);
            jsonObject.put("contact_email", project.contactEmail == null? "null": project.contactEmail);
            jsonObject.put("completion_date", JSONObject.escape(project.completionDate.toString()));

            if(expand){
                JSONArray infoItemsArray = new JSONArray();
                for (ProjectInfoItem infoItem: project.getInfoItems()){
                    JSONObject infoItemObj = new JSONObject();
                    infoItemObj.put("id", infoItem.id);
                    infoItemObj.put("entry_type", infoItem.entryType);
                    infoItemObj.put("content", infoItem.content);
                    infoItemObj.put("expected", infoItem.expected == null? "null": infoItem.expected);
                    infoItemObj.put("received", infoItem.received == null? "null": infoItem.received);
                    infoItemsArray.add(infoItemObj);
                }
                jsonObject.put("info_items", infoItemsArray);

                jsonObject.put("donor_cases", new CaseService().toJson(project.getCases(), true));
                jsonObject.put("deliverables", new DeliverableService().toJson(project.getDeliverables()));
            } else {
                jsonObject.put("info_items", project.infoItems);
                jsonObject.put("donor_cases", project.donorCases);
                jsonObject.put("deliverables", project.deliverables);
            }

            jsonArray.add(jsonObject);
        }

        return jsonArray.toJSONString();
    }

    public String getProjectOverviewJson(Project subject) throws Exception {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", subject.id);
        jsonObject.put("name", subject.name);
        jsonObject.put("contact_name", subject.contactName == null? "null": subject.contactName);
        jsonObject.put("contact_email", subject.contactEmail == null? "null": subject.contactEmail);
        jsonObject.put("completion_date", subject.completionDate  == null? "null": JSONObject.escape(subject.completionDate.toString()));
        jsonObject.put("cases_total", subject.getCasesTotal());
        jsonObject.put("cases_completed", subject.getCasesCompleted());
        jsonObject.put("qcables_total", subject.getQCablesTotal());
        jsonObject.put("qcables_completed", subject.getQCablesCompleted());

        JSONArray infoItemsArray = new JSONArray();
        for (ProjectInfoItem infoItem: subject.getInfoItems()){
            JSONObject infoItemObj = new JSONObject();
            infoItemObj.put("id", infoItem.id);
            infoItemObj.put("entry_type", infoItem.entryType);
            infoItemObj.put("content", infoItem.content);
            infoItemObj.put("expected", infoItem.expected == null? "null": infoItem.expected);
            infoItemObj.put("received", infoItem.received == null? "null": infoItem.expected);
            infoItemsArray.add(infoItemObj);
        }
        jsonObject.put("info_items", infoItemsArray);

        JSONArray deliverablesArray = new JSONArray();
        for (Deliverable deliverable: subject.getDeliverables()){
            JSONObject deliverableObj = new JSONObject();
            deliverableObj.put("id", deliverable.id);
            deliverableObj.put("expiry_date", JSONObject.escape(deliverable.expiryDate.toString()));
            deliverableObj.put("content", deliverable.content);
            infoItemsArray.add(deliverableObj);
        }
        jsonObject.put("deliverables", deliverablesArray);

        JSONArray failureArray = new JSONArray();
        for(QCable failedQCable: subject.getFailedQCables()){
            JSONObject failureObj = new JSONObject();
            failureObj.put("id", failedQCable.id);
            failureObj.put("alias", failedQCable.OICRAlias);
            failureObj.put("failure_reason", failedQCable.failureReason == null? "null": failedQCable.failureReason);
            failureArray.add(failureObj);
        }
        jsonObject.put("failures", failureArray);

        jsonObject.put("sankey_transitions", new DBConnector().buildSankeyTransitions(subject));

        return jsonObject.toJSONString();
    }

    public static void getProjectOverviewParams(HttpServerExchange hse) throws Exception {
        Deque<String> idparams = hse.getQueryParameters().get("id");
        if(idparams.size() > 1){
            throw new UnsupportedOperationException("Only takes 1 project ID");
        } else if(idparams.isEmpty()){
            throw new UnsupportedOperationException("Got no project IDs");
        }
        ProjectService ps = new ProjectService();
        hse.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        hse.getResponseSender().send(ps.getProjectOverviewJson(ps.get(Integer.valueOf(idparams.getFirst()))));
    }
}
