package ru.alepar.rpc.netty;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.serialization.ObjectDecoder;
import org.jboss.netty.handler.codec.serialization.ObjectEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.alepar.rpc.RpcServer;
import ru.alepar.rpc.exception.ProtocolException;
import ru.alepar.rpc.exception.TransportException;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class NettyRpcServer implements RpcServer {

    private static final Logger log = LoggerFactory.getLogger(NettyRpcServer.class);

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

            try {
                Object returnValue;
                Method method;
                if (msg.args != null) {
                    method = clazz.getMethod(msg.methodName, msg.types);
                    returnValue = method.invoke(impl, msg.args);
                } else {
                    method = clazz.getMethod(msg.methodName);
                    returnValue = method.invoke(impl);
                }
                try {
                    Serializable safeReturnValue = (Serializable) returnValue;
                    e.getChannel().write(new InvocationResponse(safeReturnValue, null));
                } catch (Exception exc) {
                    e.getChannel().write(new InvocationResponse(null, new ProtocolException("could not serialize returnValue for method: " + method, exc)));
                }
            } catch (InvocationTargetException exc) {
                e.getChannel().write(new InvocationResponse(null, exc.getCause()));
            } catch (Throwable t) {
                e.getChannel().write(new InvocationResponse(null, t));
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
            log.debug("NettyRpcServer caught exception", e.getCause());
            e.getChannel().write(new InvocationResponse(null, new TransportException(e.getCause()))) //try to pass exception to client
                    .addListener(ChannelFutureListener.CLOSE);  //and close connection on completion
        }
    }

}
