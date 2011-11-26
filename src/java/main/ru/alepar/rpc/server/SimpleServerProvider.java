package ru.alepar.rpc.server;

import org.jboss.netty.channel.Channel;

public class SimpleServerProvider<T> implements ServerProvider<T> {
    private final T impl;

    public SimpleServerProvider(T impl) {
        this.impl = impl;
    }

    @Override
    public T provideFor(Channel channel) {
        return impl;
    }
}
