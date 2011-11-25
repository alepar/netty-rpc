package ru.alepar.rpc;

public interface RpcClient {
    <T> void addImplementation(Class<T> interfaceClass, T implObject);

    <T> T getImplementation(Class<T> clazz);

    void addExceptionListener(ExceptionListener listener);

    Client.Id getClientId();

    void shutdown();

    boolean isWritable();

    interface ExceptionListener {
        void onExceptionCaught(Exception e);
    }
}
