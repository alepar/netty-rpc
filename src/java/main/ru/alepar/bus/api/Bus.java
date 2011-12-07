package ru.alepar.bus.api;

import java.util.concurrent.Future;

public interface Bus {

    <T extends Message> void addListener(Key<T> key, MessageListener<T> listener);

    <T extends Message> void send(Key<T> key, T msg);

    <T extends Message> Action scheduleSend(Key<T> key, T msg);

    <T extends Message> Future<T> waitFor(Key<T> mKey);
}
