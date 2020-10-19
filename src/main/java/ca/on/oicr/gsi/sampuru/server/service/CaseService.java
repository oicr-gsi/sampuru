package service;

import type.Case;

public class CaseService extends Service {

    public CaseService(){
        super(Case.class);
    }

    public Case get(String name){
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
