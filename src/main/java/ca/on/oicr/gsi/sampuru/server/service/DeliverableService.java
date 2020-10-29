package ca.on.oicr.gsi.sampuru.server.service;

import ca.on.oicr.gsi.sampuru.server.type.Deliverable;
import io.undertow.server.HttpServerExchange;

public class DeliverableService extends Service {

    public DeliverableService(){
        super(Deliverable.class);
    }

    public static void getIdParams(HttpServerExchange hse) throws Exception {
        getIdParams(new DeliverableService(), hse);
    }

    public static void getAllParams(HttpServerExchange hse) throws Exception {
        getAllParams(new DeliverableService(), hse);
    }
}
