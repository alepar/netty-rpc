package ru.alepar.rpc.common;

import java.util.concurrent.ThreadFactory;

public class BossThreadFactory implements ThreadFactory {

    private static final ThreadGroup group = new ThreadGroup("NettyRpc-boss");

    public Thread newThread(Runnable r) {
        Thread t = new Thread(
                group, r,
                ""
        );

        t.setDaemon(true);
        t.setPriority(Thread.NORM_PRIORITY);

        return t;
    }
}