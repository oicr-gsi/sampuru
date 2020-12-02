package ca.on.oicr.gsi.sampuru.server.service;

import ca.on.oicr.gsi.sampuru.server.DBConnector;
import ca.on.oicr.gsi.sampuru.server.type.*;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.jooq.Record;
import org.jooq.util.postgres.PostgresDSL;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static tables_generated.Tables.*;


public class ProjectService extends Service<Project> {

    public ProjectService(){
        super(Project.class);
    }

    public static void getIdParams(HttpServerExchange hse) throws Exception {
        getIdParams(new ProjectService(), hse);
    }

    public static void getAllParams(HttpServerExchange hse) throws Exception {
        getAllParams(new ProjectService(), hse);
    }

    // TODO: Some real good repeat code b/w this and getActiveProjects - refactor me
    public List<Project> getCompletedProjects() throws Exception {
        DSLContext context = new DBConnector().getContext();
        List<Project> newList = new LinkedList<>();

        Result<Record> results = context
                .select(PROJECT.asterisk(),
                        PostgresDSL.array(context
                                .select(DONOR_CASE.ID)
                                .from(DONOR_CASE)
                                .where(DONOR_CASE.PROJECT_ID.eq(PROJECT.ID)))
                                .as(Project.CASE_IDS),
                        PostgresDSL.array(context
                                .select(PROJECT_INFO_ITEM.ID)
                                .from(PROJECT_INFO_ITEM)
                                .where(PROJECT_INFO_ITEM.PROJECT_ID.eq(PROJECT.ID)))
                                .as(Project.INFO_ITEM_IDS),
                        PostgresDSL.array(context
                                .select(DELIVERABLE_FILE.ID)
                                .from(DELIVERABLE_FILE)
                                .where(DELIVERABLE_FILE.PROJECT_ID.eq(PROJECT.ID)))
                                .as(Project.DELIVERABLE_IDS))
                .from(PROJECT)
                .where(PROJECT.COMPLETION_DATE.isNotNull())
                .fetch();

        for (Record result: results) {
            newList.add(new Project(result));
        }

        return newList;
    }

    public static void getCompletedProjectsParams(HttpServerExchange hse) throws Exception {
        String name = hse.getRequestHeaders().get("X-Remote-User").element();
        ProjectService ps = new ProjectService();
        hse.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        hse.getResponseSender().send(ps.getCompletedProjectsJson());
    }

    //TODO: add user to params
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

    //TODO: add user to params
    public List<Project> getActiveProjects() throws Exception {
        DSLContext context = new DBConnector().getContext();
        List<Project> newList = new LinkedList<>();

        Result<Record> results = context
                .select(PROJECT.asterisk(),
                        PostgresDSL.array(context
                                .select(DONOR_CASE.ID)
                                .from(DONOR_CASE)
                                .where(DONOR_CASE.PROJECT_ID.eq(PROJECT.ID)))
                                .as(Project.CASE_IDS),
                        PostgresDSL.array(context
                                .select(PROJECT_INFO_ITEM.ID)
                                .from(PROJECT_INFO_ITEM)
                                .where(PROJECT_INFO_ITEM.PROJECT_ID.eq(PROJECT.ID)))
                                .as(Project.INFO_ITEM_IDS),
                        PostgresDSL.array(context
                                .select(DELIVERABLE_FILE.ID)
                                .from(DELIVERABLE_FILE)
                                .where(DELIVERABLE_FILE.PROJECT_ID.eq(PROJECT.ID)))
                                .as(Project.DELIVERABLE_IDS),
                        PostgresDSL.field(context
                                .selectCount()
                                .from(DONOR_CASE)
                                .where(DONOR_CASE.PROJECT_ID.eq(PROJECT.ID)))
                                .as(Project.CASES_TOTAL),
                        PostgresDSL.field(context
                                .selectCount()
                                .from(context
                                        .selectDistinct(QCABLE.CASE_ID)
                                        .from(QCABLE)
                                        .where(QCABLE.CASE_ID.in(context
                                                .select(DONOR_CASE.ID)
                                                .from(DONOR_CASE)
                                                .where(DONOR_CASE.PROJECT_ID.eq(PROJECT.ID)))
                                                .and(QCABLE.QCABLE_TYPE.eq("final_report"))
                                                .and(QCABLE.STATUS.eq(DBConnector.QC_PASSED))
                                                .andExists(context
                                                        .select(DELIVERABLE_FILE.ID)
                                                        .from(DELIVERABLE_FILE)
                                                        .where(DELIVERABLE_FILE.CASE_ID.in(context
                                                                .select(QCABLE.CASE_ID)
                                                                .from(QCABLE).where(QCABLE.CASE_ID.in(context
                                                                        .select(DONOR_CASE.ID)
                                                                        .from(DONOR_CASE)
                                                                        .where(DONOR_CASE.PROJECT_ID.eq(PROJECT.ID)))
                                                                        .and(QCABLE.QCABLE_TYPE.eq("final_report")))))))))
                                .as(Project.CASES_COMPLETED),
                        PostgresDSL.field(context
                                .selectCount()
                                .from(QCABLE)
                                .where(QCABLE.PROJECT_ID.eq(PROJECT.ID)))
                                .as(Project.QCABLES_TOTAL),
                        PostgresDSL.field(context
                                .selectCount()
                                .from(QCABLE)
                                .where(QCABLE.PROJECT_ID.eq(PROJECT.ID)
                                        .and(QCABLE.STATUS.eq(DBConnector.QC_PASSED))))
                                .as(Project.QCABLES_COMPLETED)
                        )
                .from(PROJECT)
                .where(PROJECT.COMPLETION_DATE.isNull())
                .fetch();

        for (Record result: results) {
            newList.add(new Project(result));
        }

        return newList;
    }

