package ru.alepar.bus.impl;

import ru.alepar.bus.api.Bus;
import ru.alepar.bus.api.Key;
import ru.alepar.bus.api.Message;
import ru.alepar.bus.api.MessageListener;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;

public class FullBroadcastingBus implements Bus {

    private final Map<Class<? extends Message>, Collection> listenerMap = new ConcurrentHashMap<Class<? extends Message>, Collection>();

    @Override
    public <T extends Message> void addListener(Key<T> key, MessageListener<T> listener) {
        listenersForKey(key).add(listener);
    }

    @Override
    public <T extends Message> void send(Key<T> key, T msg) {
        Collection<MessageListener<T>> listeners = listenersForKey(key);
        for (MessageListener<T> listener : listeners) {
            listener.onMessage(new Context<T>(this, key, msg));
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends Message> Collection<MessageListener<T>> listenersForKey(Key<T> key) {
        Collection<MessageListener<T>> listeners = (Collection<MessageListener<T>>) listenerMap.get(key.getClazz());
        if(listeners == null) {
            listeners = new CopyOnWriteArrayList<MessageListener<T>>();
            listenerMap.put(key.getClazz(), listeners);
        }
        
        return listeners;
    }

    @Override
    public <T extends Message> Action scheduleSend(Key<T> key, T msg) {
        return new Action<T>(this, key, msg);
    }

    @Override
    public <T extends Message> Future<T> waitFor(Key<T> key) {
        final FutureMessageListener<T> listener = new FutureMessageListener<T>();
        addListener(key, listener);
        return listener;
    }
}
