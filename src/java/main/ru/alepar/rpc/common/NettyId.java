package ru.alepar.rpc.common;

import ru.alepar.rpc.api.Remote;

public class NettyId implements Remote.Id {

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

    private static byte[] toByteArray(int l) {
        byte[] result = new byte[4];
        for(int i=0; i<4; i++) {
            result[i] = (byte)(l & 0xff);
            l = l >> 8;
        }
        return result;
    }

    private static String toHexString(byte[] digest) {
        StringBuilder result = new StringBuilder();
        for (int i =  digest.length-1; i >= 0; i--) {
            result.append(Integer.toHexString(0xFF & (int) digest[i]));
        }
        return result.toString();
    }
}
