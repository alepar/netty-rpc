package ru.alepar.rpc.api;

import java.io.Serializable;

public interface Remote {
    
    Id getId();
    ProxyFactory getProxyFactory();
    String getRemoteAddress();
    boolean isWritable();

    public interface Id extends Serializable {}
    
    public interface ProxyFactory {
        <T> T getProxy(Class<T> clazz);
    }

}
