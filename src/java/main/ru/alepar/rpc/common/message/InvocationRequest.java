package ru.alepar.rpc.common.message;

import java.io.Serializable;

public class InvocationRequest extends RpcMessage {

    public final String className;
    public final String methodName;
    public final Serializable[] args;
    public final Class<?>[] types;

    public InvocationRequest(String className, String methodName, Serializable[] args, Class<?>[] types) {
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
