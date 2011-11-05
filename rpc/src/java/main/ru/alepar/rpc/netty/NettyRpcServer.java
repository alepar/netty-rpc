package ru.alepar.rpc.netty;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.serialization.ObjectDecoder;
import org.jboss.netty.handler.codec.serialization.ObjectEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.alepar.rpc.ClientId;
import ru.alepar.rpc.ImplementationFactory;
import ru.alepar.rpc.RpcServer;
import ru.alepar.rpc.exception.TransportException;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;

import static ru.alepar.rpc.netty.Util.invokeMethod;

public class NettyRpcServer implements RpcServer {

    private final Logger log = LoggerFactory.getLogger(NettyRpcServer.class);

    private List<ExceptionListener> exceptionListeners = new CopyOnWriteArrayList<ExceptionListener>();
    private final List<ClientListener> clientListeners = new CopyOnWriteArrayList<ClientListener>();

    private final Map<Class<?>, ServerProvider<?>> implementations = new HashMap<Class<?>, ServerProvider<?>>();
    private final ServerBootstrap bootstrap;

    private final ClientRepository clients = new ClientRepository();

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
        
        addExceptionListener(new ExceptionListener() {
            @Override
            public void onExceptionCaught(ClientId clientId, Exception e) {
                log.error("server caught an exception from client " + clientId.toString(), e);
            }
        });

        bootstrap.bind(bindAddress);
    }

    @Override
    public void shutdown() {
        try {
            List<ChannelFuture> futures = new LinkedList<ChannelFuture>();
            for(Channel c: clients.getChannels()) {
                if (c.isOpen()) {
                    futures.add(c.close());
                }
            }
            for (ChannelFuture future : futures) {
                future.await();
            }
            bootstrap.releaseExternalResources();
        } catch (InterruptedException e) {
            throw new RuntimeException("failed to shutdown properly", e);
        }
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

    @Override
    public void addExceptionListener(ExceptionListener listener) {
        exceptionListeners.add(listener);
    }

    @Override
    public void addClientListener(ClientListener listener) {
        clientListeners.add(listener);
    }

    private void fireException(ClientId clientId, Exception exc) {
        for (ExceptionListener listener : exceptionListeners) {
            try {
                listener.onExceptionCaught(clientId, exc);
            } catch (Exception e) {
                log.error("exception listener " + listener + " threw exception", e);
            }
        }
    }

    private void fireClientConnect(ClientId clientId) {
        for (ClientListener listener : clientListeners) {
            try {
                listener.onClientConnect(clientId);
            } catch (Exception e) {
                log.error("client listener " + listener + " threw exception", e);
            }
        }
    }

    private void fireClientDisconnect(ClientId clientId) {
        for (ClientListener listener : clientListeners) {
            try {
                listener.onClientDisconnect(clientId);
            } catch (Exception e) {
                log.error("client listener " + listener + " threw exception", e);
            }
        }
    }

    private class RpcHandler extends SimpleChannelHandler {

        private final ConcurrentMap<Class<?>, Object> cache = new ConcurrentHashMap<Class<?>, Object> ();

        private NettyClientId clientId;

        @Override
        public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
            clientId = new NettyClientId(ctx.getChannel());
            clients.addClient(clientId, ctx.getChannel());
            fireClientConnect(clientId);
        }

        @Override
        public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
            fireClientDisconnect(clientId);
            clients.removeClient(clientId);
        }

        @Override
        public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
            Object message = e.getMessage();
            log.debug("server got message {} from {}", message.toString(), ctx.getChannel().toString());

            if (message instanceof InvocationRequest) {
                processRequest(ctx, e, (InvocationRequest) message);
            } else if(message instanceof ExceptionNotify) {
                fireException(clientId, ((ExceptionNotify) message).exc);
            } else if(message instanceof HandshakeFromClient) {
                ctx.getChannel().write(new HandshakeFromServer(clientId));
            } else {
                log.error("got unknown message from the channel: {}", message);
            }
        }

        private void processRequest(ChannelHandlerContext ctx, MessageEvent e, InvocationRequest message) {
            try {
                Class clazz = Class.forName(message.className);
                Object impl = getImplementation(ctx, clazz);
                invokeMethod(message, clazz, impl);
            } catch (Exception exc) {
                log.error("caught exception while trying to invoke implementation", exc);
                e.getChannel().write(new ExceptionNotify(exc));
            }
        }

        private Object getImplementation(ChannelHandlerContext ctx, Class clazz) {
            Object impl = cache.get(clazz);
            if (impl == null) {
                impl = createImplementation(ctx, clazz);
                cache.put(clazz, impl);
            }
            return impl;
        }

        private Object createImplementation(ChannelHandlerContext ctx, Class clazz) {
            ServerProvider<?> provider = implementations.get(clazz);
            if(provider == null) {
                throw new RuntimeException("interface is not registered on server: " + clazz.getCanonicalName());
            }
            return provider.provideFor(ctx.getChannel());
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
            fireException(clientId, new TransportException(e.getCause()));
        }
    }

}
