package ru.alepar.rpc.api;

public interface RpcClient {
    <T> T getImplementation(Class<T> clazz);
    
    Remote getRemote();

    void shutdown();

}
