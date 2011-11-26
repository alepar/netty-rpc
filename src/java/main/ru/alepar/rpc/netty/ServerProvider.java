package ru.alepar.rpc.netty;

import org.jboss.netty.channel.Channel;

public interface ServerProvider<T> {
    
    T provideFor(Channel channel);
    
}
