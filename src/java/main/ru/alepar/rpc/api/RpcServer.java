package ru.alepar.rpc.api;

import java.util.Collection;

public interface RpcServer {

    Remote getClient(Remote.Id clientId);

    void shutdown();

    Collection<Remote> getClients();

}
