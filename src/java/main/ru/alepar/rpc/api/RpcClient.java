package ru.alepar.rpc.api;

public interface RpcClient {

    /**
     * @return remote associated with server
     */
    Remote getRemote();

    /**
     * shutdowns client and releases all resources used
     */
    void shutdown();

}
