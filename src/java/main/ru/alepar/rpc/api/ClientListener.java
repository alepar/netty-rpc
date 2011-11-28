package ru.alepar.rpc.api;

public interface ClientListener {

    /**
     * called when client connect event happens
     * @param remote associated with connected client
     */
    void onClientConnect(Remote remote);

    /**
     * called when client disconnect event happens
     * @param remote associated with disconnected client
     */
    void onClientDisconnect(Remote remote);
}
