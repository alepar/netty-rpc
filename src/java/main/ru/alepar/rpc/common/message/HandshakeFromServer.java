package ru.alepar.rpc.common.message;

import ru.alepar.rpc.api.Remote;

import java.util.Set;

public class HandshakeFromServer extends RpcMessage {

    public final Remote.Id clientId;
    public final Class<?>[] classes;

    public HandshakeFromServer(Remote.Id clientId, Set<Class<?>> classes) {
        this.clientId = clientId;
        this.classes = classes.toArray(new Class<?>[classes.size()]);
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
