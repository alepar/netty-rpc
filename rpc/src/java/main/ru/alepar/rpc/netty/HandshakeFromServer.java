package ru.alepar.rpc.netty;

import ru.alepar.rpc.Client;

import java.io.Serializable;

class HandshakeFromServer implements Serializable {

    final Client.Id clientId;

    HandshakeFromServer(Client.Id clientId) {
        this.clientId = clientId;
    }
}
