package ru.alepar.bus;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Bus {

    private final Map<Class<? extends Message>, Collection> listenerMap = new ConcurrentHashMap<Class<? extends Message>, Collection>();

    public <T extends Message> void addListener(Key<T> key, MessageListener<T> listener) {
        listenersForKey(key).add(listener);
    }

    public <T extends Message> void send(Key<T> key, T msg) {
        Collection<MessageListener<T>> listeners = listenersForKey(key);
        for (MessageListener<T> listener : listeners) {
            listener.onMessage(msg);
        }
    }

    private <T extends Message> Collection<MessageListener<T>> listenersForKey(Key<T> key) {
        Collection<MessageListener<T>> listeners = (Collection<MessageListener<T>>) listenerMap.get(key.getClazz());
        if(listeners == null) {
            listeners = new CopyOnWriteArrayList<MessageListener<T>>();
            listenerMap.put(key.getClazz(), listeners);
        }
        
        return listeners;
    }
}
