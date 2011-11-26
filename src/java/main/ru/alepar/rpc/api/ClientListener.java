package ru.alepar.rpc.api;

public interface ClientListener {
    void onClientConnect(Remote remote);

    void onClientDisconnect(Remote remote);
}
