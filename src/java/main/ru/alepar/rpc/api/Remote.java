package ru.alepar.rpc.api;

import java.io.Serializable;

public interface Remote {
    
    Id getId();
    String getRemoteAddress();
    boolean isWritable();

    <T> T getProxy(Class<T> clazz);

    public interface Id extends Serializable {}
    
}
