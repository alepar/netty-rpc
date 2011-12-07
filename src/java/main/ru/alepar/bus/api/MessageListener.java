package ru.alepar.bus.api;

public interface MessageListener<T extends Message> {
    void onMessage(Context<T> ctx);
}
