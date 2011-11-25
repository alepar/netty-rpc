package ru.alepar.rpc.netty;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

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
import org.jboss.netty.handler.codec.serialization.ObjectDecoder;
import org.jboss.netty.handler.codec.serialization.ObjectEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.alepar.rpc.Client;
import ru.alepar.rpc.RpcClient;
import ru.alepar.rpc.exception.TransportException;

import static ru.alepar.rpc.netty.Util.invokeMethod;
import static ru.alepar.rpc.netty.Util.validateMethod;

public class NettyRpcClient implements RpcClient {

    private final Logger log = LoggerFactory.getLogger(NettyRpcClient.class);

    private final InvocationHandler handler = new ClientProxyHandler();
    private final Channel channel;
    private final ClientBootstrap bootstrap;
    private final Map<Class<?>, Object> implementations = new HashMap<Class<?>, Object>();
    private final List<ExceptionListener> listeners = new CopyOnWriteArrayList<ExceptionListener>();
    private final CountDownLatch latch;

    private Client.Id clientId;
    private final Thread keepAliveThread;

    public NettyRpcClient(final InetSocketAddress remoteAddress) {
        this(remoteAddress, 30000l);
    }

    public NettyRpcClient(final InetSocketAddress remoteAddress, final long keepalivePeriod) {
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

        ChannelFuture future = bootstrap.connect(remoteAddress);
        channel = future.awaitUninterruptibly().getChannel();

        if (!future.isSuccess()) {
            bootstrap.releaseExternalResources();
            throw new TransportException("failed to connect to " + remoteAddress, future.getCause());
        }

        latch = new CountDownLatch(1);
        channel.write(new HandshakeFromClient());
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException("interrupted waiting for handshake", e);
        }

        keepAliveThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (!Thread.interrupted()) {
                        channel.write(new KeepAlive());
                        Thread.sleep(keepalivePeriod);
                    }
                } catch (InterruptedException ignored) {
                }
                log.warn("NettyRpcClient-KeepAlive interrupted");
            }
        }, "NettyRpcClient-KeepAlive");
        keepAliveThread.start();
    }

    @Override
    public void shutdown() {
        keepAliveThread.interrupt();
        channel.close().awaitUninterruptibly();
        bootstrap.releaseExternalResources();
    }

    @Override
    public <T> void addImplementation(Class<T> interfaceClass, T implObject) {
        implementations.put(interfaceClass, implObject);
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public <T> T getImplementation(Class<T> clazz) {
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, handler);
    }

    @Override
    public void addExceptionListener(ExceptionListener listener) {
        listeners.add(listener);
    }

    @Override
    public Client.Id getClientId() {
        return clientId;
    }

    @Override
    public boolean isWritable() {
        return channel.isWritable();
    }

    private void fireException(Exception exc) {
        for (ExceptionListener listener : listeners) {
            try {
                listener.onExceptionCaught(exc);
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

    private class RpcHandler extends SimpleChannelHandler {
        @Override
        public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
            Object message = e.getMessage();
            log.debug("client got message {}", message.toString());

            if(message instanceof InvocationRequest) {
                processRequest(e, (InvocationRequest) message);
            } else if (message instanceof ExceptionNotify) {
                fireException(((ExceptionNotify) message).exc);
            } else if(message instanceof HandshakeFromServer) {
                clientId = ((HandshakeFromServer)message).clientId;
                latch.countDown();
            } else if(message instanceof KeepAlive) {
                // ignore
            } else {
                log.error("got unknown message from the channel: {}", message);
            }
        }

        private void processRequest(MessageEvent e, InvocationRequest msg) {
            try {
                Class clazz = Class.forName(msg.className);
                Object impl = getImplementation(msg, clazz);

                invokeMethod(msg, clazz, impl);
            } catch (Exception exc) {
                log.error("caught exception while trying to invoke implementation", exc);
                e.getChannel().write(new ExceptionNotify(exc));
            }
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
