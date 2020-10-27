package ca.on.oicr.gsi.sampuru.server.service;

import ca.on.oicr.gsi.sampuru.server.type.QCable;
import io.undertow.server.HttpServerExchange;

public class QCableService extends Service {

    public QCableService(){
        super(QCable.class);
    }

    public QCable get(String alias){
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public static void getIdParams(HttpServerExchange hse) throws Exception {
        getIdParams(new QCableService(), hse);
    }

    public static void getAllParams(HttpServerExchange hse){
        getAllParams(new QCableService(), hse);
    }

}
