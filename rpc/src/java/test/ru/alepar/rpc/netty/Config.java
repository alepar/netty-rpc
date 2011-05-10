package ru.alepar.rpc.netty;

import java.net.InetSocketAddress;

public class Config {
    public static final InetSocketAddress BIND_ADDRESS = new InetSocketAddress(8080);
    public static final long TIMEOUT = 1000l;
}
