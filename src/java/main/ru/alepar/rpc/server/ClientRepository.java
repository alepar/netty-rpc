package ru.alepar.rpc.server;

import org.jboss.netty.channel.Channel;
import ru.alepar.rpc.api.Remote;
import ru.alepar.rpc.common.NettyRemote;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class ClientRepository {
    
    private final Map<Remote.Id, NettyRemote> clients = new ConcurrentHashMap<Remote.Id, NettyRemote>();

    public void addClient(NettyRemote remote) {
        clients.put(remote.getId(), remote);
    }

    public void removeClient(NettyRemote remote) {
        clients.remove(remote.getId());
    }

    public Collection<Channel> getChannels() {
        List<Channel> channels = new ArrayList<Channel>(clients.size());
        for (NettyRemote remote : clients.values()) {
            channels.add(remote.getChannel());
        }
        return channels;
    }

    public NettyRemote getClient(Remote.Id clientId) {
        return clients.get(clientId);
    }

    public Collection<Remote> getClients() {
        Collection<Remote> result = new ArrayList<Remote>(clients.size());
        for (NettyRemote remote : clients.values()) {
            result.add(remote);
        }
        return result;
    }
}
