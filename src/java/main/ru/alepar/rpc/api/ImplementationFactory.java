package ru.alepar.rpc.api;

public interface ImplementationFactory<T> {
    T create(Remote remote);
}
