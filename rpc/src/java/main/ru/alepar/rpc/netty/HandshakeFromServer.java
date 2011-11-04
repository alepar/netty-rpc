package ru.alepar.rpc.netty;

import ru.alepar.rpc.ClientId;

import java.io.Serializable;

class HandshakeFromServer implements Serializable {

    final ClientId clientId;

    HandshakeFromServer(ClientId clientId) {
        this.clientId = clientId;
    }
}
