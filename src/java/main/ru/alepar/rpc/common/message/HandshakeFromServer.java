package ru.alepar.rpc.common.message;

import ru.alepar.rpc.api.Remote;

public class HandshakeFromServer extends RpcMessage {

    public final Remote.Id clientId;

    public HandshakeFromServer(Remote.Id clientId) {
        this.clientId = clientId;
    }

    @Override
    public void visit(Visitor visitor) {
        visitor.acceptHandshakeFromServer(this);
    }

    @Override
    public String toString() {
        return "HandshakeFromServer{" +
                "clientId=" + clientId +
                '}';
    }
}
