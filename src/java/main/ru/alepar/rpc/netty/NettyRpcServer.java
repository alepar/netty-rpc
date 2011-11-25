package ru.alepar.rpc.netty;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.serialization.ObjectDecoder;
import org.jboss.netty.handler.codec.serialization.ObjectEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.alepar.rpc.Remote;
import ru.alepar.rpc.ImplementationFactory;
import ru.alepar.rpc.RpcServer;
import ru.alepar.rpc.exception.TransportException;

import static ru.alepar.rpc.netty.Util.invokeMethod;

public class NettyRpcServer implements RpcServer {

    private final Logger log = LoggerFactory.getLogger(NettyRpcServer.class);

    private List<ExceptionListener> exceptionListeners = new CopyOnWriteArrayList<ExceptionListener>();
    private final List<ClientListener> clientListeners = new CopyOnWriteArrayList<ClientListener>();

    private final Map<Class<?>, ServerProvider<?>> implementations = new HashMap<Class<?>, ServerProvider<?>>();
    private final ServerBootstrap bootstrap;

    private final ClientRepository clients = new ClientRepository();
    private final Channel acceptChannel;
    private ServerKeepAliveThread keepAliveThread;

    public NettyRpcServer(final InetSocketAddress bindAddress) {
        this(bindAddress, 30000l);
    }

    public NettyRpcServer(final InetSocketAddress bindAddress, final long keepalivePeriod) {
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
            public void onExceptionCaught(Remote remote, Exception e) {
                log.error("server caught an exception from " + remote.toString(), e);
            }
        });

        keepAliveThread = new ServerKeepAliveThread(clients, keepalivePeriod);
        acceptChannel = bootstrap.bind(bindAddress);
    }

    @Override
    public void shutdown() {
        try {
            keepAliveThread.safeInterrupt();

            // close main channel
            acceptChannel.close().await();

            // send close message to all clients
            List<ChannelFuture> futures = new LinkedList<ChannelFuture>();
            for(Channel c: clients.getChannels()) {
                if (c.isOpen()) {
                    futures.add(c.close());
                }
            }

            // wait for close to complete
            for (ChannelFuture future : futures) {
                future.await();
            }

            // release executors
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

    @Override
    public Remote getClient(Remote.Id clientId) {
        return clients.getClient(clientId);
    }

    @Override
    public Collection<Remote> getClients() {
        return clients.getClients();
    }

    private void fireException(Remote remote, Exception exc) {
        for (ExceptionListener listener : exceptionListeners) {
            try {
                listener.onExceptionCaught(remote, exc);
            } catch (Exception e) {
                log.error("exception listener " + listener + " threw exception", e);
            }
        }
    }

    private void fireClientConnect(Remote remote) {
        for (ClientListener listener : clientListeners) {
            try {
                listener.onClientConnect(remote);
            } catch (Exception e) {
                log.error("remote listener " + listener + " threw exception", e);
            }
        }
    }

    private void fireClientDisconnect(Remote remote) {
        for (ClientListener listener : clientListeners) {
            try {
                listener.onClientDisconnect(remote);
            } catch (Exception e) {
                log.error("remote listener " + listener + " threw exception", e);
            }
        }
    }

    private class RpcHandler extends SimpleChannelHandler {

        private final ConcurrentMap<Class<?>, Object> cache = new ConcurrentHashMap<Class<?>, Object> ();

        private NettyClient client;

        @Override
        public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
            client = new NettyClient(ctx.getChannel());
            clients.addClient(client);
            fireClientConnect(client);
        }

        @Override
        public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
            fireClientDisconnect(client);
            clients.removeClient(client);
        }

        @Override
        public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
            Object message = e.getMessage();
            log.debug("server got message {} from {}", message.toString(), ctx.getChannel().toString());

            if (message instanceof InvocationRequest) {
                processRequest(ctx, e, (InvocationRequest) message);
            } else if(message instanceof ExceptionNotify) {
                fireException(client, ((ExceptionNotify) message).exc);
            } else if(message instanceof HandshakeFromClient) {
                ctx.getChannel().write(new HandshakeFromServer(client.getId()));
            } else if(message instanceof KeepAlive) {
                // ignore
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
            fireException(client, new TransportException(e.getCause()));
        }
    }

}
