package ru.alepar.rpc.netty;

import java.net.InetSocketAddress;

public class Config {
    public static final InetSocketAddress BIND_ADDRESS = new InetSocketAddress(8338);
    public static final long TIMEOUT = 2000l;
}
