package ru.alepar.rpc;

public interface RpcClient extends ProxyFactory {

    <T> void addImplementation(Class<T> interfaceClass, T implObject);

    void shutdown();

}
