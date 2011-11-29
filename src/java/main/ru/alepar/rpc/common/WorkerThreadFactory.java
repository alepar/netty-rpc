package ru.alepar.rpc.common;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class WorkerThreadFactory implements ThreadFactory {

    private static final ThreadGroup group = new ThreadGroup("NettyRpc-worker");
    private final AtomicInteger threadNumber = new AtomicInteger(1);

    public Thread newThread(Runnable r) {
        Thread t = new Thread(
                group, r,
                "" + threadNumber.getAndIncrement()
        );

        t.setDaemon(true);
        t.setPriority(Thread.NORM_PRIORITY);

        return t;
    }
}