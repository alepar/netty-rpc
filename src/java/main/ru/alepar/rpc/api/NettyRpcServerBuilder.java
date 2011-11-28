package ru.alepar.rpc.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.alepar.rpc.common.Validator;
import ru.alepar.rpc.server.*;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.unmodifiableMap;

public class NettyRpcServerBuilder {

    private final InetSocketAddress bindAddress;

    private final Validator validator = new Validator();
    private final Map<Class<?>, ServerProvider<?>> implementations = new HashMap<Class<?>, ServerProvider<?>>();
    private final List<ExceptionListener> exceptionListeners = new ArrayList<ExceptionListener>();
    private final List<ClientListener> clientListeners = new ArrayList<ClientListener>();

    private long keepAlivePeriod = 30000l;

    /**
     * @param bindAddress local address to bind to
     */
    public NettyRpcServerBuilder(InetSocketAddress bindAddress) {
        this.bindAddress = bindAddress;

        new ExceptionListener() {
            private final Logger log = LoggerFactory.getLogger(NettyRpcServer.class);
            @Override
            public void onExceptionCaught(Remote remote, Exception e) {
                log.error("server caught an exception from " + remote.toString(), e);
            }
        };
    }

    /**
     * add object, that will serve client requests <br/>
     * this instance will be shared by all clients, meaning that this object should be multithread-safe <br/>
     * @param interfaceClass interface that will be exposed on remote side
     * @param implementingObject object, that will handle all remote invocations
     * @param <T> interface, all parameters in all methods must be serializable, return type must be void
     * @return this builder
     */
    public <T> NettyRpcServerBuilder addObject(Class<T> interfaceClass, T implementingObject) {
        validator.validateInterface(interfaceClass);
        implementations.put(interfaceClass, new SimpleServerProvider<T>(implementingObject));
        return this;
    }

    /**
     * add class, that will serve client requests <br/>
     * each client will get it's own instance of this class <br/>
     * so you don't need to worry about multithread-safeness <br/>
     * and you can keep some state related to current client here <br/>
     * <br/>
     * you can also define constructor, which accepts Remote param, annotated with @Inject <br/>
     * this way you can get Remote for communicating with client which is served by current instance <br/>
     * e.g.: <br/>
     * <blockquote><pre>
     * public SomeImplClass(@Inject Remote remote) {
     *     this.remote = remote
     * }</blockquote></pre><br/>
     * @param interfaceClass interface that will be exposed on remote side
     * @param implClass class, that will be instantiated to serve client's requests
     * @param <T> interface, all parameters in all methods must be serializable, return type must be void
     * @return this builder
     */
    public <T> NettyRpcServerBuilder addClass(Class<T> interfaceClass, Class<? extends T> implClass) {
        validator.validateInterface(interfaceClass);
        implementations.put(interfaceClass, new InjectingServerProvider<T>(implClass));
        return this;
    }

    /**
     * add factory, which will create objects, that will serve client requests <br/>
     * each client will get it's own instance, created by this factory <br/>
     * so you don't need to worry about multithread-safeness of instances, created by this factory <br/>
     * and you can keep some state related to current client there <br/>
     * @param interfaceClass interface that will be exposed on remote side
     * @param factory factory, which will create objects, that will serve client requests
     * @param <T> interface, all parameters in all methods must be serializable, return type must be void
     * @return this builder
     */
    public <T> NettyRpcServerBuilder addFactory(Class<T> interfaceClass, ImplementationFactory<? extends T> factory) {
        validator.validateInterface(interfaceClass);
        implementations.put(interfaceClass, new FactoryServerProvider<T>(factory));
        return this;
    }

    /**
     * this listener will be called if TransportException / RemoteException caught
     * @param listener to add
     * @return this builder
     */
    public NettyRpcServerBuilder addExceptionListener(ExceptionListener listener) {
        exceptionListeners.add(listener);
        return this;
    }

    /**
     * this listener will be called on any client connects\disconnects
     * @param listener to add
     * @return this builder
     */
    public NettyRpcServerBuilder addClientListener(ClientListener listener) {
        clientListeners.add(listener);
        return this;
    }

    /**
     * enables sending keepalive packets at given interval
     * @param keepAlivePeriod interval in milliseconds
     * @return this builder
     */
    public NettyRpcServerBuilder enableKeepAlive(long keepAlivePeriod) {
        this.keepAlivePeriod = keepAlivePeriod;
        return this;
    }

    /**
     * @return configured RpcServer
     */
    public RpcServer build() {
        return new NettyRpcServer(
                bindAddress,
                unmodifiableMap(implementations), 
                exceptionListeners.toArray(new ExceptionListener[exceptionListeners.size()]),
                clientListeners.toArray(new ClientListener[clientListeners.size()]),
                keepAlivePeriod
        );
    }
}
