package ca.on.oicr.gsi.sampuru.server.service;

import ca.on.oicr.gsi.sampuru.server.type.Deliverable;
import ca.on.oicr.gsi.sampuru.server.type.SampuruType;
import io.undertow.server.HttpServerExchange;

import java.util.Collection;

public class DeliverableService extends Service<Deliverable> {

    public DeliverableService(){
        super(Deliverable.class);
    }

    public static void getIdParams(HttpServerExchange hse) throws Exception {
        getIdParams(new DeliverableService(), hse);
    }

    public static void getAllParams(HttpServerExchange hse) throws Exception {
        getAllParams(new DeliverableService(), hse);
    }

    // TODO implement
    public String toJson(Collection<? extends SampuruType> toWrite){
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
