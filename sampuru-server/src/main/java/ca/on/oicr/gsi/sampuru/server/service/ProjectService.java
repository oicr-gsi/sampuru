package ca.on.oicr.gsi.sampuru.server.service;

import ca.on.oicr.gsi.sampuru.server.DBConnector;
import ca.on.oicr.gsi.sampuru.server.type.*;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.PathTemplateMatch;
import org.jooq.Result;
import org.jooq.Record;
import org.jooq.util.postgres.PostgresDSL;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

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
    public List<Project> getCompletedProjects(String username) throws Exception {
        List<Project> newList = new LinkedList<>();

        Result<Record> results = new DBConnector().fetch(
                PostgresDSL.select(PROJECT.asterisk(),
                        PostgresDSL.array(PostgresDSL
                                .select(DONOR_CASE.ID)
                                .from(DONOR_CASE)
                                .where(DONOR_CASE.PROJECT_ID.eq(PROJECT.ID)))
                                .as(Project.CASE_IDS),
                        PostgresDSL.array(PostgresDSL
                                .select(PROJECT_INFO_ITEM.ID)
                                .from(PROJECT_INFO_ITEM)
                                .where(PROJECT_INFO_ITEM.PROJECT_ID.eq(PROJECT.ID)))
                                .as(Project.INFO_ITEM_IDS),
                        PostgresDSL.array(PostgresDSL
                                .select(DELIVERABLE_FILE.ID)
                                .from(DELIVERABLE_FILE)
                                .where(DELIVERABLE_FILE.PROJECT_ID.eq(PROJECT.ID)))
                                .as(Project.DELIVERABLE_IDS))
                .from(PROJECT)
                .where(PROJECT.COMPLETION_DATE.isNotNull()
                        .and(PROJECT.ID.in(PostgresDSL
                                        .select(USER_ACCESS.PROJECT)
                                        .from(USER_ACCESS)
                                        .where(USER_ACCESS.USERNAME.eq(username)))
                                .or(DBConnector.ADMIN_ROLE
                                        .in(PostgresDSL
                                                .select(USER_ACCESS.PROJECT)
                                                .from(USER_ACCESS)
                                                .where(USER_ACCESS.USERNAME.eq(username)))))));

        for (Record result: results) {
            newList.add(new Project(result));
        }

        return newList;
    }

    public static void getCompletedProjectsParams(HttpServerExchange hse) throws Exception {
        String username = hse.getRequestHeaders().get("X-Remote-User").element();
        ProjectService ps = new ProjectService();
        hse.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        hse.getResponseSender().send(ps.getCompletedProjectsJson(username));
    }

    private String getCompletedProjectsJson(String username) throws Exception {
        List<Project> completedProjects = getCompletedProjects(username);
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

    public List<Project> getActiveProjects(String username) throws Exception {
        List<Project> newList = new LinkedList<>();

        Result<Record> results = new DBConnector().fetch(
                PostgresDSL.select(PROJECT.asterisk(),
                        PostgresDSL.array(PostgresDSL
                                .select(DONOR_CASE.ID)
                                .from(DONOR_CASE)
                                .where(DONOR_CASE.PROJECT_ID.eq(PROJECT.ID)))
                                .as(Project.CASE_IDS),
                        PostgresDSL.array(PostgresDSL
                                .select(PROJECT_INFO_ITEM.ID)
                                .from(PROJECT_INFO_ITEM)
                                .where(PROJECT_INFO_ITEM.PROJECT_ID.eq(PROJECT.ID)))
                                .as(Project.INFO_ITEM_IDS),
                        PostgresDSL.array(PostgresDSL
                                .select(DELIVERABLE_FILE.ID)
                                .from(DELIVERABLE_FILE)
                                .where(DELIVERABLE_FILE.PROJECT_ID.eq(PROJECT.ID)))
                                .as(Project.DELIVERABLE_IDS),
                        PostgresDSL.field(PostgresDSL
                                .selectCount()
                                .from(DONOR_CASE)
                                .where(DONOR_CASE.PROJECT_ID.eq(PROJECT.ID)))
                                .as(Project.CASES_TOTAL),
                        PostgresDSL.field(PostgresDSL
                                .selectCount()
                                .from(PostgresDSL
                                        .selectDistinct(QCABLE.CASE_ID)
                                        .from(QCABLE)
                                        .where(QCABLE.CASE_ID.in(PostgresDSL
                                                .select(DONOR_CASE.ID)
                                                .from(DONOR_CASE)
                                                .where(DONOR_CASE.PROJECT_ID.eq(PROJECT.ID)))
                                                .and(QCABLE.QCABLE_TYPE.eq("final_report"))
                                                .and(QCABLE.STATUS.eq(DBConnector.QC_PASSED))
                                                // Can't use 'in' on text[] https://www.postgresql.org/docs/current/arrays.html
                                                .andExists(PostgresDSL
                                                        .select(DELIVERABLE_FILE.ID)
                                                        .from(DELIVERABLE_FILE)
                                                        .where(QCABLE.CASE_ID.eq(PostgresDSL.any(DELIVERABLE_FILE.CASE_ID)))))))
                                .as(Project.CASES_COMPLETED),
                        PostgresDSL.field(PostgresDSL
                                .selectCount()
                                .from(QCABLE)
                                .where(QCABLE.PROJECT_ID.eq(PROJECT.ID)))
                                .as(Project.QCABLES_TOTAL),
                        PostgresDSL.field(PostgresDSL
                                .selectCount()
                                .from(QCABLE)
                                .where(QCABLE.PROJECT_ID.eq(PROJECT.ID)
                                        .and(QCABLE.STATUS.eq(DBConnector.QC_PASSED))))
                                .as(Project.QCABLES_COMPLETED)
                        )
                .from(PROJECT)
                .where(PROJECT.COMPLETION_DATE.isNull()
                        .and(PROJECT.ID.in(PostgresDSL
                                .select(USER_ACCESS.PROJECT)
                                .from(USER_ACCESS)
                                .where(USER_ACCESS.USERNAME.eq(username)))
                        .or(DBConnector.ADMIN_ROLE
                                .in(PostgresDSL
                                        .select(USER_ACCESS.PROJECT)
                                        .from(USER_ACCESS)
                                        .where(USER_ACCESS.USERNAME.eq(username)))))));
        for (Record result: results) {
            newList.add(new Project(result));
        }

        return newList;
    }

    public static void getActiveProjectsParams(HttpServerExchange hse) throws Exception {
        String username = hse.getRequestHeaders().get("X-Remote-User").element();
        ProjectService ps = new ProjectService();
        hse.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        hse.getResponseSender().send(ps.getActiveProjectsJson(ps.getActiveProjects(username)));
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
    public List<Project> getAll(String username) {
        List<Project> projects = new LinkedList<>();

        Result<Record> results = new DBConnector().fetch(
                PostgresDSL.select(PROJECT.asterisk(),
                        PostgresDSL.array(PostgresDSL
                                .select(DONOR_CASE.ID)
                                .from(DONOR_CASE)
                                .where(DONOR_CASE.PROJECT_ID.eq(PROJECT.ID)))
                                .as(Project.CASE_IDS),
                        PostgresDSL.array(PostgresDSL
                                .select(PROJECT_INFO_ITEM.ID)
                                .from(PROJECT_INFO_ITEM)
                                .where(PROJECT_INFO_ITEM.PROJECT_ID.eq(PROJECT.ID)))
                                .as(Project.INFO_ITEM_IDS),
                        PostgresDSL.array(PostgresDSL
                                .select(DELIVERABLE_FILE.ID)
                                .from(DELIVERABLE_FILE)
                                .where(DELIVERABLE_FILE.PROJECT_ID.eq(PROJECT.ID)))
                                .as(Project.DELIVERABLE_IDS))
                .from(PROJECT));

        for(Record result: results){
            projects.add(new Project(result));
        }
        return projects;
    }

    @Override
    public List<Project> search(String term, String username) {
        List<Project> projects = new LinkedList<>();
        DBConnector dbConnector = new DBConnector();
        Result<Record> results = dbConnector.fetch(PostgresDSL
                .select(PROJECT.asterisk(),
                        PostgresDSL.array(PostgresDSL
                                .select(DONOR_CASE.ID)
                                .from(DONOR_CASE)
                                .where(DONOR_CASE.PROJECT_ID.eq(PROJECT.ID)))
                                .as(Project.CASE_IDS),
                        PostgresDSL.array(PostgresDSL
                                .select(PROJECT_INFO_ITEM.ID)
                                .from(PROJECT_INFO_ITEM)
                                .where(PROJECT_INFO_ITEM.PROJECT_ID.eq(PROJECT.ID)))
                                .as(Project.INFO_ITEM_IDS),
                        PostgresDSL.array(PostgresDSL
                                .select(DELIVERABLE_FILE.ID)
                                .from(DELIVERABLE_FILE)
                                .where(DELIVERABLE_FILE.PROJECT_ID.eq(PROJECT.ID)))
                                .as(Project.DELIVERABLE_IDS),
                        PostgresDSL.field(PostgresDSL
                                .selectCount()
                                .from(DONOR_CASE)
                                .where(DONOR_CASE.PROJECT_ID.eq(PROJECT.ID)))
                                .as(Project.CASES_TOTAL),
                        PostgresDSL.field(PostgresDSL
                                .selectCount()
                                .from(PostgresDSL
                                        .selectDistinct(QCABLE.CASE_ID)
                                        .from(QCABLE)
                                        .where(QCABLE.CASE_ID.in(PostgresDSL
                                                .select(DONOR_CASE.ID)
                                                .from(DONOR_CASE)
                                                .where(DONOR_CASE.PROJECT_ID.eq(PROJECT.ID)))
                                                .and(QCABLE.QCABLE_TYPE.eq("final_report"))
                                                .and(QCABLE.STATUS.eq(DBConnector.QC_PASSED))
                                                // Can't use 'in' on text[] https://www.postgresql.org/docs/current/arrays.html
                                                .andExists(PostgresDSL
                                                        .select(DELIVERABLE_FILE.ID)
                                                        .from(DELIVERABLE_FILE)
                                                        .where(QCABLE.CASE_ID.eq(PostgresDSL.any(DELIVERABLE_FILE.CASE_ID)))))))
                                .as(Project.CASES_COMPLETED),
                        PostgresDSL.field(PostgresDSL
                                .selectCount()
                                .from(QCABLE)
                                .where(QCABLE.PROJECT_ID.eq(PROJECT.ID)))
                                .as(Project.QCABLES_TOTAL),
                        PostgresDSL.field(PostgresDSL
                                .selectCount()
                                .from(QCABLE)
                                .where(QCABLE.PROJECT_ID.eq(PROJECT.ID)
                                        .and(QCABLE.STATUS.eq(DBConnector.QC_PASSED))))
                                .as(Project.QCABLES_COMPLETED))
                .from(PROJECT)
                .where(PROJECT.ID.like("%"+term+"%")
                        .and(PROJECT.ID.in(PostgresDSL
                                .select(USER_ACCESS.PROJECT)
                                .from(USER_ACCESS)
                                .where(USER_ACCESS.USERNAME.eq(username)))
                        .or(DBConnector.ADMIN_ROLE.in(PostgresDSL
                                .select(USER_ACCESS.PROJECT)
                                .from(USER_ACCESS)
                                .where(USER_ACCESS.USERNAME.eq(username)))))));
        for(Record result: results){
            projects.add(new Project(result));
        }

        return projects;
    }


    @Override
    public String toJson(Collection<? extends SampuruType> toWrite, String username) throws Exception {
        return toJson(toWrite, false, username);
    }

    public String toJson(Collection<? extends SampuruType> toWrite, boolean expand, String username) throws Exception {
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
                for (ProjectInfoItem infoItem: project.getInfoItems(username)){
                    JSONObject infoItemObj = new JSONObject();
                    infoItemObj.put("id", infoItem.id);
                    infoItemObj.put("entry_type", infoItem.entryType);
                    infoItemObj.put("content", infoItem.content);
                    infoItemObj.put("expected", infoItem.expected == null? "null": infoItem.expected);
                    infoItemObj.put("received", infoItem.received == null? "null": infoItem.received);
                    infoItemsArray.add(infoItemObj);
                }
                jsonObject.put("info_items", infoItemsArray);

                jsonObject.put("donor_cases", new CaseService().toJson(project.getCases(username), true, username));
                jsonObject.put("deliverables", new DeliverableService().toJson(project.getDeliverables(username), username));
            } else {
                jsonObject.put("info_items", project.infoItems);
                jsonObject.put("donor_cases", project.donorCases);
                jsonObject.put("deliverables", project.deliverables);
            }

            jsonArray.add(jsonObject);
        }

        return jsonArray.toJSONString();
    }

    public String getProjectOverviewJson(Project subject, String username) throws Exception {
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
        for (ProjectInfoItem infoItem: subject.getInfoItems(username)){
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
        for (Deliverable deliverable: subject.getDeliverables(username)){
            JSONObject deliverableObj = new JSONObject();
            deliverableObj.put("id", deliverable.id);
            deliverableObj.put("expiry_date", JSONObject.escape(deliverable.expiryDate.toString()));
            deliverableObj.put("location", deliverable.location);
            deliverableObj.put("notes", deliverable.notes);
            infoItemsArray.add(deliverableObj);
        }
        jsonObject.put("deliverables", deliverablesArray);

        JSONArray failureArray = new JSONArray();
        for(QCable failedQCable: subject.getFailedQCables(username)){
            JSONObject failureObj = new JSONObject();
            failureObj.put("id", failedQCable.id);
            failureObj.put("alias", failedQCable.OICRAlias);
            failureObj.put("failure_reason", failedQCable.failureReason == null? "null": failedQCable.failureReason);
            failureArray.add(failureObj);
        }
        jsonObject.put("failures", failureArray);

        jsonObject.put("sankey_transitions", new DBConnector().getSankeyTransitions(subject.id, username));

        return jsonObject.toJSONString();
    }

    public static void getProjectOverviewParams(HttpServerExchange hse) throws Exception {
        String username = hse.getRequestHeaders().get("X-Remote-User").element();
        PathTemplateMatch ptm = hse.getAttachment(PathTemplateMatch.ATTACHMENT_KEY);
        String idparam = ptm.getParameters().get("id");
        ProjectService ps = new ProjectService();
        hse.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        // TODO: please refactor, i think this is the last holdout using get()
        hse.getResponseSender().send(ps.getProjectOverviewJson(ps.get(idparam, username), username));
    }
}
