package ru.alepar.rpc;

import java.util.Collection;

public interface RpcServer {

    <T> void addImplementation(Class<T> interfaceClass, T implObject);

    <T> void addClass(Class<T> interfaceClass, Class<? extends T> implClass);

    <T> void addFactory(Class<T> interfaceClass, ImplementationFactory<? extends T> factory);

    void addExceptionListener(ExceptionListener listener);

    void addClientListener(ClientListener listener);

    Remote getClient(Remote.Id clientId);

    void shutdown();

    Collection<Remote> getClients();

    public interface ExceptionListener {
        void onExceptionCaught(Remote remote, Exception e);
    }

    public interface ClientListener {
        void onClientConnect(Remote remote);

        void onClientDisconnect(Remote remote);
    }
}
