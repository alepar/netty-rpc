package ru.alepar.rpc.server;

import org.jboss.netty.channel.Channel;

public interface ServerProvider<T> {
    
    T provideFor(Channel channel);
    
}
