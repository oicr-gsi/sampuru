package ca.on.oicr.gsi.sampuru.server.service;

import ca.on.oicr.gsi.sampuru.server.DBConnector;
import ca.on.oicr.gsi.sampuru.server.type.ChangelogEntry;
import ca.on.oicr.gsi.sampuru.server.type.SampuruType;
import io.undertow.server.HttpServerExchange;
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

    @Override
    public List<ChangelogEntry> getAll(String username) throws Exception {
        List<ChangelogEntry> changelogs = new LinkedList<>();

        Result<Record> results = new DBConnector().fetch(
                PostgresDSL.select()
                .from(CHANGELOG));

        for (Record result: results){
            changelogs.add(new ChangelogEntry(result));
        }
        return changelogs;
    }

    @Override
    public List<ChangelogEntry> search(String term, String username) throws SQLException {
        List<ChangelogEntry> changelogEntries = new LinkedList<>();
        DBConnector dbConnector = new DBConnector();
        Result<Record> results = dbConnector.fetch(PostgresDSL
                .select()
                .from(CHANGELOG)
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
            jsonObject.put("change_date", JSONObject.escape(changelogEntry.changeDate.toString()));

            jsonArray.add(jsonObject);
        }

        return jsonArray.toJSONString();
    }
}
