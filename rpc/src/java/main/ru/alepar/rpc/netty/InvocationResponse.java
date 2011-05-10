package ru.alepar.rpc.netty;

import java.io.Serializable;

class InvocationResponse implements Serializable {

    final Serializable returnValue;

    InvocationResponse(Serializable returnValue) {
        this.returnValue = returnValue;
    }
}
