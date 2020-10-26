package ca.on.oicr.gsi.sampuru.server.service;

import ca.on.oicr.gsi.sampuru.server.type.SampuruType;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class Service<T extends SampuruType> {
    private Class<T> targetClass;

    public Service(Class<T> newTarget){
        targetClass = newTarget;
    }

    public T get(int id) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        return targetClass.getDeclaredConstructor(Integer.class).newInstance(id);
    }

    public List<T> getAll() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        return (List<T>)targetClass.getDeclaredMethod("getAll").invoke(null);
    }

    public List<T> search(String term){
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
