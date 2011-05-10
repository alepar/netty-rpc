package ru.alepar.rpc.netty;

import java.io.Serializable;

class InvocationResponse implements Serializable {

    final Serializable returnValue;
    final Throwable exc;

    InvocationResponse(Serializable returnValue, Throwable exc) {
        this.returnValue = returnValue;
        this.exc = exc;
    }
}
