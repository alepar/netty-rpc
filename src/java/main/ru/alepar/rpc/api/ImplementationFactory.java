package ru.alepar.rpc.api;

public interface ImplementationFactory<T> {

    /**
     * creates new object which will serve requests to client
     * @param remote associated with client, which will be served by this instance
     * @return new implementation instance
     */
    T create(Remote remote);
}
