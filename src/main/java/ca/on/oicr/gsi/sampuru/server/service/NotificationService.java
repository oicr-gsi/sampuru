package ca.on.oicr.gsi.sampuru.server.service;

import ca.on.oicr.gsi.sampuru.server.type.Notification;
import io.undertow.server.HttpServerExchange;

public class NotificationService extends Service {

    public NotificationService(){
        super(Notification.class);
    }

    public static void getIdParams(HttpServerExchange hse) throws Exception {
        getIdParams(new NotificationService(), hse);
    }

    public static void getAllParams(HttpServerExchange hse){
        getAllParams(new NotificationService(), hse);
    }
}
