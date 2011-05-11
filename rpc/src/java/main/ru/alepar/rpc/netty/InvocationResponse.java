package ru.alepar.rpc.netty;

import ru.alepar.rpc.exception.RemoteException;
import ru.alepar.rpc.exception.SafeCheckedException;
import ru.alepar.rpc.exception.SafeRuntimeException;

import java.io.Serializable;

class InvocationResponse implements Serializable {

    final Serializable returnValue;
    final Throwable exc;

    InvocationResponse(Serializable returnValue, Throwable exc) {
        this.returnValue = returnValue;
        this.exc = smartWrap(exc);
    }

    private static Throwable smartWrap(Throwable exc) {
        if (exc == null) {
            return null;
        }

        if (exc instanceof SafeRuntimeException || exc instanceof SafeCheckedException) {
            return exc;
        }

        if(exc.getClass().getName().startsWith("java.lang.")) {
            return exc;
        }

        return new RemoteException("caught unsafe throwable", exc);
    }
}
