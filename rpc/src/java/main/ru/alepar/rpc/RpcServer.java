package ru.alepar.rpc;

public interface RpcServer {

    <T> void addImplementation(Class<T> interfaceClass, T implObject);

    <T> void addClass(Class<T> interfaceClass, Class<? extends T> implClass);

    <T> void addFactory(Class<T> interfaceClass, ImplementationFactory<? extends T> factory);

    void addExceptionListener(ExceptionListener listener);

    void addClientListener(ClientListener listener);

    void shutdown();

    public interface ExceptionListener {
        void onExceptionCaught(Client client, Exception e);
    }

    public interface ClientListener {
        void onClientConnect(Client client);

        void onClientDisconnect(Client client);
    }
}
