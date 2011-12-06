package ru.alepar.bus;

public interface MessageListener<T extends Message> {
    void onMessage(T msg);
}
