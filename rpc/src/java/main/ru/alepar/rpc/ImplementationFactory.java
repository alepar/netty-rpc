package ru.alepar.rpc;

public interface ImplementationFactory<T> {
    T create(ProxyFactory proxyFactory);
}
