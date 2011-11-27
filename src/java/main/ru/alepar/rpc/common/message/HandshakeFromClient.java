package ru.alepar.rpc.common.message;

import java.util.Set;

public class HandshakeFromClient extends RpcMessage {

    public final Class<?>[] classes;

    public HandshakeFromClient(Set<Class<?>> classes) {
        this.classes = classes.toArray(new Class<?>[classes.size()]);
    }

    @Override
    public void visit(Visitor visitor) {
        visitor.acceptHandshakeFromClient(this);
    }

    public String toString() {
        return "HandshakeFromClient";
    }

}
