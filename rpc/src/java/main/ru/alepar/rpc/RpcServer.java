package ru.alepar.rpc;

public interface RpcServer {

    <T> void addImplementation(Class<T> clazz, T impl);

    void shutdown();
}
