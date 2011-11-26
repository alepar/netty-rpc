package ru.alepar.rpc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.alepar.rpc.netty.*;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.unmodifiableMap;

public class NettyRpcServerBuilder {

    private final InetSocketAddress bindAddress;

    private final Map<Class<?>, ServerProvider<?>> implementations = new HashMap<Class<?>, ServerProvider<?>>();
    private final List<RpcServer.ExceptionListener> exceptionListeners = new ArrayList<RpcServer.ExceptionListener>();
    private final List<RpcServer.ClientListener> clientListeners = new ArrayList<RpcServer.ClientListener>();

    private long keepAlivePeriod = 30000l;

    public NettyRpcServerBuilder(InetSocketAddress bindAddress) {
        this.bindAddress = bindAddress;

        new RpcServer.ExceptionListener() {
            private final Logger log = LoggerFactory.getLogger(NettyRpcServer.class);
            @Override
            public void onExceptionCaught(Remote remote, Exception e) {
                log.error("server caught an exception from " + remote.toString(), e);
            }
        };
    }

    public <T> NettyRpcServerBuilder addObject(Class<T> interfaceClass, T implementingObject) {
        implementations.put(interfaceClass, new SimpleServerProvider<T>(implementingObject));
        return this;
    }

    public <T> NettyRpcServerBuilder addClass(Class<T> interfaceClass, Class<? extends T> implClass) {
        implementations.put(interfaceClass, new InjectingServerProvider<T>(implClass));
        return this;
    }

    public <T> NettyRpcServerBuilder addFactory(Class<T> interfaceClass, ImplementationFactory<? extends T> factory) {
        implementations.put(interfaceClass, new FactoryServerProvider<T>(factory));
        return this;
    }

    public NettyRpcServerBuilder addExceptionListener(RpcServer.ExceptionListener listener) {
        exceptionListeners.add(listener);
        return this;
    }

    public NettyRpcServerBuilder addClientListener(RpcServer.ClientListener listener) {
        clientListeners.add(listener);
        return this;
    }

    public NettyRpcServerBuilder enableKeepAlive(long keepAlivePeriod) {
        this.keepAlivePeriod = keepAlivePeriod;
        return this;
    }

    public RpcServer build() {
        return new NettyRpcServer(
                bindAddress,
                unmodifiableMap(implementations), 
                exceptionListeners.toArray(new RpcServer.ExceptionListener[exceptionListeners.size()]), 
                clientListeners.toArray(new RpcServer.ClientListener[clientListeners.size()]),
                keepAlivePeriod
        );
    }
}
