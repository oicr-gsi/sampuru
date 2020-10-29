package ca.on.oicr.gsi.sampuru.server.service;

import ca.on.oicr.gsi.sampuru.server.type.Case;
import io.undertow.server.HttpServerExchange;

public class CaseService extends Service {

    public CaseService(){
        super(Case.class);
    }

    public Case get(String name){
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public static void getIdParams(HttpServerExchange hse){
        getIdParams(hse);
    }

    public static void getAllParams(HttpServerExchange hse) throws Exception {
        getAllParams(new CaseService(), hse);
    }
}
