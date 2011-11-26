package ru.alepar.rpc.client;

import org.jboss.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.alepar.rpc.common.message.KeepAlive;

class ClientKeepAliveThread extends Thread {

    private final Logger log = LoggerFactory.getLogger(ClientKeepAliveThread.class);

    private final Channel channel;
    private final long keepalivePeriod;

    private volatile boolean safeInterrupt = false;

    public ClientKeepAliveThread(Channel channel, long keepalivePeriod) {
        setName("KeepAlive-NettyRpcClient");

        this.channel = channel;
        this.keepalivePeriod = keepalivePeriod;
    }

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                channel.write(new KeepAlive());
                Thread.sleep(keepalivePeriod);
            }
        } catch (InterruptedException ignored) {}
        if (!safeInterrupt) {
            log.warn("{} interrupted", getName());
        }
    }

    public void safeInterrupt() {
        safeInterrupt = true;
        interrupt();
    }
}
