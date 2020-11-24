package ca.on.oicr.gsi.sampuru.server.service;

import ca.on.oicr.gsi.sampuru.server.DBConnector;
import ca.on.oicr.gsi.sampuru.server.type.Notification;
import ca.on.oicr.gsi.sampuru.server.type.SampuruType;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static tables_generated.Tables.*;


public class NotificationService extends Service<Notification> {

    public NotificationService(){
        super(Notification.class);
    }

    public static void getIdParams(HttpServerExchange hse) throws Exception {
        getIdParams(new NotificationService(), hse);
    }

    public static void getAllParams(HttpServerExchange hse) throws Exception {
        getAllParams(new NotificationService(), hse);
    }

    @Override
    public List<Notification> getAll() throws Exception {
        DSLContext context = new DBConnector().getContext();
        List<Notification> notifications = new LinkedList<>();

        Result<Record> results = context.select().from(NOTIFICATION).fetch();

        for(Record result: results){
            notifications.add(new Notification(result));
        }

        return notifications;
    }

    @Override
    public List<Notification> search(String term) throws Exception {
        List<Integer> ids = new DBConnector().search(NOTIFICATION, NOTIFICATION.ID, NOTIFICATION.CONTENT, term);
        List<Notification> notifications = new LinkedList<>();

        for (Integer id: ids){
            notifications.add(get(id));
        }

        return notifications;
    }

    @Override
    public String toJson(Collection<? extends SampuruType> toWrite){
        JSONArray jsonArray = new JSONArray();

        for (SampuruType item: toWrite){
            Notification notificationItem = (Notification)item;
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", notificationItem.id);
            jsonObject.put("issue_date", JSONObject.escape(notificationItem.issueDate.toString()));
            jsonObject.put("resolved_date", notificationItem.resolvedDate == null? "null": JSONObject.escape(notificationItem.resolvedDate.toString()));
            jsonObject.put("content", notificationItem.content);
            jsonArray.add(jsonObject);
        }

        return jsonArray.toJSONString();
    }

    public static void getActiveParams(HttpServerExchange hse) {
        NotificationService ns = new NotificationService();
        hse.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        hse.getResponseSender().send(ns.toJson(ns.getActiveNotifications()));
    }

    private List<Notification> getActiveNotifications() {
        Result<Record> results = new DBConnector().getContext()
                .select()
                .from(NOTIFICATION)
                .where(NOTIFICATION.RESOLVED_DATE.isNull())
                .fetch();
        List<Notification> newList = new LinkedList<>();
        for(Record result: results){
            newList.add(new Notification(result));
        }
        return newList;
    }
}
