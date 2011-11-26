package ru.alepar.rpc.common.message;

public class ExceptionNotify extends RpcMessage {
    
    public final Exception exc;

    public ExceptionNotify(Exception exc) {
        this.exc = exc;
    }

    @Override
    public String toString() {
        return "ExceptionNotify{" +
                "exc=" + exc +
                '}';
    }
}