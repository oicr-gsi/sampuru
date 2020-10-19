package ca.on.oicr.gsi.sampuru.server.service;

import ca.on.oicr.gsi.sampuru.server.type.QCable;

public class QCableService extends Service {

    public QCableService(){
        super(QCable.class);
    }

    public QCable get(String alias){
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
