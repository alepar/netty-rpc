package ru.alepar.rpc.netty;

import ch.qos.logback.classic.pattern.MethodOfCallerConverter;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.serialization.ObjectDecoder;
import org.jboss.netty.handler.codec.serialization.ObjectEncoder;
import ru.alepar.rpc.RpcClient;
import ru.alepar.rpc.exception.ProtocolException;
import ru.alepar.rpc.exception.TransportException;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

public class NettyRpcClient implements RpcClient {

    private final InvocationHandler handler = new NettyInvocationHandler();
    private final Channel channel;
    private final ClientBootstrap bootstrap;

    private CountDownLatch latch;
    private InvocationResponse response;

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
    }

    @Override
    public void shutdown() {
        channel.close().awaitUninterruptibly();
        bootstrap.releaseExternalResources();
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public <T> T getImplementation(Class<T> clazz) {
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, handler);
    }

    private class NettyInvocationHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            latch = new CountDownLatch(1);
            validateMethod(method);
            channel.write(new InvocationRequest(method.getDeclaringClass().getName(), method.getName(), toSerializable(args), method.getParameterTypes()));
            latch.await();
            if (response.exc != null) {
                throw response.exc;
            }
            return response.returnValue;
        }

        private void validateMethod(Method method) {
            try {
                for (int i = 0; i < method.getParameterTypes().length; i++) {
                    Class<?> clazz = method.getParameterTypes()[i];
                    if (!clazz.isPrimitive() && !Serializable.class.isAssignableFrom(clazz)) {
                        throw new RuntimeException("param #" + (i + 1) + "(" + clazz.getName() + ") is not serializable");
                    }
                }
                Class<?> clazz = method.getReturnType();
                if(!clazz.isPrimitive() && !Serializable.class.isAssignableFrom(clazz)) {
                    throw new RuntimeException("return type (" + clazz.getName() + ") is not serializable");
                }
            } catch (RuntimeException e) {
                throw new ProtocolException("cannot rpc method: " + method, e);
            }
        }
    }

    private static Serializable[] toSerializable(Object[] args) {
        if(args == null) {
            return null;
        }
        Serializable[] result = new Serializable[args.length];
        for (int i = 0; i < args.length; i++) {
            result[i] = (Serializable) args[i];
        }
        return result;
    }

    private class RpcHandler extends SimpleChannelHandler {
        @Override
        public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
            response = (InvocationResponse) e.getMessage();
            latch.countDown();
        }
    }

}
