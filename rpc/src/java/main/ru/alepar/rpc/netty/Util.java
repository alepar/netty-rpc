package ru.alepar.rpc.netty;

import ru.alepar.rpc.exception.ProtocolException;

import java.io.Serializable;
import java.lang.reflect.Method;

class Util {

    static void validateMethod(Method method) {
        try {
            for (int i = 0; i < method.getParameterTypes().length; i++) {
                Class<?> clazz = method.getParameterTypes()[i];
                if (!clazz.isPrimitive() && !Serializable.class.isAssignableFrom(clazz)) {
                    throw new RuntimeException("param #" + (i + 1) + "(" + clazz.getName() + ") is not serializable");
                }
            }
            Class<?> clazz = method.getReturnType();
            if(!clazz.isPrimitive() && !Serializable.class.isAssignableFrom(clazz)) {
                throw new RuntimeException("return type (" + clazz.getName() + ") is not serializable");
            }
        } catch (RuntimeException e) {
            throw new ProtocolException("cannot rpc method: " + method, e);
        }
    }

    static Serializable[] toSerializable(Object[] args) {
        if(args == null) {
            return null;
        }
        Serializable[] result = new Serializable[args.length];
        for (int i = 0; i < args.length; i++) {
            result[i] = (Serializable) args[i];
        }
        return result;
    }
}
