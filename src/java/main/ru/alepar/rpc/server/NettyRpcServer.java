package ru.alepar.rpc.server;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

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
import org.jboss.netty.handler.codec.serialization.ClassResolver;
import org.jboss.netty.handler.codec.serialization.ObjectDecoder;
import org.jboss.netty.handler.codec.serialization.ObjectEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.alepar.rpc.api.ClientListener;
import ru.alepar.rpc.api.ExceptionListener;
import ru.alepar.rpc.api.Remote;
import ru.alepar.rpc.api.RpcServer;
import ru.alepar.rpc.api.exception.TransportException;
import ru.alepar.rpc.common.KeepAliveTimer;
import ru.alepar.rpc.common.NettyId;
import ru.alepar.rpc.common.NettyRemote;
import ru.alepar.rpc.common.message.ExceptionNotify;
import ru.alepar.rpc.common.message.HandshakeFromClient;
import ru.alepar.rpc.common.message.HandshakeFromServer;
import ru.alepar.rpc.common.message.InvocationRequest;
import ru.alepar.rpc.common.message.KeepAlive;
import ru.alepar.rpc.common.message.RpcMessage;

import static java.util.Collections.unmodifiableCollection;
import static ru.alepar.rpc.common.Util.foldClassesToStrings;
import static ru.alepar.rpc.common.Util.invokeMethod;
import static ru.alepar.rpc.common.Util.unfoldStringToClasses;

public class NettyRpcServer implements RpcServer {

    private final Logger log = LoggerFactory.getLogger(NettyRpcServer.class);

    private final ClientRepository clients = new ClientRepository();
    private final ClassResolver classResolver;
    private final KeepAliveTimer keepAliveTimer;

    private final Map<Class<?>, ServerProvider<?>> implementations;
    private final ExceptionListener[] exceptionListeners;
    private final ClientListener[] clientListeners;

    private final ServerBootstrap bootstrap;
    private final Channel acceptChannel;

    public NettyRpcServer(final InetSocketAddress bindAddress, final Map<Class<?>, ServerProvider<?>> implementations, final ExceptionListener[] exceptionListeners, final ClientListener[] clientListeners, final ClassResolver classResolver, final long keepalivePeriod, final ExecutorService bossExecutor, final ExecutorService workerExecutor) {
        this.exceptionListeners = exceptionListeners;
        this.clientListeners = clientListeners;
        this.implementations = implementations;
        this.classResolver = classResolver;
        bootstrap = new ServerBootstrap(
                new NioServerSocketChannelFactory(bossExecutor,workerExecutor)
        );

        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            public ChannelPipeline getPipeline() throws Exception {
                return Channels.pipeline(
                        new ObjectEncoder(),
                        new ObjectDecoder(classResolver),
                        new RpcHandler());
            }
        });
        
        keepAliveTimer = new KeepAliveTimer(this.clients.getClients(), keepalivePeriod);
        acceptChannel = bootstrap.bind(bindAddress);
    }

    @Override
    public void shutdown() {
        try {
            keepAliveTimer.stop();

            // close main channel
            acceptChannel.close().await();

            // send close message to all clients
            List<ChannelFuture> futures = new LinkedList<ChannelFuture>();
            for(NettyRemote client: clients.getClients()) {
                if (client.getChannel().isOpen()) {
                    futures.add(client.getChannel().close());
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
    public Remote getClient(Remote.Id clientId) {
        return clients.getClient(clientId);
    }

    @Override
    public Collection<Remote> getClients() {
        return unmodifiableCollection((Collection<? extends Remote>) clients.getClients());
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

    private class RpcHandler extends SimpleChannelHandler implements RpcMessage.Visitor {

        private final ConcurrentMap<Class<?>, Object> cache = new ConcurrentHashMap<Class<?>, Object> ();
        
        private Channel channel;
        private NettyRemote remote;

        @Override
        public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
            channel = ctx.getChannel();
        }

        @Override
        public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
            fireClientDisconnect(remote);
            clients.removeClient(remote.getId());
        }

        @Override
        public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
            RpcMessage message = (RpcMessage) e.getMessage();
            log.debug("server got message {} from {}", message.toString(), ctx.getChannel().toString());
            message.visit(this);
        }

        @Override
        public void acceptExceptionNotify(ExceptionNotify msg) {
            fireException(remote, msg.exc);
        }

        @Override
        public void acceptHandshakeFromClient(HandshakeFromClient msg) {
            try {
                remote = new NettyRemote(channel, new NettyId(channel.getId()), new HashSet<Class<?>>(unfoldStringToClasses(classResolver, msg.classNames)));
                channel.write(new HandshakeFromServer(remote.getId(), foldClassesToStrings(new ArrayList<Class<?>>(implementations.keySet()))));
                clients.addClient(remote);
                fireClientConnect(remote);
            } catch (ClassNotFoundException e) {
                log.error("interfaces registered on client side are not in the classpath", e);
                throw new RuntimeException("interfaces registered on client side are not in the classpath", e);
            }
        }

        @Override
        public void acceptHandshakeFromServer(HandshakeFromServer msg) {
            // ignore // shudn't happen
        }

        @Override
        public void acceptInvocationRequest(InvocationRequest msg) {
            try {
                Class<?> clazz = classResolver.resolve(msg.className);
                Object impl = getImplementation(clazz);
                invokeMethod(msg, impl, classResolver);
            } catch (Exception exc) {
                log.error("caught exception while trying to invoke implementation", exc);
                channel.write(new ExceptionNotify(exc));
            }
        }

        @Override
        public void acceptKeepAlive(KeepAlive msg) {
            // ignore
        }

        private Object getImplementation(Class<?> clazz) {
            Object impl = cache.get(clazz);
            if (impl == null) {
                impl = createImplementation(clazz);
                cache.put(clazz, impl);
            }
            return impl;
        }

        private Object createImplementation(Class<?> clazz) {
            ServerProvider<?> provider = implementations.get(clazz);
            if(provider == null) {
                throw new RuntimeException("interface is not registered on server: " + clazz.getCanonicalName());
            }
            return provider.provideFor(remote);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
            fireException(remote, new TransportException(e.getCause()));
        }
    }

}
