package ru.alepar.rpc.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.alepar.rpc.common.NettyRemote;
import ru.alepar.rpc.common.message.KeepAlive;

class ServerKeepAliveThread extends Thread {

    private final Logger log = LoggerFactory.getLogger(ServerKeepAliveThread.class);

    private final long keepalivePeriod;

    private volatile boolean safeInterrupt = false;
    private final ClientRepository clients;

    public ServerKeepAliveThread(ClientRepository clients, long keepalivePeriod) {
        setName("KeepAlive-NettyRpcServer");

        this.clients = clients;
        this.keepalivePeriod = keepalivePeriod;
    }

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                for (NettyRemote remote : clients.getClients()) {
                    remote.getChannel().write(new KeepAlive());
                }
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