    public static void getActiveProjectsParams(HttpServerExchange hse) throws Exception {
        String name = hse.getRequestHeaders().get("X-Remote-User").element();
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
            projectObject.put("last_update", activeProject.lastUpdate == null? "null": JSONObject.escape(activeProject.lastUpdate.toString()));
            projectObject.put("cases_total", activeProject.casesTotal);
            projectObject.put("cases_completed", activeProject.casesCompleted);
            projectObject.put("qcables_total", activeProject.qcablesTotal);
            projectObject.put("qcables_completed", activeProject.qcablesCompleted);
            jsonArray.add(projectObject);
        }
        return jsonArray.toJSONString();
    }

    @Override
    public List<Project> getAll() {
        DSLContext context = new DBConnector().getContext();
        List<Project> projects = new LinkedList<>();

        Result<Record> results = context
                .select(PROJECT.asterisk(),
                        PostgresDSL.array(context
                                .select(DONOR_CASE.ID)
                                .from(DONOR_CASE)
                                .where(DONOR_CASE.PROJECT_ID.eq(PROJECT.ID)))
                                .as(Project.CASE_IDS),
                        PostgresDSL.array(context
                                .select(PROJECT_INFO_ITEM.ID)
                                .from(PROJECT_INFO_ITEM)
                                .where(PROJECT_INFO_ITEM.PROJECT_ID.eq(PROJECT.ID)))
                                .as(Project.INFO_ITEM_IDS),
                        PostgresDSL.array(context
                                .select(DELIVERABLE_FILE.ID)
                                .from(DELIVERABLE_FILE)
                                .where(DELIVERABLE_FILE.PROJECT_ID.eq(PROJECT.ID)))
                                .as(Project.DELIVERABLE_IDS))
                .from(PROJECT)
                .fetch();

        for(Record result: results){
            projects.add(new Project(result));
        }
        return projects;
    }

    @Override
    public List<Project> search(String term) throws Exception {
        List<String> ids = new DBConnector().search(PROJECT, PROJECT.ID, PROJECT.NAME, term).stream().map(o -> (String)o).collect(Collectors.toList());
        List<Project> projects = new LinkedList<>();

        for (String id: ids){
            projects.add(get(id));
        }

        return projects;
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
            jsonObject.put("completion_date", project.completionDate == null? "null": JSONObject.escape(project.completionDate.toString()));

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
        jsonObject.put("cases_total", subject.casesTotal);
        jsonObject.put("cases_completed", subject.casesCompleted);
        jsonObject.put("qcables_total", subject.qcablesTotal);
        jsonObject.put("qcables_completed", subject.qcablesCompleted);

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

        jsonObject.put("sankey_transitions", new DBConnector().getSankeyTransitions(subject.id));

        return jsonObject.toJSONString();
    }

    public static void getProjectOverviewParams(HttpServerExchange hse) throws Exception {
        String name = hse.getRequestHeaders().get("X-Remote-User").element();

        //TODO: different retrieval method?
        Deque<String> idparams = hse.getQueryParameters().get("id");
        if(idparams.size() > 1){
            throw new UnsupportedOperationException("Only takes 1 project ID");
        } else if(idparams.isEmpty()){
            throw new UnsupportedOperationException("Got no project IDs");
        }
        ProjectService ps = new ProjectService();
        hse.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        hse.getResponseSender().send(ps.getProjectOverviewJson(ps.get(idparams.getFirst())));
    }
}
