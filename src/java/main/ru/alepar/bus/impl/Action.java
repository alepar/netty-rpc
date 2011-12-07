package ru.alepar.bus.impl;

import ru.alepar.bus.api.Bus;
import ru.alepar.bus.api.Key;
import ru.alepar.bus.api.Message;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

class Action<T extends Message> implements ru.alepar.bus.api.Action {

    private final Bus bus;
    private final Key<T> key;
    private final T msg;
    
    Action(Bus bus, Key<T> key, T msg) {
        this.bus = bus;
        this.key = key;
        this.msg = msg;
    }

    @Override
    public <M extends Message> M waitForResponse(Class<M> responseClass) throws InterruptedException, ExecutionException {
        final Key<T> requestKey = key.deriveRequest();
        final Future<M> future = bus.waitFor(requestKey.deriveResponse(responseClass));
        bus.send(requestKey, msg);
        return future.get();
    }

}
