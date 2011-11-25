package ru.alepar.rpc.netty;

import org.jboss.netty.channel.Channel;

interface ServerProvider<T> {
    
    T provideFor(Channel channel);
    
}
