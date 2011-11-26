package ru.alepar.rpc.client;

import org.jboss.netty.channel.Channel;
import ru.alepar.rpc.common.NettyRemote;

class NettyClientRemote extends NettyRemote {

    private Id id;

    public NettyClientRemote(Channel channel) {
        super(channel);
    }

    @Override
    public Id getId() {
        return id;
    }

    public void setId(Id id) {
        this.id = id;
    }
}
