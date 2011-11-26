package ru.alepar.rpc.common;

import ru.alepar.rpc.api.exception.ProtocolException;
import ru.alepar.rpc.common.message.InvocationRequest;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Util {

    public static void validateMethod(Method method) {
        try {
            for (int i = 0; i < method.getParameterTypes().length; i++) {
                Class<?> clazz = method.getParameterTypes()[i];
                if (!clazz.isPrimitive() && !Serializable.class.isAssignableFrom(clazz)) {
                    throw new RuntimeException("param #" + (i + 1) + "(" + clazz.getName() + ") is not serializable");
                }
            }
            Class<?> clazz = method.getReturnType();
            if(clazz != Void.TYPE) {
                throw new RuntimeException("method must have void as return type");
            }
        } catch (RuntimeException e) {
            throw new ProtocolException("cannot rpc method: " + method, e);
        }
    }

    public static Serializable[] toSerializable(Object[] args) {
        if(args == null) {
            return null;
        }
        Serializable[] result = new Serializable[args.length];
        for (int i = 0; i < args.length; i++) {
            result[i] = (Serializable) args[i];
        }
        return result;
    }

    public static void invokeMethod(InvocationRequest msg, Class clazz, Object impl) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Method method;
        if (msg.args != null) {
            method = clazz.getMethod(msg.methodName, msg.types);
            if(method == null) {
                throw new RuntimeException("method is not found in server implementation: " + msg.methodName);
            }
            method.invoke(impl, (Object[]) msg.args);
        } else {
            method = clazz.getMethod(msg.methodName);
            if(method == null) {
                throw new RuntimeException("method is not found in server implementation: " + msg.methodName);
            }
            method.invoke(impl);
        }
    }
}
