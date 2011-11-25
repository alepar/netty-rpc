package ru.alepar.rpc.netty;

import java.io.Serializable;

class InvocationRequest implements Serializable {

    final String className;
    final String methodName;
    final Serializable[] args;
    final Class<?>[] types;

    InvocationRequest(String className, String methodName, Serializable[] args, Class<?>[] types) {
        this.className = className;
        this.methodName = methodName;
        this.args = args;
        this.types = types;
    }

    @Override
    public String toString() {
        return "InvocationRequest{" +
                "className='" + className + '\'' +
                ", methodName='" + methodName + '\'' +
                '}';
    }
}
