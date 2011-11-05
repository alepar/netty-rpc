package ru.alepar.rpc.netty;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.serialization.ObjectDecoder;
import org.jboss.netty.handler.codec.serialization.ObjectEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.alepar.rpc.ClientId;
import ru.alepar.rpc.RpcClient;
import ru.alepar.rpc.exception.TransportException;

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

    private ClientId clientId;

    public NettyRpcClient(InetSocketAddress remoteAddress) {
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
    }

    @Override
    public void shutdown() {
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
    public ClientId getClientId() {
        return clientId;
    }

    private void notifyExceptionListeners(Exception exc) {
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
                notifyExceptionListeners(((ExceptionNotify) message).exc);
            } else if(message instanceof HandshakeFromServer) {
                clientId = ((HandshakeFromServer)message).clientId;
                latch.countDown();
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
            notifyExceptionListeners(new TransportException(e.getCause()));
        }

    }

}
