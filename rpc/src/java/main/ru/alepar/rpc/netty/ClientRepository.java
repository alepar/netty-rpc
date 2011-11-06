package ru.alepar.rpc.netty;

import org.jboss.netty.channel.Channel;
import ru.alepar.rpc.Client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class ClientRepository {
    
    private final Map<Client.Id, NettyClient> clients = new ConcurrentHashMap<Client.Id, NettyClient>();

    public synchronized void addClient(NettyClient client) {
        clients.put(client.getId(), client);
    }

    public synchronized void removeClient(NettyClient client) {
        clients.remove(client.getId());
    }

    public Collection<Channel> getChannels() {
        List<Channel> channels = new ArrayList<Channel>(clients.size());
        for (NettyClient client : clients.values()) {
            channels.add(client.getChannel());
        }
        return channels;
    }

    public NettyClient getClient(Client.Id clientId) {
        return clients.get(clientId);
    }
}
