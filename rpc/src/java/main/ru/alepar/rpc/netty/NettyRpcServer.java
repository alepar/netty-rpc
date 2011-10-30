package ru.alepar.rpc.netty;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.serialization.ObjectDecoder;
import org.jboss.netty.handler.codec.serialization.ObjectEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.alepar.rpc.ImplementationFactory;
import ru.alepar.rpc.RpcServer;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;

public class NettyRpcServer implements RpcServer {

    private static final Logger log = LoggerFactory.getLogger(NettyRpcServer.class);

    private final Map<Class<?>, ServerProvider<?>> implementations = new HashMap<Class<?>, ServerProvider<?>>();
    private final ServerBootstrap bootstrap;

    public NettyRpcServer(InetSocketAddress bindAddress) {
        bootstrap = new ServerBootstrap(
                new NioServerSocketChannelFactory(
                        Executors.newCachedThreadPool(),
                        Executors.newCachedThreadPool()));

        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            public ChannelPipeline getPipeline() throws Exception {
                return Channels.pipeline(
                        new ObjectEncoder(),
                        new ObjectDecoder(),
                        new RpcHandler());
            }
        });

        bootstrap.bind(bindAddress);
    }

    @Override
    public void shutdown() {
        bootstrap.releaseExternalResources();
    }

    @Override
    public <T> void addImplementation(Class<T> interfaceClass, T impl) {
        implementations.put(interfaceClass, new SimpleServerProvider<T>(impl));
    }

    @Override
    public <T> void addClass(Class<T> interfaceClass, Class<? extends T> implClass) {
        implementations.put(interfaceClass, new InjectingServerProvider<T>(implClass));
    }

    @Override
    public <T> void addFactory(Class<T> interfaceClass, ImplementationFactory<? extends T> factory) {
        implementations.put(interfaceClass, new FactoryServerProvider<T>(factory));
    }

    private class RpcHandler extends SimpleChannelHandler {

        private final ConcurrentMap<Class<?>, Object> cache = new ConcurrentHashMap<Class<?>, Object> ();

        @Override
        public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
            InvocationRequest msg = (InvocationRequest) e.getMessage();

            try {
                Class clazz = Class.forName(msg.className);

                Object impl = cache.get(clazz);
                if (impl == null) {
                    impl = createImplementation(ctx, msg, clazz);
                    cache.put(clazz, impl);
                }

                Object returnValue;
                Method method;
                if (msg.args != null) {
                    method = clazz.getMethod(msg.methodName, msg.types);
                    if(method == null) {
                        throw new RuntimeException("method is not found in server implementation: " + msg.methodName);
                    }
                    returnValue = method.invoke(impl, (Object[]) msg.args);
                } else {
                    method = clazz.getMethod(msg.methodName);
                    if(method == null) {
                        throw new RuntimeException("method is not found in server implementation: " + msg.methodName);
                    }
                    returnValue = method.invoke(impl);
                }
                Serializable safeReturnValue = (Serializable) returnValue;
                e.getChannel().write(new InvocationResponse(safeReturnValue, null));
            } catch (InvocationTargetException exc) {
                e.getChannel().write(new InvocationResponse(null, exc.getCause()));
            } catch (Throwable t) {
                e.getChannel().write(new InvocationResponse(null, t));
            }
        }

        private Object createImplementation(ChannelHandlerContext ctx, InvocationRequest msg, Class clazz) {
            ServerProvider<?> provider = implementations.get(clazz);
            if(provider == null) {
                throw new RuntimeException("interface is not registered on server: " + msg.className);
            }
            return provider.provideFor(ctx.getChannel());
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
            log.warn("NettyRpcServer caught exception", e.getCause());
            if (e.getChannel().isOpen()) {
                e.getChannel().close();
            }
        }
    }

}
