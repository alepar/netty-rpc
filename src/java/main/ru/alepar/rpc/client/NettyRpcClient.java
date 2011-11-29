package ru.alepar.rpc.client;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.serialization.ClassResolver;
import org.jboss.netty.handler.codec.serialization.ObjectDecoder;
import org.jboss.netty.handler.codec.serialization.ObjectEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.alepar.rpc.api.ExceptionListener;
import ru.alepar.rpc.api.Remote;
import ru.alepar.rpc.api.RpcClient;
import ru.alepar.rpc.api.exception.TransportException;
import ru.alepar.rpc.common.KeepAliveTimer;
import ru.alepar.rpc.common.NettyRemote;
import ru.alepar.rpc.common.message.ExceptionNotify;
import ru.alepar.rpc.common.message.HandshakeFromClient;
import ru.alepar.rpc.common.message.HandshakeFromServer;
import ru.alepar.rpc.common.message.InvocationRequest;
import ru.alepar.rpc.common.message.KeepAlive;
import ru.alepar.rpc.common.message.RpcMessage;

import static ru.alepar.rpc.common.Util.foldClassesToStrings;
import static ru.alepar.rpc.common.Util.invokeMethod;
import static ru.alepar.rpc.common.Util.unfoldStringToClasses;

public class NettyRpcClient implements RpcClient {

    private final Logger log = LoggerFactory.getLogger(NettyRpcClient.class);

    private final ClassResolver classResolver;
    private final KeepAliveTimer keepAliveTimer;
    private final CountDownLatch latch;

    private final Map<Class<?>, Object> implementations;
    private final ExceptionListener[] listeners;

    private final ClientBootstrap bootstrap;
    private final Channel channel;
    private volatile NettyRemote remote;

    public NettyRpcClient(final InetSocketAddress remoteAddress, final Map<Class<?>, Object> implementations, final ExceptionListener[] listeners, final ClassResolver classResolver, final long keepalivePeriod, ExecutorService bossExecutor, ExecutorService workerExecutor) {
        this.implementations = implementations;
        this.listeners = listeners;
        this.classResolver = classResolver;

        bootstrap = new ClientBootstrap(
                new NioClientSocketChannelFactory(bossExecutor, workerExecutor)
        );

        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            public ChannelPipeline getPipeline() throws Exception {
                return Channels.pipeline(
                        new ObjectEncoder(),
                        new ObjectDecoder(classResolver),
                        new RpcHandler());
            }
        });

        final ChannelFuture future = bootstrap.connect(remoteAddress);
        channel = future.awaitUninterruptibly().getChannel();

        if (!future.isSuccess()) {
            bootstrap.releaseExternalResources();
            throw new TransportException("failed to connect to " + remoteAddress, future.getCause());
        }
        
        latch = new CountDownLatch(1);
        channel.write(new HandshakeFromClient(foldClassesToStrings(new ArrayList<Class<?>>(implementations.keySet()))));
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException("interrupted waiting for handshake", e);
        }

        keepAliveTimer = new KeepAliveTimer(Collections.singleton(remote), keepalivePeriod);
    }

    @Override
    public void shutdown() {
        keepAliveTimer.stop();
        channel.close().awaitUninterruptibly();
        bootstrap.releaseExternalResources();
    }

    @Override
    public Remote getRemote() {
        return remote;
    }

    private void fireException(Exception exc) {
        for (ExceptionListener listener : listeners) {
            try {
                listener.onExceptionCaught(remote, exc);
            } catch (Exception e) {
                log.error("exception listener " + listener + " threw exception", exc);
            }
        }
    }

    private class RpcHandler extends SimpleChannelHandler implements RpcMessage.Visitor {

        @Override
        public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
            RpcMessage message = (RpcMessage) e.getMessage();
            log.debug("client got message {}", message.toString());

            message.visit(this);
        }

        @Override
        public void acceptExceptionNotify(ExceptionNotify msg) {
            fireException(msg.exc);
        }

        @Override
        public void acceptHandshakeFromClient(HandshakeFromClient msg) {
            // ignore // shudn't happen
        }

        @Override
        public void acceptHandshakeFromServer(HandshakeFromServer msg) {
            try {
                remote = new NettyRemote(channel, msg.clientId, new HashSet<Class<?>>(unfoldStringToClasses(classResolver, msg.classNames)));
            } catch (ClassNotFoundException e) {
                log.error("interfaces registered on server side are not in the classpath", e);
                throw new RuntimeException("interfaces registered on server side are not in the classpath", e);
            } finally {
                latch.countDown();
            }
        }

        @Override
        public void acceptInvocationRequest(InvocationRequest msg) {
            try {
                Class<?> clazz = classResolver.resolve(msg.className);
                Object impl = getImplementation(msg, clazz);

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

        private Object getImplementation(InvocationRequest msg, Class<?> clazz) {
            Object impl = implementations.get(clazz);
            if(impl == null) {
                throw new RuntimeException("interface is not registered on client: " + msg.className);
            }
            return impl;
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
            fireException(new TransportException(e.getCause()));
        }

    }

}
