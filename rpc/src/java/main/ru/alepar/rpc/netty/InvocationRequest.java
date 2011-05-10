package ru.alepar.rpc.netty;

import java.io.Serializable;

class InvocationRequest implements Serializable {

    final String className;
    final String methodName;

    InvocationRequest(String className, String methodName) {
        this.className = className;
        this.methodName = methodName;
    }

}
