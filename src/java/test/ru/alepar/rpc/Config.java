package ru.alepar.rpc;

import java.net.InetSocketAddress;

import static java.lang.Thread.sleep;

public class Config {

    public static final InetSocketAddress BIND_ADDRESS = new InetSocketAddress(8338);
    public static final long TIMEOUT = 2000l;

    public static void giveTimeForMessagesToBeProcessed() {
        try {
            sleep(100l);
        } catch (InterruptedException ignored) {}
    }

}
