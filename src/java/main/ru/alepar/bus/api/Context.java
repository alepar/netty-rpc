package ru.alepar.bus.api;

public interface Context<T extends Message> {
    
    Key<T> key();

    T message();

    <M extends Message> void respondWith(Class<M> clazz, M response);
    
}
