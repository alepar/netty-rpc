package ru.alepar.rpc.netty;

import org.jboss.netty.channel.Channel;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

class ClientRepository {
    
    private final Set<NettyClient> clients = Collections.newSetFromMap(new ConcurrentHashMap<NettyClient, Boolean>());

    public synchronized void addClient(NettyClient client) {
        clients.add(client);
    }

    public synchronized void removeClient(NettyClient client) {
        clients.remove(client);
    }

    public Collection<Channel> getChannels() {
        List<Channel> channels = new ArrayList<Channel>(clients.size());
        for (NettyClient client : clients) {
            channels.add(client.getChannel());
        }
        return channels;
    }
    
}
