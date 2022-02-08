package ca.on.oicr.gsi.sampuru.server.service;

import ca.on.oicr.gsi.sampuru.server.DBConnector;
import ca.on.oicr.gsi.sampuru.server.Server;
import ca.on.oicr.gsi.sampuru.server.type.ChangelogEntry;
import ca.on.oicr.gsi.sampuru.server.type.SampuruType;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.PathTemplateMatch;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.util.postgres.PostgresDSL;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static tables_generated.Tables.*;

public class ChangelogService extends Service<ChangelogEntry> {

    public ChangelogService(){
        super(ChangelogEntry.class);
    }

    public static void getIdParams(HttpServerExchange hse) throws Exception {
        getIdParams(new ChangelogService(), hse);
    }

    public static void getAllParams(HttpServerExchange hse) throws Exception {
        getAllParams(new ChangelogService(), hse);
    }

    public org.jooq.SelectOnConditionStep<org.jooq.Record> baseChangelogQuery() {
        return PostgresDSL
            .select(
                CHANGELOG.asterisk(),
                QCABLE.QCABLE_TYPE,
                QCABLE.OICR_ALIAS,
                DONOR_CASE.NAME)
            .from(CHANGELOG)
            .leftJoin(QCABLE).on(QCABLE.ID.eq(CHANGELOG.QCABLE_ID))
            .join(DONOR_CASE).on(DONOR_CASE.ID.eq(CHANGELOG.CASE_ID));
    }

    @Override
    public List<ChangelogEntry> getAll(String username) throws Exception {
        List<ChangelogEntry> changelogs = new LinkedList<>();

        Result<Record> results = new DBConnector().fetch(
                baseChangelogQuery());

        for (Record result: results){
            changelogs.add(new ChangelogEntry(result));
        }
        return changelogs;
    }

    @Override
    public List<ChangelogEntry> search(String term, String username) throws SQLException {
        List<ChangelogEntry> changelogEntries = new LinkedList<>();
        DBConnector dbConnector = new DBConnector();
        Result<Record> results = dbConnector.fetch(baseChangelogQuery()
                .where(CHANGELOG.CONTENT.like("%"+term+"%")
                        .and(CHANGELOG.CASE_ID.in(PostgresDSL
                                .select(DONOR_CASE.ID)
                                .from(DONOR_CASE)
                                .where(DONOR_CASE.PROJECT_ID.in(
                                    PostgresDSL
                                            .select(USER_ACCESS.PROJECT)
                                            .from(USER_ACCESS)
                                            .where(USER_ACCESS.USERNAME.eq(username))))
                        .or(DBConnector.ADMIN_ROLE.in(PostgresDSL
                                .select(USER_ACCESS.PROJECT)
                                .from(USER_ACCESS)
                                .where(USER_ACCESS.USERNAME.eq(username))))))));
        for(Record result: results){
            changelogEntries.add(new ChangelogEntry(result));
        }

        return changelogEntries;
    }

    @Override
    public String toJson(Collection<? extends SampuruType> toWrite, String username){
        JSONArray jsonArray = new JSONArray();

        for(SampuruType item: toWrite){
            JSONObject jsonObject = new JSONObject();
            ChangelogEntry changelogEntry = (ChangelogEntry) item;

            jsonObject.put("id", changelogEntry.id);
            jsonObject.put("content", changelogEntry.content);
            jsonObject.put("change_date", JSONObject.escape(changelogEntry.changeDate.format(ServiceUtils.DATE_TIME_FORMATTER)));

            jsonArray.add(jsonObject);
        }

        return jsonArray.toJSONString();
    }

    public static void getFilteredChangelogs(HttpServerExchange hse) throws Exception {
        String username = Server.getUsername(hse);
        ChangelogService cs = new ChangelogService();
        PathTemplateMatch ptm = hse.getAttachment(PathTemplateMatch.ATTACHMENT_KEY);
        String filterType = ptm.getParameters().get("filterType");
        String filterId = ptm.getParameters().get("filterId");
        String responseBody;
        switch(filterType) {
            case "project":
                responseBody = cs.getChangelogsByProject(filterId, username).toJSONString();
                break;
            case "case":
                responseBody = cs.getChangelogsByCase(filterId, username).toJSONString();
                break;
            case "qcable":
                responseBody = cs.getChangelogsByQcable(filterId, username).toJSONString();
                break;
            default:
                throw new UnsupportedOperationException("Bad filter type " + filterType +
                        " , supported types are: project, case, qcable");
        }
        Server.sendHTTPResponse(hse, responseBody);
    }

