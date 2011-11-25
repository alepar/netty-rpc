package ru.alepar.rpc.netty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.netty.channel.Channel;
import ru.alepar.rpc.Remote;

class ClientRepository {
    
    private final Map<Remote.Id, NettyClient> clients = new ConcurrentHashMap<Remote.Id, NettyClient>();

    public void addClient(NettyClient client) {
        clients.put(client.getId(), client);
    }

    public void removeClient(NettyClient client) {
        clients.remove(client.getId());
    }

    public Collection<Channel> getChannels() {
        List<Channel> channels = new ArrayList<Channel>(clients.size());
        for (NettyClient client : clients.values()) {
            channels.add(client.getChannel());
        }
        return channels;
    }

    public NettyClient getClient(Remote.Id clientId) {
        return clients.get(clientId);
    }

    public Collection<Remote> getClients() {
        Collection<Remote> result = new ArrayList<Remote>(clients.size());
        for (NettyClient client : clients.values()) {
            result.add(client);
        }
        return result;
    }
}
