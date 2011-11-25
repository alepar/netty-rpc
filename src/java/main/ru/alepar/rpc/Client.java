package ru.alepar.rpc;

import java.io.Serializable;

public interface Client {
    
    Id getId();
    ProxyFactory getProxyFactory();
    String getRemoteAddress();

    public interface Id extends Serializable {}
    
    public interface ProxyFactory {
        <T> T getImplementation(Class<T> clazz);
    }
    

}
