package ru.alepar.rpc.common.message;

public class HandshakeFromClient extends RpcMessage {

    @Override
    public void visit(Visitor visitor) {
        visitor.acceptHandshakeFromClient(this);
    }

    public String toString() {
        return "HandshakeFromClient";
    }

}
