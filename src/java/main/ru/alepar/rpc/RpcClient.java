package ru.alepar.rpc;

public interface RpcClient {
    <T> T getImplementation(Class<T> clazz);

    Remote.Id getClientId();

    void shutdown();

    boolean isWritable();

    interface ExceptionListener {
        void onExceptionCaught(Exception e);
    }
}
