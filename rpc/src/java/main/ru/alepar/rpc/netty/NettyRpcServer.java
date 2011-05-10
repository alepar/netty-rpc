package ru.alepar.rpc.netty;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.serialization.ObjectDecoder;
import org.jboss.netty.handler.codec.serialization.ObjectEncoder;
import ru.alepar.rpc.RpcServer;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class NettyRpcServer implements RpcServer {

    private final Map<Class, Object> implementations = new HashMap<Class, Object>();
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
    public <T> void addImplementation(Class<T> clazz, T impl) {
        implementations.put(clazz, impl);
    }

    private class RpcHandler extends SimpleChannelHandler {

        @Override
        public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
            InvocationRequest msg = (InvocationRequest) e.getMessage();

            Class clazz = Class.forName(msg.className);
            Object impl = implementations.get(clazz);

            Method method = clazz.getMethod(msg.methodName);
            Serializable returnValue = (Serializable) method.invoke(impl);

            e.getChannel().write(new InvocationResponse(returnValue));
        }
    }

}
