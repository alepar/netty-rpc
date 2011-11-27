package ru.alepar.rpc.common;

import ru.alepar.rpc.api.exception.ConfigurationException;
import ru.alepar.rpc.common.message.InvocationRequest;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Util {

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

    public static void invokeMethod(InvocationRequest msg, Object impl) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Method method;
        if (msg.args != null) {
            method = impl.getClass().getMethod(msg.methodName, msg.types);
            if(method == null) {
                throw new ConfigurationException("method is not found in server implementation: " + msg.methodName);
            }
            method.invoke(impl, (Object[]) msg.args);
        } else {
            method = impl.getClass().getMethod(msg.methodName);
            if(method == null) {
                throw new ConfigurationException("method is not found in server implementation: " + msg.methodName);
            }
            method.invoke(impl);
        }
    }
}
