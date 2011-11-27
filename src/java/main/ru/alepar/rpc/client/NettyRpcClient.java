package ru.alepar.rpc.client;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.serialization.ObjectDecoder;
import org.jboss.netty.handler.codec.serialization.ObjectEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.alepar.rpc.api.ExceptionListener;
import ru.alepar.rpc.api.Remote;
import ru.alepar.rpc.api.RpcClient;
import ru.alepar.rpc.api.exception.TransportException;
import ru.alepar.rpc.common.NettyRemote;
import ru.alepar.rpc.common.Util;
import ru.alepar.rpc.common.message.*;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static ru.alepar.rpc.common.Util.*;

public class NettyRpcClient implements RpcClient {

    private final Logger log = LoggerFactory.getLogger(NettyRpcClient.class);

    private final ClientKeepAliveThread keepAliveThread;
    private final InvocationHandler handler = new ClientProxyHandler();

    private final Map<Class<?>, Object> implementations;
    private final ExceptionListener[] listeners;

    private final ClientBootstrap bootstrap;

    private final Channel channel;
    private volatile NettyRemote remote;

    private final CountDownLatch latch;

    public NettyRpcClient(final InetSocketAddress remoteAddress, Map<Class<?>, Object> implementations, ExceptionListener[] listeners, final long keepalivePeriod) {
        this.implementations = implementations;
        this.listeners = listeners;

        bootstrap = new ClientBootstrap(
                new NioClientSocketChannelFactory(
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

        final ChannelFuture future = bootstrap.connect(remoteAddress);
        channel = future.awaitUninterruptibly().getChannel();

        if (!future.isSuccess()) {
            bootstrap.releaseExternalResources();
            throw new TransportException("failed to connect to " + remoteAddress, future.getCause());
        }
        
        latch = new CountDownLatch(1);
        channel.write(new HandshakeFromClient(implementations.keySet()));
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException("interrupted waiting for handshake", e);
        }

        keepAliveThread = new ClientKeepAliveThread(channel, keepalivePeriod);
        keepAliveThread.start();
    }

    @Override
    public void shutdown() {
        keepAliveThread.safeInterrupt();
        channel.close().awaitUninterruptibly();
        bootstrap.releaseExternalResources();
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public <T> T getImplementation(Class<T> clazz) {
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, handler);
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

    private class ClientProxyHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            validateMethod(method);
            channel.write(new InvocationRequest(method.getDeclaringClass().getName(), method.getName(), Util.toSerializable(args), method.getParameterTypes()));
            return null;
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
            remote = new NettyRemote(channel, msg.clientId, new HashSet<Class<?>>(Arrays.asList(msg.classes)));
            latch.countDown();
        }

        @Override
        public void acceptInvocationRequest(InvocationRequest msg) {
            try {
                Class clazz = Class.forName(msg.className);
                Object impl = getImplementation(msg, clazz);

                invokeMethod(msg, clazz, impl);
            } catch (Exception exc) {
                log.error("caught exception while trying to invoke implementation", exc);
                channel.write(new ExceptionNotify(exc));
            }
        }

        @Override
        public void acceptKeepAlive(KeepAlive msg) {
            // ignore
        }

        private Object getImplementation(InvocationRequest msg, Class clazz) {
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
