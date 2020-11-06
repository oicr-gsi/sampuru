package ca.on.oicr.gsi.sampuru.server.service;

import ca.on.oicr.gsi.sampuru.server.DBConnector;
import ca.on.oicr.gsi.sampuru.server.type.Case;
import ca.on.oicr.gsi.sampuru.server.type.Notification;
import ca.on.oicr.gsi.sampuru.server.type.SampuruType;
import io.undertow.server.HttpServerExchange;
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
    public List<Notification> getAll() {
        throw new UnsupportedOperationException("implement me"); //TODO implement me
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
}
