package ru.alepar.rpc.api;

public interface RpcClient {

    Remote getRemote();

    void shutdown();

}
