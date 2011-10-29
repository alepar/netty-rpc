package ru.alepar.rpc.netty;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.serialization.ObjectDecoder;
import org.jboss.netty.handler.codec.serialization.ObjectEncoder;
import ru.alepar.rpc.RpcClient;
import ru.alepar.rpc.exception.TransportException;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static ru.alepar.rpc.netty.Util.validateMethod;

public class NettyRpcClient implements RpcClient {

    private final InvocationHandler handler = new ClientProxyHandler();
    private final Channel channel;
    private final ClientBootstrap bootstrap;
    private final Map<Class<?>, Object> implementations = new HashMap<Class<?>, Object>();

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
    public <T> void addImplementation(Class<T> interfaceClass, T implObject) {
        implementations.put(interfaceClass, implObject);
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public <T> T getImplementation(Class<T> clazz) {
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, handler);
    }

    private class ClientProxyHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            latch = new CountDownLatch(1);
            validateMethod(method);
            channel.write(new InvocationRequest(method.getDeclaringClass().getName(), method.getName(), Util.toSerializable(args), method.getParameterTypes()));
            latch.await();
            if (response.exc != null) {
                throw response.exc;
            }
            return response.returnValue;
        }
    }

    private class RpcHandler extends SimpleChannelHandler {
        @Override
        public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
            Object message = e.getMessage();
            if(message instanceof InvocationResponse) {
                processResponse((InvocationResponse) message);
            } else if(message instanceof InvocationRequest) {
                processRequest((InvocationRequest) message);
            }

        }

        private void processRequest(InvocationRequest msg) {
            try {
                Class clazz = Class.forName(msg.className);
                Object impl = implementations.get(clazz);
                if(impl == null) {
                    throw new RuntimeException("interface is not registered on client: " + msg.className);
                }

                Method method;
                if (msg.args != null) {
                    method = clazz.getMethod(msg.methodName, msg.types);
                    if(method == null) {
                        throw new RuntimeException("method is not found in client implementation: " + msg.methodName);
                    }
                    method.invoke(impl, (Object[]) msg.args);
                } else {
                    method = clazz.getMethod(msg.methodName);
                    if(method == null) {
                        throw new RuntimeException("method is not found in client implementation: " + msg.methodName);
                    }
                    method.invoke(impl);
                }
            } catch (Exception e) {
                throw new RuntimeException("client failed to process request " + msg, e);
            }
        }

        private void processResponse(InvocationResponse message) {
            response = message;
            latch.countDown();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
            response = new InvocationResponse(null, new TransportException(e.getCause()));
            latch.countDown();
        }
    }

}