    public JSONArray getChangelogsByProject(String projectId, String username) throws SQLException {
        return buildChangelogsTable(new DBConnector().fetch(baseChangelogQuery()
                .where(CHANGELOG.PROJECT_ID.eq(projectId)
                        .and(CHANGELOG.PROJECT_ID.in(PostgresDSL
                                .select(USER_ACCESS.PROJECT)
                                .from(USER_ACCESS)
                                .where(USER_ACCESS.USERNAME.eq(username)))
                        .or(DBConnector.ADMIN_ROLE.in(PostgresDSL
                                .select(USER_ACCESS.PROJECT)
                                .from(USER_ACCESS)
                                .where(USER_ACCESS.USERNAME.eq(username))))))
            .orderBy(CHANGELOG.CHANGE_DATE.desc())));
    }

    public JSONArray getChangelogsByCase(String caseId, String username) throws SQLException {
        return buildChangelogsTable(new DBConnector().fetch(baseChangelogQuery()
                .where(CHANGELOG.CASE_ID.eq(caseId)
                        .and(CHANGELOG.PROJECT_ID.in(PostgresDSL
                                .select(USER_ACCESS.PROJECT)
                                .from(USER_ACCESS)
                                .where(USER_ACCESS.USERNAME.eq(username)))
                                .or(DBConnector.ADMIN_ROLE.in(PostgresDSL
                                        .select(USER_ACCESS.PROJECT)
                                        .from(USER_ACCESS)
                                        .where(USER_ACCESS.USERNAME.eq(username))))))
            .orderBy(CHANGELOG.CHANGE_DATE.desc())));
    }

    public JSONArray getChangelogsByQcable(String qcableId, String username) throws SQLException {
        return buildChangelogsTable(new DBConnector().fetch(baseChangelogQuery()
                .where(CHANGELOG.QCABLE_ID.eq(qcableId)
                        .and(CHANGELOG.PROJECT_ID.in(PostgresDSL
                                .select(USER_ACCESS.PROJECT)
                                .from(USER_ACCESS)
                                .where(USER_ACCESS.USERNAME.eq(username)))
                                .or(DBConnector.ADMIN_ROLE.in(PostgresDSL
                                        .select(USER_ACCESS.PROJECT)
                                        .from(USER_ACCESS)
                                        .where(USER_ACCESS.USERNAME.eq(username))))))
            .orderBy(CHANGELOG.CHANGE_DATE.desc())));
    }

    private JSONArray buildChangelogsTable(Result<Record> result) {
        JSONArray jsonArray = new JSONArray();
        for(Record row: result) {
            JSONObject jsonObject = new JSONObject();

            jsonObject.put("project_id", row.get(CHANGELOG.PROJECT_ID));
            jsonObject.put("case_id", row.get(CHANGELOG.CASE_ID));
            jsonObject.put("qcable_id", row.get(CHANGELOG.QCABLE_ID) == null ? "null": row.get(CHANGELOG.QCABLE_ID));
            jsonObject.put("qcable_type", row.get(QCABLE.QCABLE_TYPE) == null ? "null": row.get(QCABLE.QCABLE_TYPE));
            jsonObject.put("qcable_oicr_alias", row.get(QCABLE.OICR_ALIAS) == null ? "null": row.get(QCABLE.OICR_ALIAS));
            jsonObject.put("external_name", row.get(DONOR_CASE.NAME)); // case and qcable have the same external name, better to get the external name from donor case because a donor case changelog won't have a qcable ID
            jsonObject.put("change_date", row.get(CHANGELOG.CHANGE_DATE).format(ServiceUtils.DATE_TIME_FORMATTER));
            jsonObject.put("content", row.get(CHANGELOG.CONTENT));

            jsonArray.add(jsonObject);
        }
        return jsonArray;
    }
}

