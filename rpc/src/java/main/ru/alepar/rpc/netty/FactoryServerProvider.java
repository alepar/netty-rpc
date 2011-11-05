package ru.alepar.rpc.netty;

import org.jboss.netty.channel.Channel;
import ru.alepar.rpc.ImplementationFactory;

class FactoryServerProvider<T> implements ServerProvider<T> {

    private final ImplementationFactory<? extends T> factory;

    public FactoryServerProvider(ImplementationFactory<? extends T> factory) {
        this.factory = factory;
    }

    @Override
    public T provideFor(Channel channel) {
        return factory.create(new NettyClientId(channel), new FeedbackProxyFactory(channel));
    }
}
