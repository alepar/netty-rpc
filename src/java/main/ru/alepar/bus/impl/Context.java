package ru.alepar.bus.impl;

import ru.alepar.bus.api.Bus;
import ru.alepar.bus.api.Key;
import ru.alepar.bus.api.Message;

class Context<T extends Message> implements ru.alepar.bus.api.Context<T> {

    private final Bus bus;
    private final Key<T> key;
    private final T msg;

    public Context(Bus bus, Key<T> key, T msg) {
        this.bus = bus;
        this.key = key;
        this.msg = msg;
    }

    @Override
    public Key<T> key() {
        return key;
    }

    @Override
    public T message() {
        return msg;
    }

    @Override
    public <M extends Message> void respondWith(Class<M> clazz, M response) {
        bus.send(key.deriveResponse(clazz), response);
    }
}
