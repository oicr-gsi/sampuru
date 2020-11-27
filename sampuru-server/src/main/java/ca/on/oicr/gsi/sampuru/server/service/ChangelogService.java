package ca.on.oicr.gsi.sampuru.server.service;

import ca.on.oicr.gsi.sampuru.server.DBConnector;
import ca.on.oicr.gsi.sampuru.server.type.ChangelogEntry;
import ca.on.oicr.gsi.sampuru.server.type.SampuruType;
import io.undertow.server.HttpServerExchange;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import tables_generated.tables.Changelog;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

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
    public List<ChangelogEntry> getAll() throws Exception {
        DSLContext context = new DBConnector().getContext();
        List<ChangelogEntry> changelogs = new LinkedList<>();

        Result<Record> results = context
                .select()
                .from(CHANGELOG)
                .fetch();

        for (Record result: results){
            changelogs.add(new ChangelogEntry(result));
        }
        return changelogs;
    }

    @Override
    public List<ChangelogEntry> search(String term) throws Exception{
        List<Integer> ids = new DBConnector().search(CHANGELOG, CHANGELOG.ID, CHANGELOG.CONTENT, term).stream().map(o -> (Integer)o).collect(Collectors.toList());
        List<ChangelogEntry> changelogs = new LinkedList<>();

        for (Integer id: ids){
            changelogs.add(get(id));
        }

        return changelogs;
    }

    @Override
    public String toJson(Collection<? extends SampuruType> toWrite){
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
