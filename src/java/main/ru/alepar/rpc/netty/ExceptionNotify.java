package ru.alepar.rpc.netty;

import java.io.Serializable;

class ExceptionNotify implements Serializable {
    
    final Exception exc;

    ExceptionNotify(Exception exc) {
        this.exc = exc;
    }

    @Override
    public String toString() {
        return "ExceptionNotify{" +
                "exc=" + exc +
                '}';
    }
}
