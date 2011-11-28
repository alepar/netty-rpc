package ru.alepar.rpc.common.message;

import java.io.Serializable;

public class InvocationRequest extends RpcMessage {

    public final String className;
    public final String methodName;
    public final Serializable[] args;
    public final String[] paramClassNames;

    public InvocationRequest(final String className, final String methodName, final Serializable[] args, final String[] paramClassNames) {
        this.className = className;
        this.methodName = methodName;
        this.args = args;
        this.paramClassNames = paramClassNames;
    }

    @Override
    public void visit(Visitor visitor) {
        visitor.acceptInvocationRequest(this);
    }

    @Override
    public String toString() {
        return "InvocationRequest{" +
                "className='" + className + '\'' +
                ", methodName='" + methodName + '\'' +
                '}';
    }
}
