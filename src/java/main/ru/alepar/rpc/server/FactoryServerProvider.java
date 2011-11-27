package ru.alepar.rpc.server;

import ru.alepar.rpc.api.ImplementationFactory;
import ru.alepar.rpc.api.Remote;

public class FactoryServerProvider<T> implements ServerProvider<T> {

    private final ImplementationFactory<? extends T> factory;

    public FactoryServerProvider(ImplementationFactory<? extends T> factory) {
        this.factory = factory;
    }

    @Override
    public T provideFor(Remote remote) {
        return factory.create(remote);
    }
}
