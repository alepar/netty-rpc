package ru.alepar.rpc.netty;

import org.jboss.netty.channel.Channel;
import ru.alepar.rpc.ClientId;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.UUID;

class NettyClientId implements ClientId, Serializable {

    private final byte[] digest;

    NettyClientId(Channel channel) {
        try {
            MessageDigest md = MessageDigest.getInstance("md5");

            md.update(toByteArray(channel.getId()));
            md.update(channel.toString().getBytes());

            UUID uuid = UUID.randomUUID();
            md.update(toByteArray(uuid.getLeastSignificantBits()));
            md.update(toByteArray(uuid.getMostSignificantBits()));

            digest = md.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("couldn't build clientid", e);
        }
    }

    @Override
    public int hashCode() {
        return digest != null ? Arrays.hashCode(digest) : 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NettyClientId that = (NettyClientId) o;

        return Arrays.equals(digest, that.digest);

    }

    @Override
    public String toString() {
        return toHexString(this.digest);
    }

    private static String toHexString(byte[] digest) {
        StringBuilder result = new StringBuilder();
        for (byte b: digest) {
            result.append(Integer.toHexString(0xFF & b));
        }
        return result.toString();
    }

    private static byte[] toByteArray(long l) {
        byte[] result = new byte[8];
        for(int i=0; i<8; i++) {
            result[i] = (byte)(l & 0xff);
            l = l >> 8;
        }
        return result;
    }

}
