package service;

import type.SampuruType;
import java.util.List;

public class Service<T extends SampuruType> {
    Class<T> targetClass;

    public Service(Class<T> newTarget){
        targetClass = newTarget;
    }

    public T get(int id){
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public List<T> getAll(){
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public List<T> search(String term){
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
