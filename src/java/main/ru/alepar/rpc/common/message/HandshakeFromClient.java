package ru.alepar.rpc.common.message;

public class HandshakeFromClient extends RpcMessage {

    public final String[] classNames;

    public HandshakeFromClient(final String[] classNames) {
        this.classNames = classNames;
    }

    @Override
    public void visit(Visitor visitor) {
        visitor.acceptHandshakeFromClient(this);
    }

    public String toString() {
        return "HandshakeFromClient";
    }

}
