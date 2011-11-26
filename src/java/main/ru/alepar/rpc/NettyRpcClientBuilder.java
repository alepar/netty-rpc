package ru.alepar.rpc;

import ru.alepar.rpc.netty.NettyRpcClient;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.unmodifiableMap;

public class NettyRpcClientBuilder {

    private final InetSocketAddress serverAddress;

    private final Map<Class<?>, Object> implementations = new HashMap<Class<?>, Object>();
    private final List<RpcClient.ExceptionListener> listeners = new ArrayList<RpcClient.ExceptionListener>();

    private long keepAlivePeriod = 30000l;

    public NettyRpcClientBuilder(InetSocketAddress serverAddress) {
        this.serverAddress = serverAddress;
    }

    public <T> NettyRpcClientBuilder addObject(Class<T> interfaceClass, T implementingObject) {
        implementations.put(interfaceClass, implementingObject);
        return this;
    }
    
    public NettyRpcClientBuilder addExceptionListener(RpcClient.ExceptionListener listener) {
        listeners.add(listener);
        return this;
    }    

    public NettyRpcClientBuilder enableKeepAlive(long millis) {
        this.keepAlivePeriod = millis;
        return this;
    }

    public RpcClient build() {
        return new NettyRpcClient(
                serverAddress,
                unmodifiableMap(implementations),
                listeners.toArray(new RpcClient.ExceptionListener[listeners.size()]),
                keepAlivePeriod
        );
    }
}
