package ca.on.oicr.gsi.sampuru.server.service;

import ca.on.oicr.gsi.sampuru.server.DBConnector;
import ca.on.oicr.gsi.sampuru.server.Server;
import ca.on.oicr.gsi.sampuru.server.type.Notification;
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

import static tables_generated.Tables.NOTIFICATION;


public class NotificationService extends Service<Notification> {

    public NotificationService(){
        super(Notification.class);
    }

    public static void getIdParams(HttpServerExchange hse) throws Exception {
        getIdParams(new NotificationService(), hse);
    }

    public static void getAllParams(HttpServerExchange hse) throws Exception {
        String username = Server.getUsername(hse);
        NotificationService ns = new NotificationService();
        Server.sendHTTPResponse(hse, ns.toJson(ns.getAll(username), username));
    }

    @Override
    public List<Notification> getAll(String username) throws Exception {
        List<Notification> notifications = new LinkedList<>();

        Result<Record> results = new DBConnector().fetch(PostgresDSL.select().from(NOTIFICATION).where(NOTIFICATION.USER_ID.eq(username)));

        for(Record result: results){
            notifications.add(new Notification(result));
        }

        return notifications;
    }

    @Override
    public List<Notification> search(String term, String username) throws SQLException {
        List<Notification> notifications = new LinkedList<>();
        DBConnector dbConnector = new DBConnector();
        Result<Record> results = dbConnector.fetch(PostgresDSL
                .select()
                .from(NOTIFICATION
                .where(NOTIFICATION.CONTENT.like("%"+term+"%")
                        .and(NOTIFICATION.USER_ID.eq(username)))));
        for(Record result: results){
            notifications.add(new Notification(result));
        }

        return notifications;
    }

    @Override
    public String toJson(Collection<? extends SampuruType> toWrite, String username){
        JSONArray jsonArray = new JSONArray();

        for (SampuruType item: toWrite){
            Notification notificationItem = (Notification)item;
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", notificationItem.id);
            jsonObject.put("issue_date", JSONObject.escape(notificationItem.issueDate.format(ServiceUtils.DATE_TIME_FORMATTER)));
            jsonObject.put("resolved_date", notificationItem.resolvedDate == null? "null": JSONObject.escape(notificationItem.resolvedDate.format(ServiceUtils.DATE_TIME_FORMATTER)));
            jsonObject.put("content", notificationItem.content);
            jsonArray.add(jsonObject);
        }

        return jsonArray.toJSONString();
    }

    public static void getActiveParams(HttpServerExchange hse) throws SQLException {
        String username = Server.getUsername(hse);
        NotificationService ns = new NotificationService();
        Server.sendHTTPResponse(hse, ns.toJson(ns.getActiveNotifications(username), username));
    }

    private List<Notification> getActiveNotifications(String username) throws SQLException {
        Result<Record> results = new DBConnector().fetch(
                PostgresDSL.select()
                .from(NOTIFICATION)
                .where(NOTIFICATION.RESOLVED_DATE.isNull().and(NOTIFICATION.USER_ID.eq(username))));
        List<Notification> newList = new LinkedList<>();
        for(Record result: results){
            newList.add(new Notification(result));
        }
        return newList;
    }
}
