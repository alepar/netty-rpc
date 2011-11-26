package ru.alepar.rpc.common;

import org.jboss.netty.channel.Channel;
import ru.alepar.rpc.api.Remote;

import java.io.Serializable;

public class NettyRemote implements Remote, Serializable {

    private final Channel channel;
    private final FeedbackProxyFactory proxyFactory;
    private final NettyId clientId;

    public NettyRemote(Channel channel) {
        this.channel = channel;
        this.proxyFactory = new FeedbackProxyFactory(this.channel);
        this.clientId = new NettyId(this.channel.getId());
    }

    @Override
    public Id getId() {
        return clientId;
    }

    @Override
    public ProxyFactory getProxyFactory() {
        return proxyFactory;
    }

    @Override
    public String getRemoteAddress() {
        return channel.getRemoteAddress().toString();
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

    static class NettyId implements Id {
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
}
