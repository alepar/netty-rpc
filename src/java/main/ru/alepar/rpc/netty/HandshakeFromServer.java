package ru.alepar.rpc.netty;

import ru.alepar.rpc.Remote;

import java.io.Serializable;

class HandshakeFromServer implements Serializable {

    final Remote.Id clientId;

    HandshakeFromServer(Remote.Id clientId) {
        this.clientId = clientId;
    }

    @Override
    public String toString() {
        return "HandshakeFromServer{" +
                "clientId=" + clientId +
                '}';
    }
}
