package ru.alepar.rpc.api;

import ru.alepar.rpc.client.NettyRpcClient;
import ru.alepar.rpc.common.Validator;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.unmodifiableMap;

public class NettyRpcClientBuilder {

    private final InetSocketAddress serverAddress;

    private final Validator validator = new Validator();
    private final Map<Class<?>, Object> implementations = new HashMap<Class<?>, Object>();
    private final List<ExceptionListener> listeners = new ArrayList<ExceptionListener>();

    private long keepAlivePeriod = 30000l;

    /**
     * @param serverAddress remote address to connect to
     */
    public NettyRpcClientBuilder(InetSocketAddress serverAddress) {
        this.serverAddress = serverAddress;
    }

    /**
     * add object to serve server requests <br/>
     * all requests will be server by this instance, must not be multithread-safe though <br/>
     * @param interfaceClass interface that will be exposed on remote side
     * @param implementingObject object, that will handle all remote invocations
     * @param <T> interface, all parameters in all methods must be serializable, return type must be void
     * @return this builder
     */
    public <T> NettyRpcClientBuilder addObject(Class<T> interfaceClass, T implementingObject) {
        validator.validateInterface(interfaceClass);
        implementations.put(interfaceClass, implementingObject);
        return this;
    }

    /**
     * this listener will be called if TransportException / RemoteException caught
     * @param listener to add
     * @return this builder
     */
    public NettyRpcClientBuilder addExceptionListener(ExceptionListener listener) {
        listeners.add(listener);
        return this;
    }

    /**
     * enables sending keepalive packets at given interval
     * @param keepAlivePeriod interval in milliseconds
     * @return this builder
     */
    public NettyRpcClientBuilder enableKeepAlive(long keepAlivePeriod) {
        this.keepAlivePeriod = keepAlivePeriod;
        return this;
    }

    /**
     * @return configured RpcClient
     */
    public RpcClient build() {
        return new NettyRpcClient(
                serverAddress,
                unmodifiableMap(implementations),
                listeners.toArray(new ExceptionListener[listeners.size()]),
                keepAlivePeriod
        );
    }
}
