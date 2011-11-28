package ru.alepar.rpc.api;

import java.io.Serializable;

public interface Remote {

    /**
     * @return unique id for the connection, note that client and server have the same id for particular connection
     */
    Id getId();

    /**
     * @return remote address of the associated client
     */
    String getRemoteAddress();

    /**
     * @return see {@link org.jboss.netty.channel.Channel#isWritable() Channel#isWritable()}
     */
    boolean isWritable();

    /**
     * @param clazz interface on remote side which will serve invocations
     * @param <T> interface registered on remote side
     * @return proxy, which will pass invocations to remote side
     */
    <T> T getProxy(Class<T> clazz);

    public interface Id extends Serializable {}
    
}
