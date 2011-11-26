package ru.alepar.rpc.common;

import org.jboss.netty.channel.Channel;
import ru.alepar.rpc.api.Remote;
import ru.alepar.rpc.common.message.InvocationRequest;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static ru.alepar.rpc.common.Util.*;

public class NettyRemote implements Remote, Serializable {

    private final Channel channel;
    private final NettyId clientId;

    public NettyRemote(Channel channel) {
        this.channel = channel;
        this.clientId = new NettyId(this.channel.getId());
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public <T> T getProxy(Class<T> clazz) {
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, new ProxyHandler());
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

    private static String toHexString(byte[] digest) {
        StringBuilder result = new StringBuilder();
        for (int i =  digest.length-1; i >= 0; i--) {
            result.append(Integer.toHexString(0xFF & (int) digest[i]));
        }
        return result.toString();
    }

    private static byte[] toByteArray(int l) {
        byte[] result = new byte[4];
        for(int i=0; i<4; i++) {
            result[i] = (byte)(l & 0xff);
            l = l >> 8;
        }
        return result;
    }

    private static class NettyId implements Id {
        private final int id;

        public NettyId(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            NettyId that = (NettyId) o;

            return id == that.id;

        }

        @Override
        public int hashCode() {
            return id;
        }

        @Override
        public String toString() {
            return toHexString(toByteArray(id));
        }
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
