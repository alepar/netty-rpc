package ru.alepar.rpc;

public interface ProxyFactory {

    <T> T getImplementation(Class<T> clazz);

}
