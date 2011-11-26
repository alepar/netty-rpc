package ru.alepar.rpc.api;

import java.util.Collection;

public interface RpcServer {

    Remote getClient(Remote.Id clientId);

    void shutdown();

    Collection<Remote> getClients();

    public interface ExceptionListener {
        void onExceptionCaught(Remote remote, Exception e);
    }

    public interface ClientListener {
        void onClientConnect(Remote remote);

        void onClientDisconnect(Remote remote);
    }
}
