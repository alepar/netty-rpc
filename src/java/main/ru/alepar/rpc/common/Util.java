package ru.alepar.rpc.common;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import org.jboss.netty.handler.codec.serialization.ClassResolver;
import ru.alepar.rpc.api.exception.ConfigurationException;
import ru.alepar.rpc.common.message.InvocationRequest;

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

    public static void invokeMethod(InvocationRequest msg, Object impl, ClassResolver classResolver) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
        Method method;
        if (msg.args != null) {
            method = impl.getClass().getMethod(msg.methodName, unfoldStringToClasses(classResolver, msg.paramClassNames).toArray(new Class<?>[msg.paramClassNames.length]));
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

    public static String[] foldClassesToStrings(Set<Class<?>> classes) {
        String[] result = new String[classes.size()];
        int i=0;
        for (Class<?> clazz : classes) {
            result[i++] = clazz.getName();
        }
        return result;
    }

    public static Set<Class<?>> unfoldStringToClasses(ClassResolver classResolver, String[] classNames) throws ClassNotFoundException {
        Set<Class<?>> result = new HashSet<Class<?>>(classNames.length);
        for (String name : classNames) {
            result.add(classResolver.resolve(name));
        }
        return result;
    }
}
