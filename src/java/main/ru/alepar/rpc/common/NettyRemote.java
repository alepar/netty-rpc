package ru.alepar.rpc.common;

import org.jboss.netty.channel.Channel;
import ru.alepar.rpc.api.Remote;
import ru.alepar.rpc.api.exception.ProtocolException;
import ru.alepar.rpc.common.message.InvocationRequest;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Set;

import static ru.alepar.rpc.common.Util.*;

public class NettyRemote implements Remote, Serializable {

    private final Channel channel;
    private final Id clientId;
    private final Set<Class<?>> classes;

    public NettyRemote(Channel channel, Id clientId, Set<Class<?>> classes) {
        this.channel = channel;
        this.clientId = clientId;
        this.classes = classes;
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public <T> T getProxy(Class<T> clazz) {
        if (classes.contains(clazz)) {
            return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, new ProxyHandler());
        }

        throw new ProtocolException("no implementation on remote side for " + clazz.getCanonicalName());
    }

    @Override
    public Id getId() {
        return clientId;
    }

    @Override
    public String getRemoteAddress() {
        return channel.getRemoteAddress().toString();
    }

    @Override
    public boolean isWritable() {
        return channel.isWritable();
    }

    public Channel getChannel() {
        return channel;
    }

    @Override
    public int hashCode() {
        return clientId.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NettyRemote that = (NettyRemote) o;

        return this.clientId.equals(that.clientId);
    }

    @Override
    public String toString() {
        return "NettyRemote{" + clientId + "}";
    }

    private class ProxyHandler implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            validateMethod(method);
            channel.write(new InvocationRequest(method.getDeclaringClass().getName(), method.getName(), toSerializable(args), method.getParameterTypes()));
            return null;
        }

    }

}
