package ru.alepar.rpc;

public interface RpcClient {

    <T> T getImplementation(Class<T> clazz);

    void shutdown();
}
