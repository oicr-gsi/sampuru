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
        getAllParams(new CaseService(), hse);
    }

    public static void getAllParams(HttpServerExchange hse){
        getAllParams(new CaseService(), hse);
    }
}
