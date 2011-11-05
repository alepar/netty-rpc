package ru.alepar.rpc.netty;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.util.internal.ConcurrentIdentityHashMap;
import ru.alepar.rpc.ClientId;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

class ClientRepository {
    
    private final ConcurrentMap<ClientId, Channel> idToChannel = new ConcurrentHashMap<ClientId, Channel>();
    private final ConcurrentMap<Channel, ClientId> channelToId = new ConcurrentIdentityHashMap<Channel, ClientId>();

    public synchronized void addClient(ClientId clientId, Channel channel) {
        idToChannel.put(clientId, channel);
        channelToId.put(channel, clientId);
    }

    public synchronized void removeClient(ClientId clientId) {
        Channel channel = idToChannel.remove(clientId);
        channelToId.remove(channel);
    }

    public Collection<Channel> getChannels() {
        return channelToId.keySet();
    }
    
}
