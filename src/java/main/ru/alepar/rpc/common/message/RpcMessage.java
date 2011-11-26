package ru.alepar.rpc.common.message;

import java.io.Serializable;

public abstract class RpcMessage implements Serializable {

    public abstract void visit(Visitor visitor);

    public interface Visitor {
        void acceptExceptionNotify(ExceptionNotify msg);
        void acceptHandshakeFromClient(HandshakeFromClient msg);
        void acceptHandshakeFromServer(HandshakeFromServer msg);
        void acceptInvocationRequest(InvocationRequest msg);
        void acceptKeepAlive(KeepAlive msg);
    }
}
