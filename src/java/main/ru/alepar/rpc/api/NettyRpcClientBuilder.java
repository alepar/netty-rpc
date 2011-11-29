package ru.alepar.rpc.api;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.jboss.netty.handler.codec.serialization.ClassResolver;
import ru.alepar.rpc.client.NettyRpcClient;
import ru.alepar.rpc.common.BossThreadFactory;
import ru.alepar.rpc.common.PrimitiveTypesClassResolver;
import ru.alepar.rpc.common.Validator;
import ru.alepar.rpc.common.WorkerThreadFactory;

import static java.util.Collections.unmodifiableMap;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static org.jboss.netty.handler.codec.serialization.ClassResolvers.softCachingConcurrentResolver;

public class NettyRpcClientBuilder {

    private final InetSocketAddress serverAddress;

    private final Validator validator = new Validator();
    private final Map<Class<?>, Object> implementations = new HashMap<Class<?>, Object>();
    private final List<ExceptionListener> listeners = new ArrayList<ExceptionListener>();

    private ClassResolver classResolver = softCachingConcurrentResolver(null);
    private ExecutorService bossExecutor = newCachedThreadPool(new BossThreadFactory());
    private ExecutorService workerExecutor = newCachedThreadPool(new WorkerThreadFactory());
    private long keepAlive = 30000l;

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
     * sets interval at which KeepAlive packets will be sent to server <br/>
     *  <br/>
     * setting it to zero will effectively disable KeepAlive  <br/>
     * this is not recommended - you most probably will miss abrupt disconnects  <br/>
     * @param interval interval in milliseconds, if zero - keepAlive will be disabled
     * @return this builder
     */
    public NettyRpcClientBuilder setKeepAlive(long interval) {
        this.keepAlive = interval;
        return this;
    }

    /**
     * sets classResolver that will be used by this RpcClient <br/>
     * see {@link org.jboss.netty.handler.codec.serialization.ClassResolvers ClassResolvers} for available implementations
     * @param classResolver to be used, default is {@link org.jboss.netty.handler.codec.serialization.ClassResolvers#softCachingConcurrentResolver(java.lang.ClassLoader) softCachingConcurrentResolver}
     * @return this builder
     */
    public NettyRpcClientBuilder setClassResolver(ClassResolver classResolver) {
        this.classResolver = classResolver;
        return this;
    }

    /**
     * set executor, which will take care of all socket.accept() routine
     * by default, netty takes only one thread from this
     * default executor is {@link java.util.concurrent.Executors#newCachedThreadPool() newCachedThreadPool}(new {@link ru.alepar.rpc.common.BossThreadFactory BossThreadFactory}())
     * @param bossExecutor executor to be used as boss
     */
    public void setBossExecutor(ExecutorService bossExecutor) {
        this.bossExecutor = bossExecutor;
    }

    /**
     * set executor, which will take care of all remote calls
     * default executor is {@link java.util.concurrent.Executors#newCachedThreadPool() newCachedThreadPool}(new {@link ru.alepar.rpc.common.WorkerThreadFactory WorkerThreadFactory}())
     * @param workerExecutor executor to be used as boss
     */
    public void setWorkerExecutor(ExecutorService workerExecutor) {
        this.workerExecutor = workerExecutor;
    }

    /**
     * @return configured RpcClient
     */
    public RpcClient build() {
        return new NettyRpcClient(
                serverAddress,
                unmodifiableMap(implementations),
                listeners.toArray(new ExceptionListener[listeners.size()]),
                new PrimitiveTypesClassResolver(classResolver),
                keepAlive,
                bossExecutor,
                workerExecutor
        );
    }
}
