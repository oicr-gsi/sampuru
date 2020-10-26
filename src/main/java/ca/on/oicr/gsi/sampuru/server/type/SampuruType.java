package ca.on.oicr.gsi.sampuru.server.type;

import org.jooq.Table;

import java.util.List;

public abstract class SampuruType<T> {
    public static Table TABLE_NAME;
    public int id;

    public static List<? extends SampuruType> getAll(){
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public static List<? extends SampuruType> search(String term){
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
