package ru.alepar.rpc.server;

import org.jboss.netty.channel.Channel;
import ru.alepar.rpc.api.ImplementationFactory;
import ru.alepar.rpc.common.NettyRemote;

public class FactoryServerProvider<T> implements ServerProvider<T> {

    private final ImplementationFactory<? extends T> factory;

    public FactoryServerProvider(ImplementationFactory<? extends T> factory) {
        this.factory = factory;
    }

    @Override
    public T provideFor(Channel channel) {
        return factory.create(new NettyRemote(channel));
    }
}
