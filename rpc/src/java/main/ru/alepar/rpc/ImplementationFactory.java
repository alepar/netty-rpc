package ru.alepar.rpc;

public interface ImplementationFactory<T> {
    T create(ClientId clientId, ProxyFactory proxyFactory);
}
