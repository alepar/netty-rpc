package ru.alepar.rpc.server;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ru.alepar.rpc.api.Remote;
import ru.alepar.rpc.common.NettyRemote;

import static java.util.Collections.unmodifiableCollection;

class ClientRepository {
    
    private final Map<Remote.Id, NettyRemote> clients = new ConcurrentHashMap<Remote.Id, NettyRemote>();

    public void addClient(NettyRemote remote) {
        clients.put(remote.getId(), remote);
    }

    public void removeClient(NettyRemote remote) {
        clients.remove(remote.getId());
    }

    public NettyRemote getClient(Remote.Id clientId) {
        return clients.get(clientId);
    }

    public Collection<NettyRemote> getClients() {
        return unmodifiableCollection(clients.values());
    }
}
